package io.github.aravenad.classification.experiences;

import io.github.aravenad.classification.Classification;
import io.github.aravenad.classification.Depeche;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Réglage des paramètres de génération des lexiques par validation croisée sur le corpus d'apprentissage
 * (cf. docs/analyse.md, section « Réglage des lexiques générés »).
 *
 * <p>Ce n'est pas un test unitaire : il se lance à la main, depuis la racine du projet, après {@code ./mvnw test-compile} :
 * <pre>
 * java -cp target/classes:target/test-classes io.github.aravenad.classification.experiences.ValidationCroisee
 * java -cp target/classes:target/test-classes io.github.aravenad.classification.experiences.ValidationCroisee grille
 * </pre>
 * Sans argument, il évalue les variantes du tableau de docs/analyse.md ; avec {@code grille}, il parcourt toutes les
 * combinaisons de paramètres (environ 30 s sur 8 cœurs).
 */
public class ValidationCroisee {

    private static final List<String> CATEGORIES =
            List.of("environnement-sciences", "culture", "economie", "politique", "sports");
    private static final int NB_BLOCS = 5;
    private static final int NB_REPETITIONS = 10;
    private static final int GARDER_TOUS = Integer.MIN_VALUE;

    /**
     * Paramètres de génération : longueur minimale des mots, score minimal pour qu'un mot soit gardé dans le lexique,
     * seuils de poids (poids 1 si score < seuil2, 3 si score >= seuil3, 2 sinon) et retrait des mots vides.
     */
    record Variante(int longueurMin, int scoreMin, int seuil2, int seuil3, boolean sansMotsVides) {
        int poids(int score) {
            return score < seuil2 ? 1 : score >= seuil3 ? 3 : 2;
        }

        @Override
        public String toString() {
            return String.format("mots >= %d lettres, %s, poids 1 si score < %d, 3 si score >= %d%s", longueurMin,
                    scoreMin == GARDER_TOUS ? "tous les mots" : "mots au score >= " + scoreMin, seuil2, seuil3,
                    sansMotsVides ? "" : ", mots vides gardés");
        }
    }

    private final Set<String> motsVides;

    ValidationCroisee(Set<String> motsVides) {
        this.motsVides = motsVides;
    }

    // Même algorithme que Classification.generationLexique (initDico, calculScores, poidsPourScore), paramétré.
    List<Map<String, Integer>> lexiques(List<Depeche> apprentissage, Variante v) {
        List<Map<String, Integer>> lexiques = new ArrayList<>();
        for (String categorie : CATEGORIES) {
            HashMap<String, Integer> scores = new HashMap<>();
            for (Depeche d : apprentissage) {
                if (d.getCategorie().equalsIgnoreCase(categorie)) {
                    for (String mot : d.getMots()) {
                        if (mot.length() >= v.longueurMin() && !(v.sansMotsVides() && motsVides.contains(mot))) {
                            scores.put(mot, 0);
                        }
                    }
                }
            }
            for (Depeche d : apprentissage) {
                int variation = d.getCategorie().equalsIgnoreCase(categorie) ? 1 : -1;
                for (String mot : d.getMots()) {
                    scores.computeIfPresent(mot, (m, s) -> s + variation);
                }
            }
            HashMap<String, Integer> lexique = new HashMap<>();
            scores.forEach((mot, score) -> {
                if (score >= v.scoreMin()) {
                    lexique.put(mot, v.poids(score));
                }
            });
            lexiques.add(lexique);
        }
        return lexiques;
    }

    // Moyenne des pourcentages de réussite par catégorie, comme Classification.classementDepeches.
    static double reussite(List<Map<String, Integer>> lexiques, List<Depeche> depeches) {
        int[] total = new int[CATEGORIES.size()];
        int[] bienClassees = new int[CATEGORIES.size()];
        for (Depeche d : depeches) {
            int predite = -1;
            int scoreMax = Integer.MIN_VALUE;
            for (int i = 0; i < lexiques.size(); i++) {
                int score = 0;
                for (String mot : d.getMots()) {
                    score += lexiques.get(i).getOrDefault(mot, 0);
                }
                if (score > scoreMax) {
                    scoreMax = score;
                    predite = i;
                }
            }
            int reelle = CATEGORIES.indexOf(d.getCategorie().toLowerCase());
            total[reelle]++;
            if (reelle == predite) {
                bienClassees[reelle]++;
            }
        }
        double somme = 0;
        for (int i = 0; i < total.length; i++) {
            somme += 100.0 * bienClassees[i] / total[i];
        }
        return somme / total.length;
    }

