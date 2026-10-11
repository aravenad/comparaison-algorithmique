package io.github.aravenad.classification;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CaracteristiquesTest {

    @Test
    void motsFiltreLesMotsCourtsEtExclus() {
        Depeche depeche = new Depeche("1", "010124", "SPORTS", "Le PSG gagne le match");
        assertEquals(List.of("psg", "match"), Caracteristiques.mots(depeche, Set.of("gagne"), 3));
    }

    @Test
    void nGrammesDeCaracteresMarquentLeDebutEtLaFinDesMots() {
        Depeche depeche = new Depeche("1", "010124", "SPORTS", "match");
        assertEquals(List.of("<ma", "mat", "atc", "tch", "ch>", "<mat", "matc", "atch", "tch>"),
                Caracteristiques.nGrammesDeCaracteres(depeche, 3, 4));
    }
}
