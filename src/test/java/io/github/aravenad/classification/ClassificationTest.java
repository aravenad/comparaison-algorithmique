package io.github.aravenad.classification;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ClassificationTest {

    @TempDir
    Path dossier;

    private static ArrayList<Depeche> corpus() {
        return new ArrayList<>(List.of(
                new Depeche("1", "010124", "SPORTS", "Victoire du club au match de football"),
                new Depeche("2", "010124", "SPORTS", "Le club perd son match"),
                new Depeche("3", "010124", "SPORTS", "Football : le club en finale"),
                new Depeche("4", "010124", "POLITIQUE", "Le ministre présente son projet de loi"),
                new Depeche("5", "010124", "POLITIQUE", "Le club des ministres débat de la loi")));
    }

    private Categorie categorie(String nom, String... lignes) throws IOException {
        Path fichier = dossier.resolve(nom + ".txt");
        Files.write(fichier, List.of(lignes), StandardCharsets.UTF_8);
        Categorie categorie = new Categorie(nom);
        categorie.initLexique(fichier.toString());
        return categorie;
    }

    @Test
    void lectureDepechesLitEnTetesEtTexteSurPlusieursLignes() throws IOException {
        Path fichier = dossier.resolve("depeches.txt");
        Files.writeString(fichier, ".N 001\r\n.D 140208\r\n.C SPORTS\r\n.T Première ligne\r\nseconde ligne\r\n\r\n\r\n"
                + ".N 002\n.D 150208\n.C CULTURE\n.T Un film\n", StandardCharsets.UTF_8);

        ArrayList<Depeche> depeches = Classification.lectureDepeches(fichier.toString());

        assertEquals(2, depeches.size());
        assertEquals("001", depeches.get(0).getId());
        assertEquals("140208", depeches.get(0).getDate());
        assertEquals("SPORTS", depeches.get(0).getCategorie());
        assertEquals("Première ligne\nseconde ligne", depeches.get(0).getContenu());
        assertEquals(List.of("un", "film"), depeches.get(1).getMots());
    }

    @Test
    void initDicoRenvoieLesMotsDistinctsTriesDeLaCategorie() {
        ArrayList<PaireChaineEntier> dico = Classification.initDico(corpus(), "sports", Set.of("victoire"));

        // « le », « du », « au »… sont trop courts ; « victoire » est un mot exclu ; « ministre » n'est pas dans SPORTS.
        assertEquals(List.of("club", "finale", "football", "match", "perd"),
                dico.stream().map(PaireChaineEntier::getChaine).toList());
        dico.forEach(paire -> assertEquals(0, paire.getEntier()));
    }

    @Test
    void calculScoresIncrementeDansLaCategorieEtDecrementeAilleurs() {
        ArrayList<PaireChaineEntier> dico = Classification.initDico(corpus(), "sports", Set.of());
        Classification.calculScores(corpus(), "sports", dico);

        assertEquals(3 - 1, UtilitairePaireChaineEntier.entierPourChaine(dico, "club"));
        assertEquals(2, UtilitairePaireChaineEntier.entierPourChaine(dico, "match"));
    }

    @Test
    void poidsPourScore() {
        assertEquals(1, Classification.poidsPourScore(-10));
        assertEquals(2, Classification.poidsPourScore(0));
        assertEquals(3, Classification.poidsPourScore(4));
    }

    @Test
    void classementDepechesCalculeLesPourcentagesSurLeNombreDeDepechesDeChaqueCategorie() throws IOException {
        ArrayList<Categorie> categories = new ArrayList<>(List.of(
                categorie("sports", "match:3", "football:3"),
                categorie("politique", "ministre:3", "ministres:3", "loi:2")));
        Path sortie = dossier.resolve("resultats/classement.txt");

        ArrayList<PaireChaineEntier> pourcentages =
                Classification.classementDepeches(corpus(), categories, sortie.toString());

        // Sports : dépêches 1, 2 et 3 bien classées ; politique : 4 et 5 bien classées.
        assertEquals(100, UtilitairePaireChaineEntier.entierPourChaine(pourcentages, "sports"));
        assertEquals(100, UtilitairePaireChaineEntier.entierPourChaine(pourcentages, "politique"));
        assertEquals(List.of("1:SPORTS", "2:SPORTS", "3:SPORTS", "4:POLITIQUE", "5:POLITIQUE",
                        "SPORTS : 100%", "POLITIQUE : 100%", "MOYENNE : 100%"),
                Files.readAllLines(sortie, StandardCharsets.UTF_8));
    }

    @Test
    void classementDepechesNeSupposePasCentDepechesParCategorie() throws IOException {
        // Seul « match » est connu : la dépêche 3 (sports) et les dépêches politiques ont un score nul partout
        // et sont attribuées à la première catégorie, sports.
        ArrayList<Categorie> categories = new ArrayList<>(List.of(
                categorie("sports", "match:1"),
                categorie("politique", "projet:1")));

        ArrayList<PaireChaineEntier> pourcentages = Classification.classementDepeches(
                corpus(), categories, dossier.resolve("classement.txt").toString());

        assertEquals(100, UtilitairePaireChaineEntier.entierPourChaine(pourcentages, "sports"));
        assertEquals(50, UtilitairePaireChaineEntier.entierPourChaine(pourcentages, "politique"));
    }
}
