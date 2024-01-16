import java.util.ArrayList;

public class UtilitairePaireChaineEntier {

    public static int indicePourChaine(ArrayList<PaireChaineEntier> listePaires, String chaine) {
        int i=0;
        while (i< listePaires.size()&&listePaires.get(i).getChaine().compareTo(chaine)!=0){
            i++;
        }
        if (i==listePaires.size()){
            return -1;
        }else {
            return i;
        }

    }

    public static int entierPourChaine(ArrayList<PaireChaineEntier> listePaires, String chaine) {
        int resultat = 0;
        for (PaireChaineEntier paire : listePaires) {
            if (paire.getChaine().equals(chaine)) {
                resultat = paire.getEntier();
            }
        }
        return resultat;
    }

    public static String chaineMax(ArrayList<PaireChaineEntier> listePaires) {
        // {} → {résultat = la chaîne associée au plus grand entier de listePaires}
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
        int i=0;
        int moy=0;
        while (i<listePaires.size()){
            moy+=listePaires.get(i).getEntier();
            i++;
        }
        return moy/i;
    }

}
