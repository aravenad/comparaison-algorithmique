import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class Categorie {

    private String nom; // le nom de la catégorie p.ex : sport, politique,...
    private ArrayList<PaireChaineEntier> lexique; //le lexique de la catégorie

    // constructeur
    public Categorie(String nom) {
        this.nom = nom;
    }

    public String getNom() {
        return nom;
    }

    public  ArrayList<PaireChaineEntier> getLexique() {
        return lexique;
    }

    // initialisation du lexique de la catégorie à partir du contenu d'un fichier texte
    public void initLexique(String nomFichier) {
        this.lexique = new ArrayList<>();
        try {
            FileInputStream file = new FileInputStream(nomFichier);
            Scanner scanner = new Scanner(file);

            while (scanner.hasNextLine()) {
                String ligne = scanner.nextLine();
                int indexDeuxPoints = ligne.indexOf(":");
                String premierMot = ligne.substring(0, indexDeuxPoints);
                String secondMot = ligne.substring(indexDeuxPoints + 1);
                String trieNb = secondMot.replaceAll("[^0-9]", "");
                int nbTrie = Integer.parseInt(trieNb);
                PaireChaineEntier paire = new PaireChaineEntier(premierMot, nbTrie);
                this.lexique.add(paire);
            }
            scanner.close();
            System.out.printf("%s initialisé.\n", nomFichier);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int score(Depeche d) {
        // {} → {résultat = entier correspondant au score de la dépêche d pour la catégorie sur laquelle l’appel est
        //                  réalisé. Pensez à utiliser l’attribut mots de la classe Depeche correspondant à un tableau
        //                  contenant les mots de la dépêche.}
        int score = 0;
        for (String mot : d.getMots()) {
            for (PaireChaineEntier paire : this.lexique) {
                if (mot.equals(paire.getChaine())) {
                    score += paire.getEntier();
                }
            }
        }
        return score;
    }
}