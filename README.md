# Paper CALC

🇬🇧 English description: [DESCRIPTION.md](DESCRIPTION.md)

Calculatrice Android optimisée pour le **Mudita Kompakt** (écran e-ink 4,3", 800×480, Android 12 sans services Google).

Version **1.0**

## Fonctionnalités

- Affichage noir et blanc à fort contraste, sans animations ni effets de clic (adapté à l'e-ink).
- Pavé numérique réduit de 15 % pour agrandir l'afficheur.
- Respect des priorités : parenthèses > signe − > × ÷ > + −, calcul en nombres décimaux exacts (`BigDecimal`).
- Résultat calculé **au fur et à mesure de la frappe**.
- Touche **=** : enregistre l'opération dans l'historique (puis un opérateur continue le calcul depuis le résultat, un chiffre commence un nouveau calcul).
- Touche **( )** intelligente : ouvre ou ferme la parenthèse selon le contexte (`2(` devient `2×(`).
- Touche **+/−** : change le signe du nombre ou de la parenthèse en cours.
- Retour haptique à chaque touche (désactivable).
- Icône **copier** : copie « opération = résultat » dans le presse-papiers.
- Menu **⋮ (options)** :
  - historique des 20 dernières opérations (appui court = réutiliser, appui long = copier) ;
  - séparateur décimal : `,` ou `.` ;
  - séparateur des milliers : `.`, `,` ou espace ;
  - retour haptique ;
  - langue : système, français, anglais, polonais, allemand, espagnol, portugais, italien, néerlandais, tchèque, suédois ;
  - à propos : version et lien vers la dernière release GitHub.
- Appui long sur ⌫ : tout effacer.

## Installation

Chaque push construit l'APK via GitHub Actions (onglet *Actions*, artefact `PaperCALC-apk`).
Pour publier une release : créer un tag `v1.0` et le pousser ; l'APK est alors joint à la
[dernière release](https://github.com/mdeberly/paper_calc/releases/latest).

Installation sur le Kompakt : copier l'APK sur le téléphone (ou via `adb install PaperCALC.apk`).

## Compilation locale

```
./gradlew testDebugUnitTest assembleRelease
```

Nécessite le SDK Android (compileSdk 34) et un JDK 17+.

---

Réalisé par Maximilien DEBERLY, Pharmacien, Chauny, FRANCE, avec l'aide de Claude AI pour le code.
