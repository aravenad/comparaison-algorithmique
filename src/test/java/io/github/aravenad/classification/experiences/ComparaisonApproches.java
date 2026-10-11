package io.github.aravenad.classification.experiences;

import io.github.aravenad.classification.BayesNaif;
import io.github.aravenad.classification.Caracteristiques;
import io.github.aravenad.classification.Classification;
import io.github.aravenad.classification.Classifieur;
import io.github.aravenad.classification.ClassifieurLexiques;
import io.github.aravenad.classification.Depeche;
import io.github.aravenad.classification.Evaluation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Compare les approches de classification par validation croisée sur le corpus d'apprentissage, puis sur le corpus
 * de test (cf. docs/analyse.md, section « Autres approches »).
 *
 * <p>Se lance à la main, depuis la racine du projet, après {@code ./mvnw test-compile} :
 * <pre>
 * java -cp target/classes:target/test-classes io.github.aravenad.classification.experiences.ComparaisonApproches
 * </pre>
 * Les approches à base de vecteurs TF-IDF (centroïdes, k plus proches voisins, régression logistique) ne sont
 * implémentées qu'ici : elles ne font pas mieux que le bayésien naïf, plus simple, qui est seul gardé dans le programme.
 */
public class ComparaisonApproches {

    private static final List<String> CATEGORIES =
            List.of("environnement-sciences", "culture", "economie", "politique", "sports");

    /** Vecteurs TF-IDF normalisés (tf sous-linéaire 1 + log tf, idf lissé), appris sur un corpus. */
    static class TfIdf {
        private final Function<Depeche, List<String>> caracteristiques;
        private final HashMap<String, Double> idf = new HashMap<>();

        TfIdf(Function<Depeche, List<String>> caracteristiques) {
            this.caracteristiques = caracteristiques;
        }

        void apprendre(List<Depeche> depeches) {
            idf.clear();
            HashMap<String, Integer> nbDepechesAvec = new HashMap<>();
            for (Depeche d : depeches) {
                for (String t : new HashSet<>(caracteristiques.apply(d))) {
                    nbDepechesAvec.merge(t, 1, Integer::sum);
                }
            }
            nbDepechesAvec.forEach((t, n) -> idf.put(t, Math.log((1.0 + depeches.size()) / (1.0 + n)) + 1));
        }

        Map<String, Double> vecteur(Depeche d) {
            HashMap<String, Integer> tf = new HashMap<>();
            for (String t : caracteristiques.apply(d)) {
                tf.merge(t, 1, Integer::sum);
            }
            HashMap<String, Double> v = new HashMap<>();
            tf.forEach((t, n) -> {
                Double i = idf.get(t);
                if (i != null) {
                    v.put(t, (1 + Math.log(n)) * i);
                }
            });
            return normaliser(v);
        }

        static Map<String, Double> normaliser(Map<String, Double> v) {
            double norme = Math.sqrt(v.values().stream().mapToDouble(x -> x * x).sum());
            if (norme > 0) {
                v.replaceAll((t, x) -> x / norme);
            }
            return v;
        }

        static double produitScalaire(Map<String, Double> a, Map<String, Double> b) {
            if (a.size() > b.size()) {
                Map<String, Double> c = a;
                a = b;
                b = c;
            }
            double somme = 0;
            for (Map.Entry<String, Double> e : a.entrySet()) {
                somme += e.getValue() * b.getOrDefault(e.getKey(), 0.0);
            }
            return somme;
        }
    }

    /** Méthode de Rocchio : un vecteur moyen par catégorie, la catégorie prédite est celle du plus proche (cosinus). */
    static class Centroides implements Classifieur {
        private final TfIdf tfIdf;
        private final LinkedHashMap<String, Map<String, Double>> centroides = new LinkedHashMap<>();

        Centroides(Function<Depeche, List<String>> caracteristiques) {
            this.tfIdf = new TfIdf(caracteristiques);
        }

        public String nom() {
            return "Centroïdes TF-IDF";
        }

        public void apprendre(List<Depeche> depeches) {
            tfIdf.apprendre(depeches);
            centroides.clear();
            for (Depeche d : depeches) {
                Map<String, Double> c = centroides.computeIfAbsent(d.getCategorie().toLowerCase(), k -> new HashMap<>());
                tfIdf.vecteur(d).forEach((t, x) -> c.merge(t, x, Double::sum));
            }
            centroides.values().forEach(TfIdf::normaliser);
        }

        public String categoriePredite(Depeche d) {
            Map<String, Double> v = tfIdf.vecteur(d);
            String meilleure = "";
            double max = Double.NEGATIVE_INFINITY;
            for (Map.Entry<String, Map<String, Double>> c : centroides.entrySet()) {
                double s = TfIdf.produitScalaire(v, c.getValue());
                if (s > max) {
                    max = s;
                    meilleure = c.getKey();
                }
            }
            return meilleure;
        }
    }

    /** k plus proches voisins (cosinus), votes pondérés par la similarité. */
    static class PlusProchesVoisins implements Classifieur {
        private final TfIdf tfIdf;
        private final int k;
        private final List<Map<String, Double>> vecteurs = new ArrayList<>();
        private final List<String> etiquettes = new ArrayList<>();

        PlusProchesVoisins(Function<Depeche, List<String>> caracteristiques, int k) {
            this.tfIdf = new TfIdf(caracteristiques);
            this.k = k;
        }

        public String nom() {
            return k + " plus proches voisins";
        }

