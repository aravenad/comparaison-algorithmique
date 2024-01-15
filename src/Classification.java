import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class Classification {


    private static ArrayList<Depeche> lectureDepeches(String nomFichier) {
        //creation d'un tableau de dépêches
        ArrayList<Depeche> depeches = new ArrayList<>();
        try {
            // lecture du fichier d'entrée
            FileInputStream file = new FileInputStream(nomFichier);
            Scanner scanner = new Scanner(file);

            while (scanner.hasNextLine()) {
                String ligne = scanner.nextLine();
                String id = ligne.substring(3);
                ligne = scanner.nextLine();
                String date = ligne.substring(3);
                ligne = scanner.nextLine();
                String categorie = ligne.substring(3);
                ligne = scanner.nextLine();
                String lignes = ligne.substring(3);
                while (scanner.hasNextLine() && !ligne.equals("")) {
                    ligne = scanner.nextLine();
                    if (!ligne.equals("")) {
                        lignes = lignes + '\n' + ligne;
                    }
                }
                Depeche uneDepeche = new Depeche(id, date, categorie, lignes);
                depeches.add(uneDepeche);
            }
            scanner.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return depeches;
    }


    public static void classementDepeches(ArrayList<Depeche> depeches, ArrayList<Categorie> categories, String nomFichier) {
    }


    public static ArrayList<PaireChaineEntier> initDico(ArrayList<Depeche> depeches, String categorie) {
        ArrayList<PaireChaineEntier> resultat = new ArrayList<>();
        return resultat;

    }

    public static void calculScores(ArrayList<Depeche> depeches, String categorie, ArrayList<PaireChaineEntier> dictionnaire) {
    }

    public static int poidsPourScore(int score) {
        return 0;
    }

    public static void generationLexique(ArrayList<Depeche> depeches, String categorie, String nomFichier) {

    }

    public static void main(String[] args) {

        //Chargement des dépêches en mémoire
        System.out.println("chargement des dépêches");
        ArrayList<Depeche> depeches = lectureDepeches("./depeches.txt");

        for (Depeche depeche : depeches) {
            depeche.afficher();
        }

        Categorie culture = new Categorie("culture");
        culture.initLexique("./culture.txt");

        Categorie economie = new Categorie("economie");
        economie.initLexique("./economie.txt");

        Categorie environnementSciences = new Categorie("environnement-sciences");
        environnementSciences.initLexique("./environnement-sciences.txt");

        Categorie politique = new Categorie("politique");
        politique.initLexique("./politique.txt");

        Categorie sports = new Categorie("sports");
        sports.initLexique("./sports.txt");
        System.out.printf("Lexique de la catégorie %s\n", sports.getNom());
        for (int i = 0; i < sports.getLexique().size(); i++) {
            System.out.printf("%s (score : %d)\n",
                    sports.getLexique().get(i).getChaine(),
                    sports.getLexique().get(i).getEntier());
        }

        Scanner scanner = new Scanner(System.in);
        System.out.print("\nEntrez un mot : ");
        String mot = scanner.nextLine();
        System.out.printf("Le poids associé à %s est %d\n\n",
                mot,
                UtilitairePaireChaineEntier.entierPourChaine(sports.getLexique(), mot));

        ArrayList<Categorie> vCategories = new ArrayList<>(Arrays.asList(culture, economie, environnementSciences, politique, sports));
        ArrayList<PaireChaineEntier> vScores = new ArrayList<>();
        for (Categorie categorie : vCategories) {
            vScores.add(new PaireChaineEntier(categorie.getNom(), categorie.score(depeches.get((int) (Math.random() * 500)))));
        }
        for (PaireChaineEntier score : vScores) {
            System.out.printf("Score de la catégorie %s : %d\n", score.getChaine(), score.getEntier());
        }
        System.out.println("Prédiction de la catégorie de la dépêche : " + depeches.get((int) (Math.random() * 500)).getId());
        System.out.printf("La catégorie prédite est %s\n", UtilitairePaireChaineEntier.chaineMax(vScores).toUpperCase());
    }
}

