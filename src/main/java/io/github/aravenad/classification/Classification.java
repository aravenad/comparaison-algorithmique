package io.github.aravenad.classification;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class Classification {

    static final List<String> CATEGORIES =
            List.of("environnement-sciences", "culture", "economie", "politique", "sports");

    // Les mots de moins de 3 lettres sont ignorés lors de la génération automatique des lexiques. 3 plutôt que 4
    // pour garder les sigles (PIB, PSG, OGM, UMP…), très discriminants (cf. docs/analyse.md).
    static final int LONGUEUR_MIN_MOT = 3;

    public static ArrayList<Depeche> lectureDepeches(String nomFichier) {
        // {} → {résultat = dépêches du fichier nomFichier. Chaque dépêche commence par les lignes « .N id », « .D date »,
        //       « .C catégorie » puis « .T texte » ; les lignes suivantes, jusqu'à la prochaine dépêche, complètent le texte.}
        ArrayList<Depeche> depeches = new ArrayList<>();
        try (BufferedReader lecteur = Files.newBufferedReader(Paths.get(nomFichier), StandardCharsets.UTF_8)) {
            String id = null, date = null, categorie = null;
            StringBuilder contenu = null;
            String ligne;
            while ((ligne = lecteur.readLine()) != null) {
                ligne = ligne.stripTrailing();
                if (ligne.startsWith(".N ")) {
                    if (id != null) {
                        depeches.add(new Depeche(id, date, categorie, contenu.toString()));
                    }
                    id = ligne.substring(3).strip();
                    date = null;
                    categorie = null;
                    contenu = new StringBuilder();
                } else if (id == null || ligne.isEmpty()) {
                    continue;
                } else if (ligne.startsWith(".D ")) {
                    date = ligne.substring(3).strip();
                } else if (ligne.startsWith(".C ")) {
                    categorie = ligne.substring(3).strip();
                } else {
                    String texte = ligne.startsWith(".T ") ? ligne.substring(3) : ligne;
                    if (contenu.length() > 0) {
                        contenu.append('\n');
                    }
                    contenu.append(texte);
                }
            }
            if (id != null) {
                depeches.add(new Depeche(id, date, categorie, contenu.toString()));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Lecture des dépêches " + nomFichier + " impossible", e);
        }
        return depeches;
    }

    public static ArrayList<PaireChaineEntier> classementDepeches(ArrayList<Depeche> depeches, ArrayList<Categorie> categories, String nomFichier) {
        // {} → {pour chacune des dépêches de depeches, calcule le score pour chaque catégorie de categories et écrit
        //       dans le fichier de nom nomFichier, le nom de la catégorie ayant le plus grand score ainsi que les
        //       pourcentages conformément au format attendu pour les fichiers réponses.
        //       résultat = pourcentage de dépêches bien classées pour chaque catégorie}
        // Le pourcentage d'une catégorie est calculé sur le nombre de dépêches qui lui appartiennent réellement
        // (et non plus égal au nombre brut de bonnes réponses, qui ne valait un pourcentage que pour 100 dépêches).
        HashMap<String, Integer> nbDepeches = new HashMap<>();
        HashMap<String, Integer> nbBienClassees = new HashMap<>();
        ArrayList<PaireChaineEntier> vScores = new ArrayList<>();
        try (BufferedWriter fichier = ouvrirEnEcriture(nomFichier)) {
            for (Depeche depeche : depeches) {
                vScores.clear();
                for (Categorie categorie : categories) {
                    vScores.add(new PaireChaineEntier(categorie.getNom(), categorie.score(depeche)));
                }
                String catPredite = UtilitairePaireChaineEntier.chaineMax(vScores);
                fichier.write(depeche.getId() + ":" + catPredite.toUpperCase() + "\n");

                String catReelle = depeche.getCategorie().toLowerCase();
                nbDepeches.merge(catReelle, 1, Integer::sum);
                if (catReelle.equalsIgnoreCase(catPredite)) {
                    nbBienClassees.merge(catReelle, 1, Integer::sum);
                }
            }

            ArrayList<PaireChaineEntier> pourcentages = new ArrayList<>();
            for (Categorie categorie : categories) {
                String nom = categorie.getNom().toLowerCase();
                int total = nbDepeches.getOrDefault(nom, 0);
                int pourcentage = total == 0 ? 0 : Math.round(100f * nbBienClassees.getOrDefault(nom, 0) / total);
                pourcentages.add(new PaireChaineEntier(categorie.getNom(), pourcentage));
                fichier.write(categorie.getNom().toUpperCase() + " : " + pourcentage + "%\n");
            }
            fichier.write("MOYENNE : " + Math.round(UtilitairePaireChaineEntier.moyenne(pourcentages)) + "%\n");
            return pourcentages;
        } catch (IOException e) {
            throw new UncheckedIOException("Écriture du classement " + nomFichier + " impossible", e);
        }
    }

    public static HashSet<String> lectureStopWords(String nomFichier) {
        // {} → {résultat = HashSet<String> contenant tous les mots du fichier nomFichier}
        HashSet<String> stopWords = new HashSet<>();
        try (BufferedReader lecteur = Files.newBufferedReader(Paths.get(nomFichier), StandardCharsets.UTF_8)) {
            String ligne;
            while ((ligne = lecteur.readLine()) != null) {
                ligne = ligne.strip().toLowerCase();
                if (!ligne.isEmpty()) {
                    stopWords.add(ligne);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Lecture des mots vides " + nomFichier + " impossible", e);
        }
        return stopWords;
    }

    public static ArrayList<PaireChaineEntier> initDico(ArrayList<Depeche> depeches, String categorie, Set<String> motsExclus) {
        // {} → {résultat = ArrayList<PaireChaineEntier> contenant tous les mots présents dans au moins une dépêche
        //                  de la catégorie categorie. Attention, même si le mot est présent plusieurs fois, il ne doit
        //                  apparaître qu’une fois dans la ArrayList retournée. Dans les entiers, nous stockerons les
        //                  scores associés à chaque mot et dans un premier temps, nous initialiserons ce score à 0.}
        // Les mots exclus (mots vides) et les mots trop courts sont ignorés. Le résultat est trié par ordre
        // alphabétique : le TreeSet élimine les doublons en O(log n) par mot.
        TreeSet<String> mots = new TreeSet<>();
        for (Depeche depeche : depeches) {
            if (depeche.getCategorie().equalsIgnoreCase(categorie)) {
                for (String mot : depeche.getMots()) {
                    if (mot.length() >= LONGUEUR_MIN_MOT && !motsExclus.contains(mot)) {
                        mots.add(mot);
                    }
                }
            }
        }
        ArrayList<PaireChaineEntier> dico = new ArrayList<>(mots.size());
        for (String mot : mots) {
            dico.add(new PaireChaineEntier(mot, 0));
        }
        return dico;
    }

    public static void calculScores(ArrayList<Depeche> depeches, String categorie, ArrayList<PaireChaineEntier> dictionnaire) {
        // {} → {met à jour les scores des mots présents dans dictionnaire. Lorsqu'un mot présent dans dictionnaire
        //       apparaît dans une dépêche de depeches, son score est : décrémenté si la dépêche n'est pas dans la
        //       catégorie categorie et incrémenté si la dépêche est dans la catégorie categorie.}
        HashMap<String, PaireChaineEntier> index = new HashMap<>();
        for (PaireChaineEntier paire : dictionnaire) {
            index.put(paire.getChaine(), paire);
        }
        for (Depeche depeche : depeches) {
            int variation = depeche.getCategorie().equalsIgnoreCase(categorie) ? 1 : -1;
            for (String mot : depeche.getMots()) {
                PaireChaineEntier paire = index.get(mot);
                if (paire != null) {
                    paire.setEntier(paire.getEntier() + variation);
                }
            }
        }
    }

    public static int poidsPourScore(int score) {
        // {} → {résultat = une valeur de poids (1,2 ou 3) en fonction du score score
        //                  (à vous de décider quel poids pour quel score).}
        // Seuils choisis par validation croisée sur le corpus d'apprentissage (cf. docs/analyse.md) : un mot qui
        // n'apparaît pas nettement plus dans la catégorie qu'ailleurs (score <= 1) ne reçoit que le poids minimal.
        if (score <= 1) {
            return 1;
        } else if (score >= 4) {
            return 3;
        } else {
            return 2;
        }
    }

    public static void generationLexique(ArrayList<Depeche> depeches, String categorie, Set<String> motsExclus, String nomFichier) {
        // {} → {crée pour la catégorie categorie le fichier lexique de nom nomFichier à partir du vecteur de dépêches
        //       depeches. Cette méthode construit une ArrayList<PaireChaineEntier> avec initDico, puis met à
        //       jour les scores dans ce vecteur avec calculScores et enfin utilise le vecteur résultant pour créer un
        //       fichier lexique en utilisant la fonction poidsPourScore.}
        ArrayList<PaireChaineEntier> dictionnaire = initDico(depeches, categorie, motsExclus);
        calculScores(depeches, categorie, dictionnaire);
        try (BufferedWriter fichier = ouvrirEnEcriture(nomFichier)) {
            for (PaireChaineEntier paire : dictionnaire) {
                fichier.write(paire.getChaine() + ":" + poidsPourScore(paire.getEntier()) + "\n");
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Écriture du lexique " + nomFichier + " impossible", e);
        }
    }

    private static BufferedWriter ouvrirEnEcriture(String nomFichier) throws IOException {
        Path chemin = Paths.get(nomFichier);
        if (chemin.getParent() != null) {
            Files.createDirectories(chemin.getParent());
        }
        return Files.newBufferedWriter(chemin, StandardCharsets.UTF_8);
    }

    private static ArrayList<Categorie> chargerCategories(String dossierLexiques) {
        ArrayList<Categorie> categories = new ArrayList<>();
        for (String nom : CATEGORIES) {
            Categorie categorie = new Categorie(nom);
            categorie.initLexique(Paths.get(dossierLexiques, nom + ".txt").toString());
            categories.add(categorie);
        }
        return categories;
    }

    private static void afficherResultats(String titre, ArrayList<PaireChaineEntier> pourcentages, String fichier) {
        System.out.printf("%n%s (détail dans %s)%n", titre, fichier);
        for (PaireChaineEntier p : pourcentages) {
            System.out.printf("  %-24s %3d %%%n", p.getChaine(), p.getEntier());
        }
        System.out.printf("  %-24s %3d %%%n", "MOYENNE", Math.round(UtilitairePaireChaineEntier.moyenne(pourcentages)));
    }

    public static void main(String[] args) {
        // Usage : [corpus d'apprentissage] [corpus de test] [dossier des résultats]
        String fichierApprentissage = args.length > 0 ? args[0] : "data/corpus/depeches.txt";
        String fichierTest = args.length > 1 ? args[1] : "data/corpus/test.txt";
        String dossierResultats = args.length > 2 ? args[2] : "resultats";
        String dossierLexiquesAuto = Paths.get(dossierResultats, "lexiques-auto").toString();

        System.out.println("Chargement des dépêches...");
        ArrayList<Depeche> apprentissage = lectureDepeches(fichierApprentissage);
        ArrayList<Depeche> test = lectureDepeches(fichierTest);
        System.out.printf("  %d dépêches d'apprentissage (%s), %d dépêches de test (%s)%n",
                apprentissage.size(), fichierApprentissage, test.size(), fichierTest);

        // 1. Classement avec les lexiques écrits à la main
        String sortieManuelle = Paths.get(dossierResultats, "classement-manuel.txt").toString();
        ArrayList<PaireChaineEntier> resultatsManuels =
                classementDepeches(test, chargerCategories("data/lexiques"), sortieManuelle);
        afficherResultats("Lexiques manuels", resultatsManuels, sortieManuelle);

        // 2. Génération des lexiques à partir du corpus d'apprentissage, une catégorie par thread
        long debut = System.currentTimeMillis();
        HashSet<String> motsExclus = lectureStopWords("data/stopwords-fr.txt");
        CATEGORIES.parallelStream().forEach(nom -> generationLexique(
                apprentissage, nom, motsExclus, Paths.get(dossierLexiquesAuto, nom + ".txt").toString()));
        long dureeGeneration = System.currentTimeMillis() - debut;

        // 3. Classement du corpus de test avec les lexiques générés (une fois leur écriture terminée)
        debut = System.currentTimeMillis();
        String sortieAuto = Paths.get(dossierResultats, "classement-auto.txt").toString();
        ArrayList<PaireChaineEntier> resultatsAuto =
                classementDepeches(test, chargerCategories(dossierLexiquesAuto), sortieAuto);
        long dureeClassement = System.currentTimeMillis() - debut;
        afficherResultats("Lexiques générés automatiquement", resultatsAuto, sortieAuto);

        System.out.printf("%nTemps d'exécution : génération des lexiques %d ms, classement %d ms.%n",
                dureeGeneration, dureeClassement);
    }
}
