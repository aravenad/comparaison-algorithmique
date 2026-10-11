package io.github.aravenad.classification;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class UtilitairePaireChaineEntierTest {

    private static ArrayList<PaireChaineEntier> paires(Object... chainesEtEntiers) {
        ArrayList<PaireChaineEntier> liste = new ArrayList<>();
        for (int i = 0; i < chainesEtEntiers.length; i += 2) {
            liste.add(new PaireChaineEntier((String) chainesEtEntiers[i], (Integer) chainesEtEntiers[i + 1]));
        }
        return liste;
    }

    @Test
    void trierParChaineTrieParOrdreAlphabetique() {
        ArrayList<PaireChaineEntier> liste = paires("musée", 1, "art", 2, "cinéma", 3);
        UtilitairePaireChaineEntier.trierParChaine(liste);
        assertEquals(List.of("art", "cinéma", "musée"), liste.stream().map(PaireChaineEntier::getChaine).toList());
    }

    @Test
    void indicePourChaineTrouveChaqueElementDUneListeTriee() {
        ArrayList<PaireChaineEntier> liste = paires("art", 0, "cinéma", 0, "musée", 0, "opéra", 0);
        for (int i = 0; i < liste.size(); i++) {
            assertEquals(i, UtilitairePaireChaineEntier.indicePourChaine(liste, liste.get(i).getChaine()));
        }
    }

    @Test
    void indicePourChaineRenvoieMoinsUnSiAbsente() {
        ArrayList<PaireChaineEntier> liste = paires("art", 0, "musée", 0);
        assertEquals(-1, UtilitairePaireChaineEntier.indicePourChaine(liste, "zoo"));
        assertEquals(-1, UtilitairePaireChaineEntier.indicePourChaine(liste, "a"));
        assertEquals(-1, UtilitairePaireChaineEntier.indicePourChaine(new ArrayList<>(), "art"));
    }

    @Test
    void entierPourChaine() {
        ArrayList<PaireChaineEntier> liste = paires("but", 3, "match", 2);
        assertEquals(2, UtilitairePaireChaineEntier.entierPourChaine(liste, "match"));
        assertEquals(0, UtilitairePaireChaineEntier.entierPourChaine(liste, "élection"));
    }

    @Test
    void chaineMaxRenvoieLaPremiereEnCasDEgalite() {
        assertEquals("culture", UtilitairePaireChaineEntier.chaineMax(paires("sports", 1, "culture", 5, "economie", 5)));
    }

    @Test
    void chaineMaxGereLesScoresNulsOuNegatifs() {
        // Ancien bug : max initialisé à 0, donc chaîne vide renvoyée si aucun score n'est strictement positif.
        assertEquals("sports", UtilitairePaireChaineEntier.chaineMax(paires("sports", 0, "culture", 0)));
        assertEquals("culture", UtilitairePaireChaineEntier.chaineMax(paires("sports", -3, "culture", -1)));
        assertEquals("", UtilitairePaireChaineEntier.chaineMax(new ArrayList<>()));
    }

    @Test
    void moyenne() {
        assertEquals(2.5f, UtilitairePaireChaineEntier.moyenne(paires("a", 1, "b", 4)));
        assertEquals(0f, UtilitairePaireChaineEntier.moyenne(new ArrayList<>()));
    }
}
