import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Scanner;

public class Classification {

    private static ArrayList<Depeche> lectureDepeches(String nomFichier) {
        //creation d'un tableau de dépêches
        ArrayList<Depeche> depeches = new ArrayList<>();
        try {
            // lecture du fichier d'entrée
            FileInputStream file = new FileInputStream(nomFichier);
            Scanner scanner = new Scanner(file);

            while (scanner.hasNextLine()) {
                String ligne = scanner.nextLine();
                String id = ligne.substring(3);
                ligne = scanner.nextLine();
                String date = ligne.substring(3);
                ligne = scanner.nextLine();
                String categorie = ligne.substring(3);
                ligne = scanner.nextLine();
                String lignes = ligne.substring(3);
                while (scanner.hasNextLine() && !ligne.isEmpty()) {
                    ligne = scanner.nextLine();
                    if (!ligne.isEmpty()) {
                        lignes = lignes + '\n' + ligne;
                    }
                }
                Depeche uneDepeche = new Depeche(id, date, categorie, lignes);
                depeches.add(uneDepeche);
            }
            scanner.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return depeches;
    }

    public static void classementDepeches(ArrayList<Depeche> depeches, ArrayList<Categorie> categories, String nomFichier) {
        // {} → {pour chacune des dépêches de depeches, calcule le score pour chaque catégorie de categories et écrit
        //       dans le fichier de nom nomFichier, le nom de la catégorie ayant le plus grand score ainsi que les
        //       pourcentages conformément au format attendu pour les fichiers réponses (voir ci-dessus). Prenez exemple
        //       sur la classe ExempleEcritureFichier pour l’écriture dans un fichier.}
        ArrayList<PaireChaineEntier> vScores = new ArrayList<>();
        ArrayList<PaireChaineEntier> moy = new ArrayList<>();
        ArrayList<String> catPredite = new ArrayList<>();
        int cmp;
        try {
            FileWriter file = new FileWriter(nomFichier);
            for (Depeche depeche : depeches) {
                vScores.clear();
                for (Categorie categorie : categories) {
                    vScores.add(new PaireChaineEntier(categorie.getNom(), categorie.score(depeche)));
                }
                String cat = UtilitairePaireChaineEntier.chaineMax(vScores).toUpperCase();
                file.write(depeche.getId() + ":" + cat + "\n");
                catPredite.add(cat);
            }
            for (Categorie category : categories) {
                cmp = 0;
                String nomCat = category.getNom();
                int i = 0;
                while (i < depeches.size()) {
                    if (depeches.get(i).getCategorie().compareToIgnoreCase(nomCat) == 0) {
                        if (depeches.get(i).getCategorie().compareToIgnoreCase(catPredite.get(i)) == 0) {
                            cmp += 1;
                        }
                    }
                    i++;
                }
                file.write(category.getNom().toUpperCase() + " : " + cmp + "%" + "\n");
                PaireChaineEntier calcMoy = new PaireChaineEntier(category.getNom(), cmp);
                moy.add(calcMoy);
            }
            int moyenne = (int) UtilitairePaireChaineEntier.moyenne(moy);
            file.write("MOYENNE : " + moyenne + "%");
            file.close();
            System.out.printf("Classement généré avec succès dans %s\n", nomFichier);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<PaireChaineEntier> initDico(ArrayList<Depeche> depeches, String categorie) {
        // {} → {résultat = une ArrayList<PaireChaineEntier> contenant tous les mots présents dans au moins une dépêche
        //                  de la catégorie categorie. Attention, même si le mot est présent plusieurs fois, il ne doit
        //                  apparaître qu’une fois dans la ArrayList retournée. Dans les entiers, nous stockerons les
        //                  scores associés à chaque mot et dans un premier temps, nous initialiserons ce score à 0.}
    ArrayList<PaireChaineEntier> dico = new ArrayList<>();
    HashSet<String> motsExclus = new HashSet<>();
        motsExclus.add("pour");
        motsExclus.add("tous");
        motsExclus.add("tout");
        motsExclus.add("toute");
        motsExclus.add("toutes");
        motsExclus.add("dans");
        for (Depeche depeche : depeches) {
            if (depeche.getCategorie().compareToIgnoreCase(categorie) == 0) {
                for (String mot : depeche.getMots()) {
                    if (UtilitairePaireChaineEntier.indicePourChaine(dico,mot)!=-1) {
                        break;
                    }
                    PaireChaineEntier temp = new PaireChaineEntier(mot, 0);
                    if (temp.getChaine().length() > 3 &&
                            (!motsExclus.contains(temp.getChaine())))
                    {
                        dico.add(temp);
                    }
                }
            }
        }
        return dico;
    }



    public static void calculScores(ArrayList<Depeche> depeches, String categorie, ArrayList<PaireChaineEntier> dictionnaire) {
        for (Depeche depeche : depeches) {
            if (depeche.getCategorie().compareToIgnoreCase(categorie) == 0) {
                for (int i = 0; i < depeche.getMots().size(); i++) {
                    for (PaireChaineEntier paire : dictionnaire) {
                        if (paire.getChaine().compareToIgnoreCase(depeche.getMots().get(i)) == 0) {
                            paire.setEntier(paire.getEntier() + 1);
                        }
                    }
                }
            } else {
                for (int i = 0; i < depeche.getMots().size(); i++) {
                    for (PaireChaineEntier paire : dictionnaire) {
                        if (paire.getChaine().compareToIgnoreCase(depeche.getMots().get(i)) == 0) {
                            paire.setEntier(paire.getEntier() - 1);
                        }
                    }
                }
            }
        }
    }



    public static int poidsPourScore(int score) {
        // {} → {résultat = une valeur de poids (1,2 ou 3) en fonction du score score
        //                  (à vous de décider quel poids pour quel score).}
        int poids;
        if (score <= -4) {
            poids = 1;
        } else if (score >= 4) {
            poids = 3;
        } else {
            poids = 2;
        }
        return poids;
    }

    public static void generationLexique(ArrayList<Depeche> depeches, String categorie, String nomFichier) {
        // {} → {crée pour la catégorie categorie le fichier lexique de nom nomFichier à partir du vecteur de dépêches
        //       depeches. Cette méthode doit construire une ArrayList<PaireChaineEntier> avec initDico, puis mettre à
        //       jour les scores dans ce vecteur avec calculScores et enfin utiliser le vecteur résultant pour créer un
        //       fichier lexique en utilisant la fonction poidsPourScore. Prenez exemple sur la classe
        //       ExempleEcritureFichier pour l’écriture dans un fichier.}
        try {
            ArrayList<PaireChaineEntier> dictionnaire = initDico(depeches, categorie);
            calculScores(depeches, categorie, dictionnaire);
            FileWriter file = new FileWriter(nomFichier);
            for (PaireChaineEntier paire : dictionnaire) {
                file.write(paire.getChaine() + ":" + poidsPourScore(paire.getEntier()) + "\n");
            }
            file.close();
            System.out.printf("Lexique généré avec succès dans %s\n", nomFichier);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        //Chargement des dépêches en mémoire
        System.out.println("Chargement des dépêches...");
        ArrayList<Depeche> depeches = lectureDepeches("./depeches.txt");

        // Affichage des dépêches
//        for (Depeche depeche : depeches) {
//            depeche.afficher();
//        }

        // Création des catégories
        Categorie culture = new Categorie("culture");
        Categorie economie = new Categorie("economie");
        Categorie environnementSciences = new Categorie("environnement-sciences");
        Categorie politique = new Categorie("politique");
        Categorie sports = new Categorie("sports");

        System.out.println("Initialisation des lexiques...");
        culture.initLexique("./culture.txt");
        economie.initLexique("./economie.txt");
        environnementSciences.initLexique("./environnement-sciences.txt");
        politique.initLexique("./politique.txt");
        sports.initLexique("./sports.txt");
        ArrayList<Categorie> vCategories = new ArrayList<>(Arrays.asList(environnementSciences, culture, economie, politique, sports));

        System.out.println("Génération du classement des dépêches...");
        classementDepeches(depeches,vCategories,"./output.txt");

//        System.out.printf("Lexique de la catégorie %s\n", sports.getNom());
//        for (int i = 0; i < sports.getLexique().size(); i++) {
//            System.out.printf("%s (score : %d)\n",
//                    sports.getLexique().get(i).getChaine(),
//                    sports.getLexique().get(i).getEntier());
//        }

//        Scanner scanner = new Scanner(System.in);
//        System.out.print("\nEntrez un mot : ");
//        String mot = scanner.nextLine();
//        System.out.printf("Le poids associé à %s est %d\n\n",
//                mot,
//                UtilitairePaireChaineEntier.entierPourChaine(sports.getLexique(), mot));

//        ArrayList<PaireChaineEntier> vScores = new ArrayList<>();
//        for (Categorie categorie : vCategories) {
//            vScores.add(new PaireChaineEntier(categorie.getNom(), categorie.score(depeches.get((int) (Math.random() * 500)))));
//        }
//        for (PaireChaineEntier score : vScores) {
//            System.out.printf("Score de la catégorie %s : %d\n", score.getChaine(), score.getEntier());
//        }
//        System.out.println("Prédiction de la catégorie de la dépêche : " + depeches.get((int) (Math.random() * 500)).getId());
//        System.out.printf("La catégorie prédite est %s\n", UtilitairePaireChaineEntier.chaineMax(vScores).toUpperCase());
//        System.out.printf("Le poids associé à %s est %d\n", mot, UtilitairePaireChaineEntier.entierPourChaine(sports.getLexique(), mot));

        // Calcul du temps d'exécution (début)
        long startTimeProcess = System.currentTimeMillis();

        System.out.println("\nGénération des nouveaux lexiques...");
        ArrayList<Thread> threads = new ArrayList<>();
        threads.add(new Thread(() -> generationLexique(depeches, "culture", "./cultureAuto.txt")));
        threads.add(new Thread(() -> generationLexique(depeches, "economie", "./economieAuto.txt")));
        threads.add(new Thread(() -> generationLexique(depeches, "environnement-sciences", "./environnement-sciencesAuto.txt")));
        threads.add(new Thread(() -> generationLexique(depeches, "politique", "./politiqueAuto.txt")));
        threads.add(new Thread(() -> generationLexique(depeches, "sports", "./sportsAuto.txt")));

        System.out.println("Initialisation des nouveaux lexiques...");
        culture.initLexique("./cultureAuto.txt");
        economie.initLexique("./economieAuto.txt");
        environnementSciences.initLexique("./environnement-sciencesAuto.txt");
        politique.initLexique("./politiqueAuto.txt");
        sports.initLexique("./sportsAuto.txt");
        ArrayList<Categorie> vCategoriesAuto = new ArrayList<>(Arrays.asList(environnementSciences, culture, economie, politique, sports));

        System.out.println("Génération du classement des dépêches...");
        classementDepeches(depeches,vCategoriesAuto,"./outputAuto.txt");

        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Calcul du temps d'exécution (fin)
        long endTimeProcess = System.currentTimeMillis();
        System.out.printf("\nTemps d'exécution du programme avec les nouveaux lexiques -> %dms.\n", (endTimeProcess-startTimeProcess));
    }
}