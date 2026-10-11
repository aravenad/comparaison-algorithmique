package io.github.aravenad.classification;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BayesNaifTest {

    private static final List<Depeche> APPRENTISSAGE = List.of(
            new Depeche("1", "010124", "SPORTS", "Le club gagne le match de football"),
            new Depeche("2", "010124", "SPORTS", "Match nul pour le club de rugby"),
            new Depeche("3", "010124", "POLITIQUE", "Le ministre défend son projet de loi"),
            new Depeche("4", "010124", "POLITIQUE", "Les députés votent la loi du ministre"));

    private static Depeche depeche(String texte) {
        return new Depeche("x", "010124", "SPORTS", texte);
    }

    @Test
    void predictLaCategorieDontLeVocabulaireEstLePlusProche() {
        BayesNaif bayes = BayesNaif.surMots(Set.of());
        bayes.apprendre(APPRENTISSAGE);

        assertEquals("sports", bayes.categoriePredite(depeche("Un grand match pour le club")));
        assertEquals("politique", bayes.categoriePredite(depeche("La loi du ministre")));
    }

    @Test
    void lesNGrammesReconnaissentDesFormesDeMotsJamaisVues() {
        BayesNaif surMots = BayesNaif.surMots(Set.of());
        BayesNaif surNGrammes = BayesNaif.surNGrammes();
        surMots.apprendre(APPRENTISSAGE);
        surNGrammes.apprendre(APPRENTISSAGE);

        // « footballeurs » et « ministres » n'apparaissent pas tels quels à l'apprentissage.
        assertEquals("sports", surNGrammes.categoriePredite(depeche("Footballeurs")));
        assertEquals("politique", surNGrammes.categoriePredite(depeche("Ministres")));
        // Sur les mots, rien n'est connu : seule la probabilité a priori compte (égale ici), la première catégorie
        // apprise l'emporte.
        assertEquals("sports", surMots.categoriePredite(depeche("Ministres")));
    }

    @Test
    void laProbabiliteAPrioriDepartageQuandRienNEstConnu() {
        BayesNaif bayes = BayesNaif.surMots(Set.of());
        bayes.apprendre(List.of(
                new Depeche("1", "010124", "SPORTS", "match"),
                new Depeche("2", "010124", "CULTURE", "film"),
                new Depeche("3", "010124", "CULTURE", "concert")));

        assertEquals("culture", bayes.categoriePredite(depeche("inconnu")));
    }

    @Test
    void reapprendreRemplaceLeModelePrecedent() {
        BayesNaif bayes = BayesNaif.surMots(Set.of());
        bayes.apprendre(APPRENTISSAGE);
        bayes.apprendre(List.of(new Depeche("1", "010124", "CULTURE", "Un film au festival")));

        assertEquals("culture", bayes.categoriePredite(depeche("Le match du club")));
    }
}
