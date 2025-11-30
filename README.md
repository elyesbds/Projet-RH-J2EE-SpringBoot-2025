# CY-RH-SpringBoot - Guide d'Installation Rapide

## Prérequis
- Java 21
- JDK 25
- MySQL Workbench
- Eclipse IDE
- Apache Tomcat 10
- Maven

---

## Étape 1 : Base de Données

1. Ouvrir MySQL Workbench
2. Configurer la connexion MySQL
   - **Option A :** Créer un utilisateur `root` avec le mot de passe `1234`
   - **Option B :** Modifier les informations de connexion dans `src/main/resources/application.properties` (lignes 4 et 5) avec vos identifiants MySQL
3. Exécuter le script `cy_rh_bdd.sql`
   - File → Open SQL Script → Sélectionner `cy_rh_bdd.sql`
   - Cliquer sur l'éclair pour exécuter
4. La base `CY_RH` est créée avec les données de test
