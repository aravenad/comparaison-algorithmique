import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
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
        ArrayList<PaireChaineEntier> vScores = new ArrayList<>();
        ArrayList<PaireChaineEntier> moy = new ArrayList<>();
        ArrayList<String>catpredit=new ArrayList<>();
        int cmp;
        try {
            FileWriter file = new FileWriter(nomFichier);
            for (int n = 0; n < depeches.size(); n++) {
                vScores.clear();
                for (Categorie categorie : categories) {
                    vScores.add(new PaireChaineEntier(categorie.getNom(), categorie.score(depeches.get(n))));
                }
                String cat = UtilitairePaireChaineEntier.chaineMax(vScores).toUpperCase();
                file.write(depeches.get(n).getId() + ":" + cat + "\n");
                catpredit.add(cat);
            }

            for (int p=0;p<categories.size();p++) {
                cmp=0;
                String nomcat=categories.get(p).getNom();
                int i=0;
                while (i < depeches.size()) {
                    if (depeches.get(i).getCategorie().compareToIgnoreCase(nomcat) == 0) {
                        if (depeches.get(i).getCategorie().compareToIgnoreCase(catpredit.get(i)) == 0) {
                            cmp+=1;
                        }
                    }
                    i++;
                }
                file.write(categories.get(p).getNom().toUpperCase()+" : "+cmp+"%"+"\n");
                PaireChaineEntier calcmoy=new PaireChaineEntier(categories.get(p).getNom(),cmp);
                moy.add(calcmoy);
            }
            int moyenne = (int) UtilitairePaireChaineEntier.moyenne(moy);
            file.write("MOYENNE : "+moyenne+"%");
            file.close();
            System.out.println("Votre saisie a été écrite avec succès dans fichier-sortie.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static ArrayList<PaireChaineEntier> initDico(ArrayList<Depeche> depeches, String categorie) {
        ArrayList<PaireChaineEntier> resultat = new ArrayList<>();
        int i=0;
        while (i<depeches.size()&&depeches.get(i).getCategorie().compareToIgnoreCase(categorie)==0){
            for (int y=0;y<depeches.get(i).getMots().size();y++){
                if (!resultat.contains(depeches.get(i).getMots().get(y))){
                    PaireChaineEntier mots=new PaireChaineEntier(depeches.get(i).getMots().get(y),0);
                    resultat.add(mots);
                }
            }
        }
        return resultat;
    }

    public static void calculScores(ArrayList<Depeche> depeches, String categorie, ArrayList<PaireChaineEntier> dictionnaire) {
        ArrayList<PaireChaineEntier>dico=dictionnaire;
        int i=0;
        int score=0;
        while (i<depeches.size()){
            for (int y=0;y<depeches.get(i).getMots().size();y++){
                score=0;
                for (int p=0;p<dictionnaire.size();p++){
                    if (depeches.get(i).getMots().get(y).compareToIgnoreCase(dictionnaire.get(p).getChaine())==0
                    && depeches.get(i).getCategorie().compareToIgnoreCase(categorie)==0){
                        score+=1;
                    }else {
                        score-=1;
                    }
                    PaireChaineEntier temp=new PaireChaineEntier(dico.get(p).getChaine(),score);
                    dico.set(p,temp);
                }
            }
            i++;
        }
    }

    public static int poidsPourScore(int score) {
        if (score>=4){
            return 3;
        } else if (score<=-4) {
            return 1;
        }else {
            return 2;
        }
    }

    public static void generationLexique(ArrayList<Depeche> depeches, String categorie, String nomFichier) {
        try {
            ArrayList<PaireChaineEntier>dico=initDico(depeches,categorie);
            calculScores(depeches,categorie,dico);
            for (int y=0;y<dico.size();y++){
                poidsPourScore(dico.get(y).getEntier());
            }
            FileWriter file = new FileWriter(nomFichier);
            int i=0;
            while (i<depeches.size()){
                file.write(dico.get(i).getChaine()+":"+dico.get(i).getEntier()+"\n");
                i++;
            }
            file.close();
            System.out.println("votre saisie a été écrite avec succès dans fichier-sortie.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        //Chargement des dépêches en mémoire
        System.out.println("chargement des dépêches");
        ArrayList<Depeche> depeches = lectureDepeches("./depeches.txt");

        generationLexique(depeches,"culture","lexique");

//        for (Depeche depeche : depeches) {
//            depeche.afficher();
//        }

//        Categorie culture = new Categorie("culture");
//        culture.initLexique("./culture.txt");
//
//        Categorie economie = new Categorie("economie");
//        economie.initLexique("./economie.txt");
//
//        Categorie environnementSciences = new Categorie("environnement-sciences");
//        environnementSciences.initLexique("./environnement-sciences.txt");
//
//        Categorie politique = new Categorie("politique");
//        politique.initLexique("./politique.txt");
//
//        Categorie sports = new Categorie("sports");
//        sports.initLexique("./sports.txt");
//        System.out.printf("Lexique de la catégorie %s\n", sports.getNom());
//        for (int i = 0; i < sports.getLexique().size(); i++) {
//            System.out.printf("%s (score : %d)\n",
//                    sports.getLexique().get(i).getChaine(),
//                    sports.getLexique().get(i).getEntier());
//        }

//        Scanner scanner = new Scanner(System.in);
//        System.out.print("\nEntrez un mot : ");
//        String mot = scanner.nextLine();
//        System.out.printf("Le poids associé à %s est %d\n\n",
//                mot,
//                UtilitairePaireChaineEntier.entierPourChaine(sports.getLexique(), mot));

//        ArrayList<Categorie> vCategories = new ArrayList<>(Arrays.asList(environnementSciences, culture, economie, politique, sports));
//        ArrayList<PaireChaineEntier> vScores = new ArrayList<>();
//        for (Categorie categorie : vCategories) {
//            vScores.add(new PaireChaineEntier(categorie.getNom(), categorie.score(depeches.get((int) (Math.random() * 500)))));
//        }
//        for (PaireChaineEntier score : vScores) {
//            System.out.printf("Score de la catégorie %s : %d\n", score.getChaine(), score.getEntier());
//        }
//        System.out.println("Prédiction de la catégorie de la dépêche : " + depeches.get((int) (Math.random() * 500)).getId());
//        System.out.printf("La catégorie prédite est %s\n", UtilitairePaireChaineEntier.chaineMax(vScores).toUpperCase());
//        System.out.printf("Le poids associé à %s est %d\n", mot, UtilitairePaireChaineEntier.entierPourChaine(sports.getLexique(), mot));

//        classementDepeches(depeches,vCategories,"output");
    }


}

