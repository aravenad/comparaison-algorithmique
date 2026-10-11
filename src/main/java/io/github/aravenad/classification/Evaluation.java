package io.github.aravenad.classification;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

/** Résultats d'un classifieur sur un ensemble de dépêches : prédictions, réussite par catégorie, matrice de confusion. */
public class Evaluation {

    private final List<String> categories;
    private final List<String> lignesClassement = new ArrayList<>();
    private final int[] nbDepeches;  // nbDepeches[réelle]
    private final int[][] confusion; // confusion[réelle][prédite]

    private Evaluation(List<String> categories) {
        this.categories = categories;
        this.nbDepeches = new int[categories.size()];
        this.confusion = new int[categories.size()][categories.size()];
    }

    /**
     * Classe chaque dépêche avec classifieur, qui doit déjà avoir appris. Les dépêches dont la catégorie réelle n'est
     * pas dans categories sont classées mais ne comptent dans aucun pourcentage.
     */
    public static Evaluation evaluer(Classifieur classifieur, List<Depeche> depeches, List<String> categories) {
        Evaluation evaluation = new Evaluation(categories);
        for (Depeche depeche : depeches) {
            String predite = classifieur.categoriePredite(depeche);
            evaluation.lignesClassement.add(depeche.getId() + ":" + predite.toUpperCase());
            int reelle = categories.indexOf(depeche.getCategorie().toLowerCase());
            if (reelle >= 0) {
                evaluation.nbDepeches[reelle]++;
                int indicePredite = categories.indexOf(predite);
                if (indicePredite >= 0) {
                    evaluation.confusion[reelle][indicePredite]++;
                }
            }
        }
        return evaluation;
    }

    /** Résultat : pourcentage des dépêches de la catégorie d'indice i qui ont été bien classées. */
    public double reussite(int i) {
        return nbDepeches[i] == 0 ? 0 : 100.0 * confusion[i][i] / nbDepeches[i];
    }

    /** Résultat : moyenne des pourcentages de réussite des catégories. */
    public double moyenne() {
        double somme = 0;
        for (int i = 0; i < categories.size(); i++) {
            somme += reussite(i);
        }
        return categories.isEmpty() ? 0 : somme / categories.size();
    }

    public List<String> getCategories() {
        return categories;
    }

    /** Résultat : pourcentages arrondis par catégorie, dans l'ordre de getCategories(). */
    public ArrayList<PaireChaineEntier> pourcentages() {
        ArrayList<PaireChaineEntier> pourcentages = new ArrayList<>();
        for (int i = 0; i < categories.size(); i++) {
            pourcentages.add(new PaireChaineEntier(categories.get(i), (int) Math.round(reussite(i))));
        }
        return pourcentages;
    }

    /** Écrit le fichier réponse : une ligne « id:CATEGORIE » par dépêche, puis les pourcentages et la moyenne. */
    public void ecrireClassement(String nomFichier) {
        Path chemin = Paths.get(nomFichier);
        try {
            if (chemin.getParent() != null) {
                Files.createDirectories(chemin.getParent());
            }
            try (BufferedWriter fichier = Files.newBufferedWriter(chemin, StandardCharsets.UTF_8)) {
                for (String ligne : lignesClassement) {
                    fichier.write(ligne + "\n");
                }
                for (PaireChaineEntier p : pourcentages()) {
                    fichier.write(p.getChaine().toUpperCase() + " : " + p.getEntier() + "%\n");
                }
                fichier.write("MOYENNE : " + Math.round(moyenne()) + "%\n");
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Écriture du classement " + nomFichier + " impossible", e);
        }
    }

    /** Résultat : la matrice de confusion en texte, une ligne par catégorie réelle, une colonne par catégorie prédite. */
    public String matriceConfusion() {
        StringBuilder texte = new StringBuilder(String.format("%-24s", "réelle \\ prédite"));
        for (String categorie : categories) {
            texte.append(String.format(" %6.6s", categorie));
        }
        texte.append('\n');
        for (int i = 0; i < categories.size(); i++) {
            texte.append(String.format("%-24s", categories.get(i)));
            for (int j = 0; j < categories.size(); j++) {
                texte.append(String.format(" %6d", confusion[i][j]));
            }
            texte.append('\n');
        }
        return texte.toString();
    }

    /**
     * Validation croisée stratifiée répétée : les dépêches sont mélangées (graines 1 à repetitions), réparties en
     * nbBlocs blocs équilibrés par catégorie ; on apprend sur nbBlocs - 1 blocs et on évalue sur le dernier, pour
     * chaque bloc. Résultat : la moyenne des réussites moyennes obtenues.
     */
    public static double validationCroisee(Supplier<Classifieur> fabrique, List<Depeche> depeches,
                                           List<String> categories, int nbBlocs, int repetitions) {
        double somme = 0;
        for (int graine = 1; graine <= repetitions; graine++) {
            List<Depeche> melangees = new ArrayList<>(depeches);
            Collections.shuffle(melangees, new Random(graine));
            HashMap<String, Integer> rangDansCategorie = new HashMap<>();
            int[] bloc = new int[melangees.size()];
            for (int i = 0; i < melangees.size(); i++) {
                String categorie = melangees.get(i).getCategorie().toLowerCase();
                bloc[i] = (rangDansCategorie.merge(categorie, 1, Integer::sum) - 1) % nbBlocs;
            }
            for (int b = 0; b < nbBlocs; b++) {
                List<Depeche> apprentissage = new ArrayList<>();
                List<Depeche> validation = new ArrayList<>();
                for (int i = 0; i < melangees.size(); i++) {
                    (bloc[i] == b ? validation : apprentissage).add(melangees.get(i));
                }
                Classifieur classifieur = fabrique.get();
                classifieur.apprendre(apprentissage);
                somme += evaluer(classifieur, validation, categories).moyenne();
            }
        }
        return somme / (nbBlocs * repetitions);
    }
}
