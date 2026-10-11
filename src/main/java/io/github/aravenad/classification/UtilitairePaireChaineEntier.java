package io.github.aravenad.classification;

import java.util.ArrayList;
import java.util.Comparator;

public class UtilitairePaireChaineEntier {

    private static final Comparator<PaireChaineEntier> ORDRE_ALPHABETIQUE =
            Comparator.comparing(PaireChaineEntier::getChaine);

    public static void trierParChaine(ArrayList<PaireChaineEntier> listePaires) {
        // {} → {listePaires est trié par ordre alphabétique croissant des chaînes}
        // Tri fusion de la bibliothèque standard (stable, O(n log n)) à la place de l'ancien tri à bulles en O(n²).
        listePaires.sort(ORDRE_ALPHABETIQUE);
    }

    public static int indicePourChaine(ArrayList<PaireChaineEntier> listePaires, String chaine) {
        // {listePaires est trié par ordre alphabétique croissant (cf. trierParChaine)}
        // → {résultat = indice de chaine dans listePaires si chaine est présente et -1 sinon via recherche dichotomique}
        int debut = 0;
        int fin = listePaires.size() - 1;
        while (debut <= fin) {
            int milieu = (debut + fin) >>> 1;
            int comparaison = listePaires.get(milieu).getChaine().compareTo(chaine);
            if (comparaison == 0) {
                return milieu;
            } else if (comparaison < 0) {
                debut = milieu + 1;
            } else {
                fin = milieu - 1;
            }
        }
        return -1;
    }

    public static int entierPourChaine(ArrayList<PaireChaineEntier> listePaires, String chaine) {
        // {} → {résultat = entier associé à la chaîne de caractères chaine dans listePaires si elle est présente et 0 sinon}
        for (PaireChaineEntier paire : listePaires) {
            if (paire.getChaine().equals(chaine)) {
                return paire.getEntier();
            }
        }
        return 0;
    }

    public static String chaineMax(ArrayList<PaireChaineEntier> listePaires) {
        // {} → {résultat = chaîne associée au plus grand entier de listePaires (la première en cas d'égalité),
        //       chaîne vide si listePaires est vide}
        String resultat = "";
        int max = Integer.MIN_VALUE;
        for (PaireChaineEntier paire : listePaires) {
            if (paire.getEntier() > max) {
                max = paire.getEntier();
                resultat = paire.getChaine();
            }
        }
        return resultat;
    }

    public static float moyenne(ArrayList<PaireChaineEntier> listePaires) {
        // {} → {résultat = moyenne des entiers stockés dans listePaires, 0 si listePaires est vide}
        if (listePaires.isEmpty()) {
            return 0f;
        }
        float somme = 0f;
        for (PaireChaineEntier paire : listePaires) {
            somme += paire.getEntier();
        }
        return somme / listePaires.size();
    }
}
