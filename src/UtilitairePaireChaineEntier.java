import java.util.ArrayList;

public class UtilitairePaireChaineEntier {

    public static int indicePourChaine(ArrayList<PaireChaineEntier> listePaires, String chaine) {
        // {} → {résultat = indice de chaine dans listePaires si chaine est présente et -1 sinon}
        int i = 0;
        while (i < listePaires.size() && listePaires.get(i).getChaine().compareTo(chaine) != 0){
            i++;
        }
        if (i == listePaires.size()){
            return -1;
        } else {
            return i;
        }

    }

    public static int entierPourChaine(ArrayList<PaireChaineEntier> listePaires, String chaine) {
        // {} → {résultat = entier associé à la chaîne de caractères chaine dans listePaires si elle est présente et 0 sinon}
        int resultat = 0;
        for (PaireChaineEntier paire : listePaires) {
            if (paire.getChaine().equals(chaine)) {
                resultat = paire.getEntier();
            }
        }
        return resultat;
    }

    public static String chaineMax(ArrayList<PaireChaineEntier> listePaires) {
        // {} → {résultat = chaîne associée au plus grand entier de listePaires}
        String resultat = "";
        int max = 0;
        for (PaireChaineEntier paire : listePaires) {
            if (paire.getEntier() > max) {
                max = paire.getEntier();
                resultat = paire.getChaine();
            }
        }
        return resultat;
    }

    public static float moyenne(ArrayList<PaireChaineEntier> listePaires) {
        // {} → {résultat = moyenne des entiers stockés dans listePaires}
        int i = 0;
        float moy = 0.0f;
        while (i < listePaires.size()){
            moy += listePaires.get(i).getEntier();
            i++;
        }
        return moy / i;
    }
}