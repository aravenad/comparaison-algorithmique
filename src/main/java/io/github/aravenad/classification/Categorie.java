package io.github.aravenad.classification;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

public class Categorie {

    private final String nom; // le nom de la catégorie p.ex : sport, politique,...
    private ArrayList<PaireChaineEntier> lexique = new ArrayList<>(); //le lexique de la catégorie
    private HashMap<String, Integer> poidsParMot = new HashMap<>(); // index du lexique pour score(), en O(1) par mot

    // constructeur
    public Categorie(String nom) {
        this.nom = nom;
    }

    public String getNom() {
        return nom;
    }

    public ArrayList<PaireChaineEntier> getLexique() {
        return lexique;
    }

    // initialisation du lexique de la catégorie à partir du contenu d'un fichier texte
    public void initLexique(String nomFichier) {
        // {initialise l'attribut lexique (une ArrayList<PaireChaineEntier>) à partir du fichier lexique dont le nom
        //  nomFichier est passé en paramètre.}
        // Format attendu : une entrée « mot:poids » par ligne. Les mots sont mis en minuscules, comme ceux des
        // dépêches, sinon les entrées « France », « PIB », « NASA »… des lexiques manuels ne seraient jamais trouvées.
        ArrayList<PaireChaineEntier> nouveauLexique = new ArrayList<>();
        try (BufferedReader lecteur = Files.newBufferedReader(Paths.get(nomFichier), StandardCharsets.UTF_8)) {
            String ligne;
            int numeroLigne = 0;
            while ((ligne = lecteur.readLine()) != null) {
                numeroLigne++;
                ligne = ligne.strip();
                if (ligne.isEmpty()) {
                    continue;
                }
                int indexDeuxPoints = ligne.lastIndexOf(':');
                if (indexDeuxPoints <= 0) {
                    throw new IllegalArgumentException(
                            String.format("%s:%d : entrée « %s » invalide, « mot:poids » attendu", nomFichier, numeroLigne, ligne));
                }
                String mot = ligne.substring(0, indexDeuxPoints).strip().toLowerCase();
                int poids = Integer.parseInt(ligne.substring(indexDeuxPoints + 1).strip());
                nouveauLexique.add(new PaireChaineEntier(mot, poids));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Lecture du lexique " + nomFichier + " impossible", e);
        }
        setLexique(nouveauLexique);
    }

    // remplace le lexique de la catégorie, p. ex. par un lexique appris en mémoire (cf. ClassifieurLexiques)
    public void setLexique(ArrayList<PaireChaineEntier> lexique) {
        HashMap<String, Integer> nouvelIndex = new HashMap<>();
        for (PaireChaineEntier paire : lexique) {
            nouvelIndex.merge(paire.getChaine(), paire.getEntier(), Integer::sum);
        }
        this.lexique = lexique;
        this.poidsParMot = nouvelIndex;
    }

    public int score(Depeche d) {
        // {} → {résultat = entier correspondant au score de la dépêche d pour la catégorie sur laquelle l’appel est
        //                  réalisé. Pensez à utiliser l’attribut mots de la classe Depeche correspondant à un tableau
        //                  contenant les mots de la dépêche.}
        // Complexité : O(|mots|) grâce à l'index, au lieu de O(|mots| × |lexique|) avec un double parcours.
        int score = 0;
        for (String mot : d.getMots()) {
            score += poidsParMot.getOrDefault(mot, 0);
        }
        return score;
    }
}
