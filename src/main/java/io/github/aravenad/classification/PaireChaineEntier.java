public class PaireChaineEntier {
    // Attributs
    private String chaine;
    private int entier;

    // Constructeur
    public PaireChaineEntier(String chaine, int entier) {
        this.chaine = chaine;
        this.entier = entier;
    }

    // Accesseurs
    public String getChaine() {
        return chaine;
    }
    public int getEntier() {
        return entier;
    }

    // Mutateurs
    public void setChaine(String chaine) {
        this.chaine = chaine;
    }
    public void setEntier(int entier) {
        this.entier = entier;
    }
}