package io.github.aravenad.classification;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Représentations d'une dépêche sous forme de liste de caractéristiques (avec répétitions). */
public final class Caracteristiques {

    private Caracteristiques() {
    }

    /** Les mots de la dépêche d'au moins longueurMin lettres et absents de motsExclus. */
    public static List<String> mots(Depeche depeche, Set<String> motsExclus, int longueurMin) {
        List<String> resultat = new ArrayList<>();
        for (String mot : depeche.getMots()) {
            if (mot.length() >= longueurMin && !motsExclus.contains(mot)) {
                resultat.add(mot);
            }
        }
        return resultat;
    }

    /**
     * Les n-grammes de nMin à nMax caractères de chaque mot, encadré par « &lt; » et « &gt; » pour marquer le début
     * et la fin du mot. P. ex. pour « match » et n = 3 : « &lt;ma », « mat », « atc », « tch », « ch&gt; ».
     * « footballeur » et « football » partagent ainsi la plupart de leurs n-grammes : c'est utile avec un petit corpus
     * d'apprentissage, où beaucoup de mots du test n'ont jamais été vus sous la même forme.
     */
    public static List<String> nGrammesDeCaracteres(Depeche depeche, int nMin, int nMax) {
        List<String> resultat = new ArrayList<>();
        for (String mot : depeche.getMots()) {
            String encadre = "<" + mot + ">";
            for (int n = nMin; n <= nMax; n++) {
                for (int debut = 0; debut + n <= encadre.length(); debut++) {
                    resultat.add(encadre.substring(debut, debut + n));
                }
            }
        }
        return resultat;
    }
}
