# Sujet (reconstitué)

> Les documents d'origine de l'IUT ne sont plus disponibles. Cette page reconstitue le sujet à partir du code,
> des spécifications recopiées en commentaire (notation `{précondition} → {postcondition}`) et de l'historique git.
> Elle peut donc différer du sujet officiel sur certains détails.

## Contexte

SAÉ S1.02 *Comparaison d'approches algorithmiques*, BUT Informatique 1ʳᵉ année, réalisée du 15 au 19 janvier 2024
par ARAVENA BRAVO Damien et RIONDET Flavien.

Le but est de **classer automatiquement des dépêches de presse** dans l'une des cinq catégories suivantes :
`ENVIRONNEMENT-SCIENCES`, `CULTURE`, `ECONOMIE`, `POLITIQUE`, `SPORTS`.

Chaque catégorie possède un **lexique** : une liste de mots pondérés. Le score d'une dépêche pour une catégorie est
la somme des poids des mots de la dépêche présents dans le lexique (un mot répété compte autant de fois qu'il
apparaît). La dépêche est attribuée à la catégorie qui obtient le plus grand score.

## Données fournies

| Fichier | Contenu |
|---|---|
| `depeches.txt` | 500 dépêches (100 par catégorie), servant de corpus d'apprentissage |
| `test.txt` | 500 autres dépêches (100 par catégorie), servant de corpus de test |
| `stopwords-fr.txt` | Mots vides du français (articles, pronoms…), à ignorer |

Format d'une dépêche :

```text
.N 001
.D 140208
.C ENVIRONNEMENT-SCIENCES
.T Des cellules souches ont été produites à partir de cellules adultes
sans provoquer de tumeur.
```

`.N` est l'identifiant, `.D` la date (JJMMAA), `.C` la catégorie réelle, `.T` le début du texte, qui se poursuit
sur les lignes suivantes jusqu'à une ligne vide.

Un lexique est un fichier texte avec une entrée `mot:poids` par ligne, le poids valant 1, 2 ou 3.

Le fichier réponse contient une ligne `id:CATEGORIE` par dépêche, puis le pourcentage de dépêches bien classées
pour chaque catégorie et leur moyenne :

```text
001:ENVIRONNEMENT-SCIENCES
002:ECONOMIE
...
ENVIRONNEMENT-SCIENCES : 63%
CULTURE : 74%
...
MOYENNE : 74%
```

## Partie 1 : classement avec des lexiques manuels

Écrire à la main un lexique par catégorie, puis implémenter :

- la classe `PaireChaineEntier` (une chaîne associée à un entier) ;
- `Categorie.initLexique(nomFichier)` : charge le lexique d'une catégorie ;
- `Categorie.score(d)` : score de la dépêche `d` pour la catégorie ;
- dans `UtilitairePaireChaineEntier` :
  - `indicePourChaine(liste, chaine)` : indice de `chaine` dans la liste, ou -1 ;
  - `entierPourChaine(liste, chaine)` : entier associé à `chaine`, ou 0 ;
  - `chaineMax(liste)` : chaîne associée au plus grand entier ;
  - `moyenne(liste)` : moyenne des entiers ;
- `Classification.classementDepeches(depeches, categories, nomFichier)` : classe toutes les dépêches et écrit le
  fichier réponse.

## Partie 2 : génération automatique des lexiques

Construire les lexiques à partir du corpus d'apprentissage :

- `initDico(depeches, categorie)` : liste sans doublon des mots apparaissant dans au moins une dépêche de la
  catégorie, chacun avec un score initial de 0 ;
- `calculScores(depeches, categorie, dictionnaire)` : pour chaque occurrence d'un mot du dictionnaire dans une
  dépêche, incrémente son score si la dépêche est de la catégorie, le décrémente sinon ;
- `poidsPourScore(score)` : convertit un score en poids 1, 2 ou 3 (seuils au choix) ;
- `generationLexique(depeches, categorie, nomFichier)` : enchaîne les trois méthodes précédentes et écrit le
  fichier lexique.

Les lexiques générés sont ensuite utilisés pour classer le corpus de test, et comparés aux lexiques manuels.

## Partie 3 : efficacité (comparaison d'approches)

Analyser la complexité des algorithmes, mesurer le temps d'exécution (`System.currentTimeMillis()`) et l'améliorer :
garder les listes triées pour remplacer la recherche séquentielle par une **recherche dichotomique**, choisir un
algorithme de tri adapté, utiliser des structures adaptées (`HashSet`, `HashMap`), etc.

Deux classes d'exemple étaient fournies (`ExempleEcritureFichier`, `ExempleTempsExecution`). Elles ont été retirées
du projet mais restent consultables dans l'historique git.
