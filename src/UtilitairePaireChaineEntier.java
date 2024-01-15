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
        return 0;
    }

    public static String chaineMax(ArrayList<PaireChaineEntier> listePaires) {
        return "SPORTS";
    }


    public static float moyenne(ArrayList<PaireChaineEntier> listePaires) {
        int i=0;
        int moy=0;
        while (i<listePaires.size()){
            moy+=listePaires.get(i).getEntier();
        }
        return moy/i;
    }

}
