# Analyse de l'existant

État du projet au dernier commit de la SAÉ (`7bf4a7e`, 19 janvier 2024), puis ce qui a été corrigé lors de la
reprise.

## Ce qui existait

- 7 classes Java dans `src/`, sans package, dont les deux exemples fournis par l'IUT ;
- les données et les fichiers générés (lexiques `*Auto.txt`, `output.txt`, `outputAuto.txt`) en vrac à la racine,
  tous versionnés ;
- un projet IntelliJ (`.idea/`, `.iml`) versionné et configuré pour Java 8 ;
- ni README, ni tests, ni outil de build.

Le code compile et s'exécute. Les parties 1 et 2 du sujet sont implémentées, ainsi que plusieurs optimisations
(recherche dichotomique, `HashSet` pour les mots vides, `HashMap` dans `calculScores`, threads).

## Problèmes relevés

### Résultats faussés

| # | Problème | Conséquence |
|---|---|---|
| 1 | Dans `main`, les threads de génération des lexiques sont créés, mais ne sont lancés qu'**après** la lecture des lexiques générés. Le thread de classement tourne en même temps qu'eux. | Le classement utilise les lexiques de l'exécution **précédente**. Au premier lancement, les fichiers n'existent pas, les lexiques sont vides et le classement est faux. |
| 2 | Le dernier commit lit `test.txt` pour l'apprentissage **et** pour l'évaluation. | Les lexiques sont évalués sur les dépêches qui ont servi à les construire : 98 % de réussite, un chiffre trompeur (surapprentissage). Sur des dépêches jamais vues, on retombe à 74 %. |
| 3 | `classementDepeches` écrit le nombre brut de dépêches bien classées suivi de `%`. | Le chiffre n'est un pourcentage que parce qu'il y a exactement 100 dépêches par catégorie. |
| 4 | Les mots des dépêches sont mis en minuscules, mais pas ceux des lexiques. | Les entrées « France », « PIB », « OGM », « NASA », « PSG »… des lexiques manuels ne sont jamais trouvées. |
| 5 | `chaineMax` démarre avec `max = 0`. | Si aucun score n'est strictement positif, la catégorie prédite est la chaîne vide, donc forcément fausse. |

### Robustesse

- Fichiers lus et écrits avec l'encodage par défaut du système : sous Windows avec Java < 18, les accents sont cassés
  et « musée » ne correspond plus à rien.
- Erreurs d'entrée/sortie avalées par `printStackTrace()`, fichiers non fermés en cas d'exception.
- `lectureDepeches` suppose exactement quatre lignes d'en-tête et une seule ligne vide entre deux dépêches.
- `initLexique` plante sur une ligne sans `:` et supprime le signe des poids négatifs (`replaceAll("[^0-9]", "")`).
- `indicePourChaine` **trie la liste qu'on lui passe** à chaque appel (effet de bord), et compare tantôt avec
  `equals`, tantôt avec `compareToIgnoreCase`.

### Efficacité

Notations : *m* le nombre de mots d'une dépêche, *L* la taille d'un lexique, *V* le nombre de mots distincts d'une
catégorie.

| Opération | Avant | Après |
|---|---|---|
| `Categorie.score` | O(*m* × *L*) : double boucle sur les mots et le lexique | O(*m*) : index `HashMap` construit au chargement |
| Doublons dans `initDico` | O(*V*) par mot : tri à bulles de toute la liste à chaque appel de `indicePourChaine`, puis dichotomie | O(log *V*) par mot : `TreeSet`, qui donne en plus un dictionnaire trié |
| Tri | Tri à bulles amélioré, O(*V*²) dans le pire cas | `List.sort` (tri fusion), O(*V* log *V*) |

Mesures sur `depeches.txt` (500 dépêches, 5 lexiques générés de 5 617 mots au total), exécution séquentielle,
JVM chauffée, OpenJDK 21 :

| Étape | Avant | Après |
|---|---|---|
| Génération des 5 lexiques | ≈ 210 ms | ≈ 15 ms |
| Calcul des 2 500 scores (500 dépêches × 5 catégories) | ≈ 560 ms | ≈ 5 ms |

Les lexiques générés et les scores obtenus sont **identiques** avant et après : seuls les temps changent.

## Corrections apportées

- Ordre d'exécution corrigé : génération des lexiques en parallèle, attente de la fin, puis classement.
- Apprentissage sur `depeches.txt`, évaluation sur `test.txt` (chemins modifiables en argument).
- Pourcentages calculés sur le nombre réel de dépêches de chaque catégorie.
- Lexiques passés en minuscules au chargement ; `chaineMax` correct quels que soient les scores.
- UTF-8 explicite partout, `try-with-resources`, erreurs remontées au lieu d'être ignorées.
- `indicePourChaine` ne trie plus la liste : elle exige une liste triée (`trierParChaine`).
- Code rangé dans un package, projet Maven, 17 tests unitaires JUnit 5.

## Résultats sur le corpus de test

| Catégorie | Lexiques manuels, avant | Lexiques manuels, après | Lexiques générés |
|---|---:|---:|---:|
| Environnement-sciences | 51 % | 74 % | 63 % |
| Culture | 42 % | 45 % | 74 % |
| Économie | 47 % | 45 % | 74 % |
| Politique | 62 % | 73 % | 71 % |
| Sports | 53 % | 53 % | 86 % |
| **Moyenne** | **51 %** | **58 %** | **74 %** |

À propos des lexiques manuels : 113 dépêches de test sur 500 ne contiennent **aucun** de leurs mots. Elles ont
un score nul partout et sont attribuées à la première catégorie, environnement-sciences. 16 d'entre elles en font
réellement partie, ce qui gonfle ce score de 16 points. Avec les lexiques générés, aucune dépêche n'est dans ce cas.

## Pistes d'amélioration

- `poidsPourScore` donne le poids 1 aux mots au score très négatif : ces mots, typiques des **autres** catégories,
  augmentent donc quand même le score. Leur donner un poids nul, ou ne pas les écrire dans le lexique, devrait
  améliorer la précision.
- Ajuster les seuils de `poidsPourScore`, ou pondérer par la fréquence relative du mot (approche de type TF-IDF ou
  bayésien naïf).
- Les mots de 3 lettres ou moins sont exclus des lexiques générés, ce qui écarte des sigles très discriminants
  (PIB, PSG, OGM, UMP…).
- Gérer explicitement les égalités et les scores nuls plutôt que de choisir la première catégorie.