        public void apprendre(List<Depeche> depeches) {
            tfIdf.apprendre(depeches);
            vecteurs.clear();
            etiquettes.clear();
            for (Depeche d : depeches) {
                vecteurs.add(tfIdf.vecteur(d));
                etiquettes.add(d.getCategorie().toLowerCase());
            }
        }

        public String categoriePredite(Depeche d) {
            Map<String, Double> v = tfIdf.vecteur(d);
            Integer[] indices = new Integer[vecteurs.size()];
            double[] similarites = new double[vecteurs.size()];
            for (int i = 0; i < indices.length; i++) {
                indices[i] = i;
                similarites[i] = TfIdf.produitScalaire(v, vecteurs.get(i));
            }
            Arrays.sort(indices, (a, b) -> Double.compare(similarites[b], similarites[a]));
            LinkedHashMap<String, Double> votes = new LinkedHashMap<>();
            for (int i = 0; i < Math.min(k, indices.length); i++) {
                votes.merge(etiquettes.get(indices[i]), similarites[indices[i]], Double::sum);
            }
            return Collections.max(votes.entrySet(), Map.Entry.comparingByValue()).getKey();
        }
    }

    /** Régression logistique multinomiale (softmax) sur TF-IDF, apprise par descente de gradient stochastique. */
    static class RegressionLogistique implements Classifieur {
        private static final double PAS = 0.5;
        private static final int EPOQUES = 15;
        private final TfIdf tfIdf;
        private final List<String> classes = new ArrayList<>();
        private final List<HashMap<String, Double>> poids = new ArrayList<>();
        private double[] biais;

        RegressionLogistique(Function<Depeche, List<String>> caracteristiques) {
            this.tfIdf = new TfIdf(caracteristiques);
        }

        public String nom() {
            return "Régression logistique";
        }

        public void apprendre(List<Depeche> depeches) {
            tfIdf.apprendre(depeches);
            classes.clear();
            poids.clear();
            depeches.stream().map(d -> d.getCategorie().toLowerCase()).distinct().forEach(c -> {
                classes.add(c);
                poids.add(new HashMap<>());
            });
            biais = new double[classes.size()];
            List<Map<String, Double>> x = depeches.stream().map(tfIdf::vecteur).toList();
            List<Integer> ordre = new ArrayList<>();
            for (int i = 0; i < depeches.size(); i++) {
                ordre.add(i);
            }
            Random aleatoire = new Random(42);
            for (int epoque = 0; epoque < EPOQUES; epoque++) {
                Collections.shuffle(ordre, aleatoire);
                for (int i : ordre) {
                    double[] p = probabilites(x.get(i));
                    int reelle = classes.indexOf(depeches.get(i).getCategorie().toLowerCase());
                    for (int c = 0; c < classes.size(); c++) {
                        double gradient = p[c] - (c == reelle ? 1 : 0);
                        HashMap<String, Double> w = poids.get(c);
                        x.get(i).forEach((t, valeur) -> w.merge(t, -PAS * gradient * valeur, Double::sum));
                        biais[c] -= PAS * gradient;
                    }
                }
            }
        }

        private double[] probabilites(Map<String, Double> v) {
            double[] z = new double[classes.size()];
            double max = Double.NEGATIVE_INFINITY;
            for (int c = 0; c < z.length; c++) {
                HashMap<String, Double> w = poids.get(c);
                double s = biais[c];
                for (Map.Entry<String, Double> e : v.entrySet()) {
                    s += w.getOrDefault(e.getKey(), 0.0) * e.getValue();
                }
                z[c] = s;
                max = Math.max(max, s);
            }
            double somme = 0;
            for (int c = 0; c < z.length; c++) {
                z[c] = Math.exp(z[c] - max);
                somme += z[c];
            }
            for (int c = 0; c < z.length; c++) {
                z[c] /= somme;
            }
            return z;
        }

        public String categoriePredite(Depeche d) {
            double[] p = probabilites(tfIdf.vecteur(d));
            int meilleure = 0;
            for (int c = 1; c < p.length; c++) {
                if (p[c] > p[meilleure]) {
                    meilleure = c;
                }
            }
            return classes.get(meilleure);
        }
    }

    public static void main(String[] args) {
        ArrayList<Depeche> apprentissage = Classification.lectureDepeches("data/corpus/depeches.txt");
        ArrayList<Depeche> test = Classification.lectureDepeches("data/corpus/test.txt");
        Set<String> motsExclus = Classification.lectureStopWords("data/stopwords-fr.txt");
        Function<Depeche, List<String>> mots = d -> Caracteristiques.mots(d, motsExclus, 3);

        List<Supplier<Classifieur>> approches = List.of(
                () -> ClassifieurLexiques.appris(CATEGORIES, motsExclus),
                () -> BayesNaif.surMots(motsExclus),
                BayesNaif::surNGrammes,
                () -> new Centroides(mots),
                () -> new PlusProchesVoisins(mots, 15),
                () -> new RegressionLogistique(mots));

        System.out.println("Validation croisée (10 × 5 blocs) | corpus de test | approche");
        for (Supplier<Classifieur> fabrique : approches) {
            double validation = Evaluation.validationCroisee(fabrique, apprentissage, CATEGORIES, 5, 10);
            Classifieur classifieur = fabrique.get();
            classifieur.apprendre(apprentissage);
            double surTest = Evaluation.evaluer(classifieur, test, CATEGORIES).moyenne();
            System.out.printf("  %5.1f %%  %5.1f %%  %s%n", validation, surTest, classifieur.nom());
        }
    }
}
