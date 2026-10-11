# Classification automatique de dépêches

[![CI](https://github.com/aravenad/comparaison-algorithmique/actions/workflows/ci.yml/badge.svg)](https://github.com/aravenad/comparaison-algorithmique/actions/workflows/ci.yml)

Projet de la SAÉ S1.02 *Comparaison d'approches algorithmiques* (BUT Informatique, IUT, janvier 2024).

Le programme classe des dépêches de presse dans cinq catégories (environnement-sciences, culture, économie,
politique, sports) à l'aide de **lexiques pondérés**. Le score d'une dépêche pour une catégorie est la somme des
poids des mots de son lexique qu'elle contient, et la catégorie retenue est celle de plus grand score. Les lexiques
sont d'abord écrits à la main, puis **générés automatiquement** à partir d'un corpus d'apprentissage.

| Lexiques | Réussite moyenne sur le corpus de test |
|---|---:|
| Écrits à la main | 58 % |
| Générés automatiquement | 76 % |

## Prérequis

- Java 17 ou plus récent

Maven n'a pas besoin d'être installé : le script `./mvnw` (`mvnw.cmd` sous Windows) télécharge la bonne version
au premier lancement.

## Utilisation

Toutes les commandes se lancent depuis la racine du projet, car les chemins des données sont relatifs à celle-ci.

```bash
./mvnw package
```

```bash
java -jar target/comparaison-algorithmique-1.0.0.jar
```

Le programme :

1. classe les dépêches de `data/corpus/test.txt` avec les lexiques manuels de `data/lexiques/` ;
2. génère un lexique par catégorie à partir de `data/corpus/depeches.txt` dans `resultats/lexiques-auto/` ;
3. classe à nouveau le corpus de test avec ces lexiques ;
4. affiche les pourcentages de réussite et les temps d'exécution.

Les classements détaillés sont écrits dans `resultats/classement-manuel.txt` et `resultats/classement-auto.txt`.
Ce dossier est régénéré à chaque exécution et n'est pas versionné.

Les chemins sont modifiables en argument, dans cet ordre : corpus d'apprentissage, corpus de test, dossier des
résultats.

```bash
java -jar target/comparaison-algorithmique-1.0.0.jar data/corpus/depeches.txt data/corpus/test.txt resultats
```

### Sans Maven ni accès réseau

```bash
javac -encoding UTF-8 -d out $(find src/main/java -name '*.java')
```

```bash
java -cp out io.github.aravenad.classification.Classification
```

## Tests

```bash
./mvnw test
```

L'intégration continue (GitHub Actions) lance le build, les tests et le classement à chaque push sur `master` et
sur chaque pull request, avec Java 17 et 21.

## Réglage des paramètres

Les paramètres de génération des lexiques ont été choisis par validation croisée sur le corpus d'apprentissage
(voir [docs/analyse.md](docs/analyse.md)). Pour relancer les mesures :

```bash
./mvnw test-compile
```

```bash
java -cp target/classes:target/test-classes io.github.aravenad.classification.experiences.ValidationCroisee
```

## Arborescence

```text
├── data/
│   ├── corpus/
│   │   ├── depeches.txt     corpus d'apprentissage (500 dépêches)
│   │   └── test.txt         corpus de test (500 dépêches)
│   ├── lexiques/            lexiques écrits à la main, un par catégorie
│   └── stopwords-fr.txt     mots vides ignorés lors de la génération
├── docs/
│   ├── sujet.md             sujet reconstitué
│   └── analyse.md           analyse de l'existant, corrections, mesures et pistes
├── src/
│   ├── main/java/io/github/aravenad/classification/
│   │   ├── Classification.java               lecture, génération des lexiques, classement, main
│   │   ├── Categorie.java                    lexique et score d'une catégorie
│   │   ├── Depeche.java                      dépêche et découpage en mots
│   │   ├── PaireChaineEntier.java
│   │   └── UtilitairePaireChaineEntier.java  recherche dichotomique, max, moyenne
│   └── test/java/…                           tests JUnit 5 et script de validation croisée
├── .github/workflows/ci.yml                  intégration continue
├── mvnw, mvnw.cmd                            Maven Wrapper
└── pom.xml
```

## Formats de fichiers

Dépêche (`.N` identifiant, `.D` date, `.C` catégorie réelle, `.T` texte sur une ou plusieurs lignes) :

```text
.N 001
.D 140208
.C ENVIRONNEMENT-SCIENCES
.T Des cellules souches ont été produites à partir de cellules adultes
sans provoquer de tumeur.
```

Lexique : une entrée `mot:poids` par ligne, avec un poids de 1, 2 ou 3.

Classement : une ligne `id:CATEGORIE` par dépêche, puis le pourcentage de réussite par catégorie et la moyenne.

## Documentation

- [Sujet reconstitué](docs/sujet.md) : les documents de l'IUT n'étant plus disponibles, le sujet a été reconstitué
  à partir du code et de l'historique.
- [Analyse de l'existant](docs/analyse.md) : bugs trouvés lors de la reprise, complexités et temps avant et après,
  réglage des lexiques générés par validation croisée, pistes d'amélioration.

## Auteurs

Damien Aravena et riondetf, d'après l'historique git.