    // Validation croisée stratifiée : la j-ème dépêche de chaque catégorie va dans le bloc j % NB_BLOCS.
    double validationCroisee(List<Depeche> depeches, Variante v) {
        HashMap<String, Integer> rangDansCategorie = new HashMap<>();
        int[] bloc = new int[depeches.size()];
        for (int i = 0; i < depeches.size(); i++) {
            int rang = rangDansCategorie.merge(depeches.get(i).getCategorie().toLowerCase(), 1, Integer::sum) - 1;
            bloc[i] = rang % NB_BLOCS;
        }
        double somme = 0;
        for (int b = 0; b < NB_BLOCS; b++) {
            List<Depeche> apprentissage = new ArrayList<>();
            List<Depeche> validation = new ArrayList<>();
            for (int i = 0; i < depeches.size(); i++) {
                (bloc[i] == b ? validation : apprentissage).add(depeches.get(i));
            }
            somme += reussite(lexiques(apprentissage, v), validation);
        }
        return somme / NB_BLOCS;
    }

    // Validation croisée répétée sur des découpages aléatoires (graines fixes), pour réduire la variance.
    double validationCroiseeRepetee(List<Depeche> depeches, Variante v) {
        double somme = 0;
        for (int graine = 1; graine <= NB_REPETITIONS; graine++) {
            List<Depeche> melangees = new ArrayList<>(depeches);
            Collections.shuffle(melangees, new Random(graine));
            somme += validationCroisee(melangees, v);
        }
        return somme / NB_REPETITIONS;
    }

    private record Resultat(Variante variante, double reussite) {
    }

    private void grille(List<Depeche> apprentissage) {
        List<Variante> variantes = new ArrayList<>();
        for (int longueurMin = 1; longueurMin <= 5; longueurMin++) {
            for (int scoreMin : new int[] {GARDER_TOUS, -2, 0, 1, 2, 3}) {
                for (int seuil2 = -6; seuil2 <= 6; seuil2++) {
                    for (int seuil3 = seuil2; seuil3 <= 12; seuil3++) {
                        variantes.add(new Variante(longueurMin, scoreMin, seuil2, seuil3, true));
                        variantes.add(new Variante(longueurMin, scoreMin, seuil2, seuil3, false));
                    }
                }
            }
        }
        Comparator<Resultat> meilleurs = Comparator.comparingDouble(Resultat::reussite).reversed();
        List<Resultat> premierTour = variantes.parallelStream()
                .map(v -> new Resultat(v, validationCroisee(apprentissage, v)))
                .sorted(meilleurs).limit(300).toList();
        List<Resultat> secondTour = premierTour.parallelStream()
                .map(r -> new Resultat(r.variante(), validationCroiseeRepetee(apprentissage, r.variante())))
                .sorted(meilleurs).limit(20).toList();
        System.out.printf("%d variantes ; les 20 meilleures en validation croisée répétée :%n", variantes.size());
        secondTour.forEach(r -> System.out.printf("  %5.1f %%  %s%n", r.reussite(), r.variante()));
    }

    public static void main(String[] args) {
        ArrayList<Depeche> apprentissage = Classification.lectureDepeches("data/corpus/depeches.txt");
        ArrayList<Depeche> test = Classification.lectureDepeches("data/corpus/test.txt");
        ValidationCroisee experience = new ValidationCroisee(Classification.lectureStopWords("data/stopwords-fr.txt"));

        if (args.length > 0 && args[0].equals("grille")) {
            experience.grille(apprentissage);
            return;
        }
        List<Variante> variantes = List.of(
                new Variante(4, GARDER_TOUS, -3, 4, true),  // version d'origine
                new Variante(4, 0, -3, 4, true),            // piste 1
                new Variante(4, 1, -3, 4, true),            // piste 1
                new Variante(4, GARDER_TOUS, 2, 4, true),   // piste 2
                new Variante(3, GARDER_TOUS, -3, 4, true),  // piste 3
                new Variante(3, GARDER_TOUS, 2, 4, true),   // pistes 2 + 3 (retenue)
                new Variante(3, GARDER_TOUS, 1, 2, true),   // meilleure de la grille
                new Variante(3, GARDER_TOUS, 2, 4, false)); // retenue, mots vides gardés
        System.out.println("Validation croisée (10 × 5 blocs) | corpus de test | variante");
        for (Variante v : variantes) {
            System.out.printf("  %5.1f %%  %5.1f %%  %s%n", experience.validationCroiseeRepetee(apprentissage, v),
                    reussite(experience.lexiques(apprentissage, v), test), v);
        }
    }
}
