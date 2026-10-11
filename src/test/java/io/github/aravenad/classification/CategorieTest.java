package io.github.aravenad.classification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CategorieTest {

    @TempDir
    Path dossier;

    private Categorie categorieAvecLexique(String... lignes) throws IOException {
        Path fichier = dossier.resolve("lexique.txt");
        Files.write(fichier, List.of(lignes), StandardCharsets.UTF_8);
        Categorie categorie = new Categorie("sports");
        categorie.initLexique(fichier.toString());
        return categorie;
    }

    @Test
    void initLexiqueLitLesPairesEtIgnoreLesLignesVides() throws IOException {
        Categorie categorie = categorieAvecLexique("but:3", "", " équipe : 2 ");
        assertEquals(2, categorie.getLexique().size());
        assertEquals("équipe", categorie.getLexique().get(1).getChaine());
        assertEquals(2, categorie.getLexique().get(1).getEntier());
    }

    @Test
    void initLexiqueRefuseUneLigneSansPoids() {
        assertThrows(IllegalArgumentException.class, () -> categorieAvecLexique("but:3", "match"));
    }

    @Test
    void scoreAdditionneLesPoidsDeChaqueOccurrence() throws IOException {
        Categorie categorie = categorieAvecLexique("but:3", "match:2");
        Depeche depeche = new Depeche("1", "010124", "SPORTS", "Un but, puis un autre but en fin de match.");
        assertEquals(3 + 3 + 2, categorie.score(depeche));
    }

    @Test
    void scoreTrouveLesEntreesDuLexiqueEcritesEnMajuscules() throws IOException {
        // Ancien bug : « PSG » dans le lexique ne correspondait jamais à « psg » dans la dépêche (mise en minuscules).
        Categorie categorie = categorieAvecLexique("PSG:3", "Benzema:3");
        Depeche depeche = new Depeche("1", "010124", "SPORTS", "Le PSG recrute Benzema.");
        assertEquals(6, categorie.score(depeche));
    }
}
