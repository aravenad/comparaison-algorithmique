package io.github.aravenad.classification;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EvaluationTest {

    private static final List<String> CATEGORIES = List.of("sports", "politique");

    /** Classifieur de test : renvoie la catégorie associée à l'identifiant de la dépêche. */
    private record Reponses(Map<String, String> parId) implements Classifieur {
        public String nom() {
            return "réponses fixes";
        }

        public void apprendre(List<Depeche> depeches) {
        }

        public String categoriePredite(Depeche depeche) {
            return parId.get(depeche.getId());
        }
    }

    private static final List<Depeche> DEPECHES = List.of(
            new Depeche("1", "010124", "SPORTS", "a"),
            new Depeche("2", "010124", "SPORTS", "b"),
            new Depeche("3", "010124", "POLITIQUE", "c"),
            new Depeche("4", "010124", "POLITIQUE", "d"),
            new Depeche("5", "010124", "POLITIQUE", "e"),
            new Depeche("6", "010124", "CULTURE", "f"));

    private static final Reponses REPONSES = new Reponses(Map.of(
            "1", "sports", "2", "politique", "3", "politique", "4", "politique", "5", "sports", "6", "sports"));

    @Test
    void reussiteParCategorieEtMoyenne() {
        Evaluation evaluation = Evaluation.evaluer(REPONSES, DEPECHES, CATEGORIES);

        assertEquals(50.0, evaluation.reussite(0));
        assertEquals(200.0 / 3, evaluation.reussite(1), 1e-9);
        assertEquals((50.0 + 200.0 / 3) / 2, evaluation.moyenne(), 1e-9);
    }

    @Test
    void matriceDeConfusionIgnoreLesCategoriesInconnues() {
        String matrice = Evaluation.evaluer(REPONSES, DEPECHES, CATEGORIES).matriceConfusion();

        List<String> lignes = matrice.lines().toList();
        assertEquals(3, lignes.size());
        assertEquals(List.of("sports", "1", "1"), List.of(lignes.get(1).trim().split("\\s+")));
        assertEquals(List.of("politique", "1", "2"), List.of(lignes.get(2).trim().split("\\s+")));
    }

    @Test
    void ecrireClassementAuFormatDuSujet(@TempDir Path dossier) throws IOException {
        Path fichier = dossier.resolve("classement.txt");
        Evaluation.evaluer(REPONSES, DEPECHES, CATEGORIES).ecrireClassement(fichier.toString());

        assertEquals(List.of("1:SPORTS", "2:POLITIQUE", "3:POLITIQUE", "4:POLITIQUE", "5:SPORTS", "6:SPORTS",
                        "SPORTS : 50%", "POLITIQUE : 67%", "MOYENNE : 58%"),
                Files.readAllLines(fichier, StandardCharsets.UTF_8));
    }

    @Test
    void validationCroiseeNEvalueJamaisSurDesDepechesApprises() {
        // Chaque dépêche n'a qu'un mot, qui lui est propre. Si le bloc évalué avait servi à l'apprentissage, le
        // bayésien naïf reconnaîtrait chaque mot (100 %). Sans fuite, aucun mot n'est connu : toutes les dépêches d'un
        // bloc vont dans la même catégorie, d'où 100 % pour l'une, 0 % pour l'autre, 50 % de moyenne.
        List<Depeche> depeches = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            depeches.add(new Depeche("s" + i, "010124", "SPORTS", "sport" + i));
            depeches.add(new Depeche("p" + i, "010124", "POLITIQUE", "politique" + i));
        }

        double reussite = Evaluation.validationCroisee(() -> BayesNaif.surMots(Set.of()), depeches, CATEGORIES, 5, 2);

        assertEquals(50.0, reussite, 1e-9);
    }
}
