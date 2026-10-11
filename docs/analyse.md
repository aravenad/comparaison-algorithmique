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

À paramètres égaux (ceux d'origine), les lexiques générés et les scores obtenus sont **identiques** avant et après la
réécriture : seuls les temps changent. Les paramètres ont ensuite été réglés séparément (voir plus bas).

## Corrections apportées

- Ordre d'exécution corrigé : génération des lexiques en parallèle, attente de la fin, puis classement.
- Apprentissage sur `depeches.txt`, évaluation sur `test.txt` (chemins modifiables en argument).
- Pourcentages calculés sur le nombre réel de dépêches de chaque catégorie.
- Lexiques passés en minuscules au chargement ; `chaineMax` correct quels que soient les scores.
- UTF-8 explicite partout, `try-with-resources`, erreurs remontées au lieu d'être ignorées.
- `indicePourChaine` ne trie plus la liste : elle exige une liste triée (`trierParChaine`).
- Code rangé dans un package, projet Maven, tests unitaires JUnit 5.
- Réglage de la génération des lexiques (voir ci-dessous).

## Réglage des lexiques générés

Trois paramètres de la génération ont été réglés :

- la longueur minimale des mots (`LONGUEUR_MIN_MOT`) ;
- les seuils de `poidsPourScore` ;
- le filtrage éventuel des mots au score négatif, c'est-à-dire plus fréquents hors de la catégorie que dedans.

**Méthode.** Les paramètres ne sont choisis que sur le corpus d'apprentissage, par validation croisée en 5 blocs
stratifiés : on apprend sur 400 dépêches et on évalue sur les 100 restantes, cinq fois. 10 140 combinaisons ont été
essayées. Les 300 meilleures ont été réévaluées sur 10 tirages aléatoires des blocs, car une seule validation croisée
sur 500 dépêches a un écart-type d'environ 2 points. Le corpus de test n'a servi qu'à vérifier le résultat final.

Les mesures se reproduisent avec
[`ValidationCroisee`](../src/test/java/io/github/aravenad/classification/experiences/ValidationCroisee.java) :

```bash
java -cp target/classes:target/test-classes io.github.aravenad.classification.experiences.ValidationCroisee
```

L'argument `grille` relance la recherche complète.

| Variante | Validation croisée (10 × 5 blocs) | Corpus de test |
|---|---:|---:|
| Version initiale : mots ≥ 4 lettres, poids 1 si score ≤ -4, 3 si score ≥ 4, 2 sinon | 73,9 % | 73,6 % |
| Piste 1 : mots au score < 0 retirés du lexique | 74,0 % | 73,4 % |
| Piste 1 : mots au score < 1 retirés du lexique | 74,6 % | 73,0 % |
| Piste 2 : poids 1 si score ≤ 1 | 75,8 % | 75,0 % |
| Piste 3 : mots ≥ 3 lettres | 75,0 % | 74,8 % |
| **Pistes 2 + 3 (retenue)** : mots ≥ 3 lettres, poids 1 si score ≤ 1, 3 si score ≥ 4, 2 sinon | **77,1 %** | **76,4 %** |
| Meilleure combinaison trouvée : mots ≥ 3 lettres, poids 1 si score ≤ 0, 3 si score ≥ 2, 2 sinon | 77,2 % | 75,4 % |
| Variante retenue, sans retirer les mots vides | 76,6 % | 73,2 % |

La variante retenue est à égalité statistique avec la meilleure trouvée. Elle a été préférée parce qu'elle ne change
qu'un seul seuil de la version initiale. Le gain est réel mais modeste : environ 3 points.

Ce que montrent les mesures :

- **Retirer les mots négatifs n'aide pas.** Un mot courant dans toutes les catégories figure dans tous les lexiques
  avec le poids 1 : il augmente tous les scores d'autant et ne change donc pas le classement. Le retirer de certains
  lexiques seulement fausse la comparaison.
- **Ce qui compte, c'est l'écart entre les poids.** Réserver le poids 1 aux mots qui ne sont pas nettement plus
  fréquents dans la catégorie qu'ailleurs (score ≤ 1, au lieu de ≤ -4) donne plus d'importance aux mots vraiment
  caractéristiques.
- **Garder les mots de 3 lettres** apporte les sigles (PIB, PSG, OGM, UMP…). Les mots de 1 ou 2 lettres n'apportent
  rien de plus.
- **Les mots vides restent utiles à retirer** : sans ce filtre, on perd 3 points sur le corpus de test.

## Résultats sur le corpus de test

| Catégorie | Lexiques manuels, avant | Lexiques manuels, après | Lexiques générés, avant réglage | Lexiques générés, après réglage |
|---|---:|---:|---:|---:|
| Environnement-sciences | 51 % | 74 % | 63 % | 69 % |
| Culture | 42 % | 45 % | 74 % | 77 % |
| Économie | 47 % | 45 % | 74 % | 74 % |
| Politique | 62 % | 73 % | 71 % | 72 % |
| Sports | 53 % | 53 % | 86 % | 90 % |
| **Moyenne** | **51 %** | **58 %** | **74 %** | **76 %** |

À propos des lexiques manuels : 113 dépêches de test sur 500 ne contiennent **aucun** de leurs mots. Elles ont
un score nul partout et sont attribuées à la première catégorie, environnement-sciences. 16 d'entre elles en font
réellement partie, ce qui gonfle ce score de 16 points. Avec les lexiques générés, aucune dépêche n'est dans ce cas.

## Pistes d'amélioration

Le modèle « somme de poids 1, 2 ou 3 » plafonne autour de 77 %. Pour aller plus loin, il faudrait changer de
modèle :

- pondérer chaque mot par sa fréquence relative dans la catégorie (approche de type TF-IDF ou bayésien naïf) au lieu
  de trois poids entiers ;
- normaliser le score par la taille du lexique, si une catégorie au lexique plus fourni s'avère avantagée (à
  vérifier) ;
- gérer explicitement les égalités et les scores nuls plutôt que de choisir la première catégorie.
