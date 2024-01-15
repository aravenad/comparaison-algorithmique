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
                PaireChaineEntier paire = new PaireChaineEntier(premierMot, Integer.parseInt(secondMot));
                this.lexique.add(paire);
            }
            scanner.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    //calcul du score d'une dépêche pour la catégorie
    public static int score(Depeche d) {
        int i=0;
        int sc=0;
        Categorie cat=new Categorie(d.getCategorie());
        cat.initLexique("./"+d.getCategorie().toLowerCase()+".txt");
        while (i<d.getMots().size()){
            for (int y=0;y<cat.getLexique().size();y++) {
                if (d.getMots().get(i).compareTo(cat.getLexique().get(y).getChaine())==0) {
                    sc += cat.getLexique().get(y).getEntier();
                }
            }
            i++;
        }
        return sc;
    }

}
