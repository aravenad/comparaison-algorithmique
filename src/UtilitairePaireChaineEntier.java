import java.util.ArrayList;

public class UtilitairePaireChaineEntier {

    private static void triBullesAmeliore(ArrayList<PaireChaineEntier> v) {
        // {} → {v est trié par ordre alphabétique croissant}
        int j;
        int i = 0;
        boolean onAPermute = true;
        while (onAPermute) {
            j = v.size() - 1;
            onAPermute = false;
            while (j > i) {
                if (v.get(j).getChaine().compareToIgnoreCase(v.get(j-1).getChaine()) < 0) {
                    PaireChaineEntier temporaire = v.get(j);
                    v.set(j, v.get(j - 1));
                    v.set(j - 1, temporaire);
                    onAPermute = true;
                }
                j = j - 1;
            }
            i = i + 1;
        }
    }
    public static int indicePourChaine(ArrayList<PaireChaineEntier> listePaires, String chaine) {
        // {} → {résultat = indice de chaine dans listePaires si chaine est présente et -1 sinon}
        triBullesAmeliore(listePaires);
        int inf = 0;
        int sup = listePaires.size();
        int m;
        while (inf < sup) {
            m = (inf + sup) / 2;
            if (listePaires.get(m).getChaine().compareToIgnoreCase(chaine) >= 0) {
                sup = m;
            } else {
                inf = m + 1;
            }
        }
        if (listePaires.get(sup).getChaine().compareToIgnoreCase(chaine) == 0) {
            return sup;
        } else {
            return -1;
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