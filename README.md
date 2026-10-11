# SAÉ 1.02 – Classification automatique de dépêches

Projet de première année de BUT Informatique (IUT2 Grenoble, janvier 2024) : classer automatiquement des dépêches de
presse dans cinq catégories (environnement-sciences, culture, économie, politique, sports). Le sujet, *Comparaison
d'approches algorithmiques*, demandait d'écrire des lexiques de mots pondérés à la main, puis de les générer
automatiquement à partir de dépêches déjà classées, et enfin de rendre les algorithmes plus efficaces.

La version rendue en janvier 2024 est marquée par le tag
[`rendu-code`](https://github.com/aravenad/comparaison-algorithmique/tree/rendu-code). J'ai repris le projet seul en
octobre 2026 pour corriger les bugs, régler l'algorithme et le comparer à d'autres méthodes d'apprentissage.

| Approche | Dépêches de test bien classées |
|---|---:|
| Lexiques écrits à la main | 58 % |
| Lexiques générés automatiquement | 76 % |
| Bayésien naïf sur les n-grammes de caractères | **78 %** |

## Ce que fait le programme

Il apprend sur les 500 dépêches de `data/corpus/depeches.txt` et s'évalue sur 500 autres, jamais vues, dans
`data/corpus/test.txt` :

- **Lexiques manuels** : un lexique par catégorie (`data/lexiques/`). Le score d'une dépêche est la somme des poids
  de ses mots présents dans le lexique, et la catégorie retenue est celle de plus grand score.
- **Lexiques générés** : même principe, mais les lexiques sont construits automatiquement. Un mot gagne un point
  chaque fois qu'il apparaît dans une dépêche de la catégorie et en perd un ailleurs ; ce score est ensuite
  converti en poids 1, 2 ou 3.
- **Bayésien naïf** : chaque mot reçoit une probabilité apprise par catégorie. Il est appliqué aux mots, puis aux
  morceaux de 3 à 6 lettres des mots (n-grammes de caractères), pour que « footballeurs » ressemble à « football ».

Pour chaque approche, il affiche le pourcentage de réussite par catégorie, puis la matrice de confusion de la
meilleure, et il écrit le classement détaillé dans `resultats/`.

## Lancer le programme

Il faut Java 17 ou plus récent. Maven est fourni par le wrapper `mvnw`, rien d'autre à installer. Les commandes se
lancent depuis la racine du projet, car les chemins des données sont relatifs :

```bash
./mvnw package
```

```bash
java -jar target/comparaison-algorithmique-1.0.0.jar
```

On peut passer en arguments un autre corpus d'apprentissage, un autre corpus de test et un autre dossier de
résultats, dans cet ordre. Sans Maven ni accès réseau, `javac` suffit :

```bash
javac -encoding UTF-8 -d out $(find src/main/java -name '*.java')
```

```bash
java -cp out io.github.aravenad.classification.Classification
```

## Les autres commandes

Les expériences se lancent après `./mvnw test-compile`, avec
`java -cp target/classes:target/test-classes io.github.aravenad.classification.experiences.<Classe>` :

| Commande | Rôle |
|---|---|
| `./mvnw test` | Tests unitaires (JUnit 5) |
| `ValidationCroisee` | Compare les réglages des lexiques générés (longueur minimale des mots, seuils des poids) |
| `ValidationCroisee grille` | Refait la recherche complète des réglages (10 140 combinaisons, environ 30 s) |
| `ComparaisonApproches` | Compare les lexiques, le bayésien naïf, les centroïdes TF-IDF, les k plus proches voisins et la régression logistique |

## Arborescence

```
├── pom.xml
├── data/
│   ├── corpus/
│   │   ├── depeches.txt      dépêches d'apprentissage (500, 100 par catégorie)
│   │   └── test.txt          dépêches de test (500)
│   ├── lexiques/             nos lexiques écrits à la main
│   └── stopwords-fr.txt      mots vides ignorés (le, de, avec…)
├── src/
│   ├── main/java/io/github/aravenad/classification/
│   │   ├── Classification.java          lecture des fichiers, génération des lexiques, programme principal
│   │   ├── Categorie.java               lexique et score d'une catégorie
│   │   ├── Depeche.java                 une dépêche et son découpage en mots
│   │   ├── PaireChaineEntier.java
│   │   ├── UtilitairePaireChaineEntier.java   recherche dichotomique, maximum, moyenne
│   │   ├── Classifieur.java             l'interface commune aux approches
│   │   ├── ClassifieurLexiques.java     l'approche du sujet
│   │   ├── BayesNaif.java               le bayésien naïf
│   │   ├── Caracteristiques.java        mots et n-grammes de caractères
│   │   └── Evaluation.java              pourcentages, matrice de confusion, validation croisée
│   └── test/java/…                      tests unitaires et expériences (experiences/)
└── docs/
    ├── sujet.md              le sujet, reconstitué
    └── analyse.md            bugs corrigés, mesures et comparaison des approches
```

## Nos choix

En 2024, on a surtout travaillé l'efficacité, comme le demandait le sujet : recherche dichotomique, `HashSet` pour les
mots vides, `HashMap` pour le calcul des scores et un thread par catégorie.

En reprenant le projet, j'ai d'abord corrigé des bugs qui faussaient les résultats, le plus gros étant que le
programme apprenait et s'évaluait sur les mêmes dépêches (98 % affichés, 74 % en réalité). Toutes les mesures suivent
maintenant le même protocole : les réglages sont choisis par **validation croisée** sur les seules dépêches
d'apprentissage, et le corpus de test ne sert qu'à vérifier le résultat final. Ça évite de choisir les paramètres qui
tombent bien sur le test.

Toutes les méthodes essayées finissent entre 75 et 79 %. Les dépêches sont courtes (40 mots en moyenne), il n'y a que
500 exemples d'apprentissage et certaines catégories se recouvrent vraiment. Une dépêche sur les OGM votés à
l'Assemblée, par exemple, est étiquetée environnement-sciences mais parle autant de politique. Le détail est dans
l'[analyse](docs/analyse.md#pourquoi-on-plafonne-vers-78-).

### Écarts avec le sujet et la version rendue

| Sujet ou version de 2024 | Réalisation | Raison |
|---|---|---|
| `initDico(depeches, categorie)` | `initDico(depeches, categorie, motsExclus)` | Les mots vides sont lus une seule fois, et non à chaque catégorie |
| `classementDepeches` ne renvoie rien | Renvoie les pourcentages par catégorie | Le programme les affiche sans relire le fichier |
| `indicePourChaine` trie la liste à chaque appel | Exige une liste déjà triée (`trierParChaine`) | Le tri à chaque appel rendait `initDico` quadratique |
| Mots de plus de 3 lettres dans les lexiques générés | Mots de 3 lettres ou plus | Garde les sigles (PIB, PSG, OGM), choisi par validation croisée |
| Absent | `Classifieur`, `BayesNaif`, `Evaluation` | Comparer les lexiques à d'autres approches |

Les deux classes d'exemple fournies par l'IUT (`ExempleEcritureFichier`, `ExempleTempsExecution`) sont retirées ;
elles restent dans le tag `rendu-code`.

## Équipe

ARAVENA BRAVO Damien, RIONDET Flavien.

## Crédits

- Sujet : SAÉ S1.02 *Comparaison d'approches algorithmiques* du département Informatique de l'IUT2 Grenoble
- Dépêches et liste de mots vides : fournies avec le sujet
