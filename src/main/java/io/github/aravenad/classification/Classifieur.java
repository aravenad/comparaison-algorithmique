package io.github.aravenad.classification;

import java.util.List;

/** Une approche de classification : elle apprend sur des dépêches étiquetées, puis prédit la catégorie d'une dépêche. */
public interface Classifieur {

    String nom();

    void apprendre(List<Depeche> depeches);

    /** Résultat : le nom de la catégorie prédite, en minuscules (p. ex. « sports »). */
    String categoriePredite(Depeche depeche);
}
