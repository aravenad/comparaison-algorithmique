package io.github.aravenad.classification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Classifieur bayésien naïf multinomial avec lissage de Laplace (additif).
 *
 * <p>La catégorie prédite est celle qui maximise log P(c) + Σ log P(t | c) sur les caractéristiques t de la dépêche,
 * avec P(t | c) = (n(t, c) + α) / (n(c) + α |V|), où n(t, c) est le nombre d'occurrences de t dans les dépêches de c,
 * n(c) leur nombre total de caractéristiques et V le vocabulaire. Les caractéristiques jamais vues à l'apprentissage
 * sont ignorées. Contrairement aux lexiques, chaque mot a un poids réel, appris, et peut faire baisser un score.
 */
public class BayesNaif implements Classifieur {

    private final String nom;
    private final Function<Depeche, List<String>> caracteristiques;
    private final double alpha;

    private final LinkedHashMap<String, Double> logPriors = new LinkedHashMap<>();
    private final HashMap<String, Map<String, Double>> logVraisemblances = new HashMap<>();
    private final HashMap<String, Double> logVraisemblanceAbsente = new HashMap<>();
    private final HashSet<String> vocabulaire = new HashSet<>();

    public BayesNaif(String nom, Function<Depeche, List<String>> caracteristiques, double alpha) {
        this.nom = nom;
        this.caracteristiques = caracteristiques;
        this.alpha = alpha;
    }

    /** Sur les mots de plus de 2 lettres hors mots vides (α choisi par validation croisée). */
    public static BayesNaif surMots(Set<String> motsExclus) {
        return new BayesNaif("Bayésien naïf (mots)", d -> Caracteristiques.mots(d, motsExclus, 3), 0.3);
    }

    /** Sur les n-grammes de 3 à 6 caractères de chaque mot (paramètres choisis par validation croisée). */
    public static BayesNaif surNGrammes() {
        return new BayesNaif("Bayésien naïf (n-grammes)", d -> Caracteristiques.nGrammesDeCaracteres(d, 3, 6), 0.1);
    }

    @Override
    public String nom() {
        return nom;
    }

    @Override
    public void apprendre(List<Depeche> depeches) {
        logPriors.clear();
        logVraisemblances.clear();
        logVraisemblanceAbsente.clear();
        vocabulaire.clear();

        LinkedHashMap<String, Integer> nbDepeches = new LinkedHashMap<>();
        HashMap<String, HashMap<String, Integer>> occurrences = new HashMap<>();
        HashMap<String, Integer> totalOccurrences = new HashMap<>();
        for (Depeche depeche : depeches) {
            String categorie = depeche.getCategorie().toLowerCase();
            nbDepeches.merge(categorie, 1, Integer::sum);
            HashMap<String, Integer> occurrencesCategorie = occurrences.computeIfAbsent(categorie, c -> new HashMap<>());
            for (String t : caracteristiques.apply(depeche)) {
                occurrencesCategorie.merge(t, 1, Integer::sum);
                totalOccurrences.merge(categorie, 1, Integer::sum);
                vocabulaire.add(t);
            }
        }

        int tailleVocabulaire = vocabulaire.size();
        for (Map.Entry<String, Integer> entree : nbDepeches.entrySet()) {
            String categorie = entree.getKey();
            logPriors.put(categorie, Math.log((double) entree.getValue() / depeches.size()));
            double denominateur = totalOccurrences.getOrDefault(categorie, 0) + alpha * tailleVocabulaire;
            HashMap<String, Double> logs = new HashMap<>();
            occurrences.get(categorie).forEach((t, n) -> logs.put(t, Math.log((n + alpha) / denominateur)));
            logVraisemblances.put(categorie, logs);
            logVraisemblanceAbsente.put(categorie, Math.log(alpha / denominateur));
        }
    }

    @Override
    public String categoriePredite(Depeche depeche) {
        List<String> ts = new ArrayList<>();
        for (String t : caracteristiques.apply(depeche)) {
            if (vocabulaire.contains(t)) {
                ts.add(t);
            }
        }
        String meilleure = "";
        double meilleurScore = Double.NEGATIVE_INFINITY;
        for (Map.Entry<String, Double> prior : logPriors.entrySet()) {
            String categorie = prior.getKey();
            Map<String, Double> logs = logVraisemblances.get(categorie);
            double absente = logVraisemblanceAbsente.get(categorie);
            double score = prior.getValue();
            for (String t : ts) {
                score += logs.getOrDefault(t, absente);
            }
            if (score > meilleurScore) {
                meilleurScore = score;
                meilleure = categorie;
            }
        }
        return meilleure;
    }
}
