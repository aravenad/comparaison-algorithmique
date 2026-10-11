package io.github.aravenad.classification;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * L'approche du sujet : un lexique pondéré par catégorie, et la catégorie de plus grand score.
 * Les lexiques sont soit lus dans des fichiers (lexiques manuels), soit appris sur les dépêches d'apprentissage
 * avec initDico, calculScores et poidsPourScore, comme le fait Classification.generationLexique.
 */
public class ClassifieurLexiques implements Classifieur {

    private final String nom;
    private final ArrayList<Categorie> categories;
    private final Set<String> motsExclus; // null : lexiques fixes, apprendre() ne les modifie pas

    public ClassifieurLexiques(String nom, ArrayList<Categorie> categories) {
        this(nom, categories, null);
    }

    private ClassifieurLexiques(String nom, ArrayList<Categorie> categories, Set<String> motsExclus) {
        this.nom = nom;
        this.categories = categories;
        this.motsExclus = motsExclus;
    }

    /** Lexiques écrits à la main, lus dans dossier/&lt;catégorie&gt;.txt. */
    public static ClassifieurLexiques manuels(List<String> nomsCategories, String dossier) {
        ArrayList<Categorie> categories = new ArrayList<>();
        for (String nomCategorie : nomsCategories) {
            Categorie categorie = new Categorie(nomCategorie);
            categorie.initLexique(Paths.get(dossier, nomCategorie + ".txt").toString());
            categories.add(categorie);
        }
        return new ClassifieurLexiques("Lexiques manuels", categories);
    }

    /** Lexiques générés automatiquement à chaque appel de apprendre(). */
    public static ClassifieurLexiques appris(List<String> nomsCategories, Set<String> motsExclus) {
        ArrayList<Categorie> categories = new ArrayList<>();
        for (String nomCategorie : nomsCategories) {
            categories.add(new Categorie(nomCategorie));
        }
        return new ClassifieurLexiques("Lexiques générés", categories, motsExclus);
    }

    @Override
    public String nom() {
        return nom;
    }

    @Override
    public void apprendre(List<Depeche> depeches) {
        if (motsExclus == null) {
            return;
        }
        ArrayList<Depeche> apprentissage = new ArrayList<>(depeches);
        for (Categorie categorie : categories) {
            ArrayList<PaireChaineEntier> dico = Classification.initDico(apprentissage, categorie.getNom(), motsExclus);
            Classification.calculScores(apprentissage, categorie.getNom(), dico);
            for (PaireChaineEntier paire : dico) {
                paire.setEntier(Classification.poidsPourScore(paire.getEntier()));
            }
            categorie.setLexique(dico);
        }
    }

    @Override
    public String categoriePredite(Depeche depeche) {
        ArrayList<PaireChaineEntier> scores = new ArrayList<>();
        for (Categorie categorie : categories) {
            scores.add(new PaireChaineEntier(categorie.getNom(), categorie.score(depeche)));
        }
        return UtilitairePaireChaineEntier.chaineMax(scores).toLowerCase();
    }
}
