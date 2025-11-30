# CY-RH-SpringBoot - Guide d'Installation Rapide (Version Spring Boot)

## Prérequis
- JDK 25
- MySQL Workbench
- Eclipse IDE
- Maven

---

## Étape 1 : Base de Données

1. Ouvrir MySQL Workbench
2. Configurer la connexion MySQL
   - **Option A :** Créer un utilisateur `root` avec le mot de passe `1234`
   - **Option B :** Modifier les informations de connexion dans `src/main/resources/application.properties` (lignes 4 et 5) avec vos identifiants MySQL
3. Exécuter le script `cy_rh_bdd.sql` (si pas déjà fait)
   - File → Open SQL Script → Sélectionner `cy_rh_bdd.sql`
   - Cliquer sur l'éclair pour exécuter
4. La base `CY_RH` est créée avec les données de test

---

## Étape 2 : Application

1. Télécharger le dossier `CY-RH-Springboot`
2. Importer dans Eclipse
   - File → Open Projects from File System
   - Directory → Sélectionner le dossier `CY-RH-Springboot`
   - Finish
3. Mettre à jour Maven
   - Clic droit sur le projet → Maven → Update Project → OK
4. Mettre à jour les mots de passe de connexion
   - Clic droit sur la classe → `Projet-RH-J2EE-SpringBoot-2025-main\src\main\java\com\example\CY_RH_Springboot\PasswordEncoderUtility.java` → Run As → Java Application
   -  Cette classe génère automatiquement les mots de passe encodés, et fournit donc deux requêtes SQL. Copiez ces deux requêtes.
   - Dans MySQL Workbench, compiler `USE cy_rh;` `SET SQL_SAFE_UPDATES = 0;` ainsi que les deux requêts généré précédemment.
5. Compiler l'application
   - Clic droit sur le projet → Run As → Maven install
   - Le projet doit afficher : BUILD SUCCESS
6. Lancer l'application
   - Clic droit sur la classe → `Projet-RH-J2EE-SpringBoot-2025-main\src\main\java\com\example\CY_RH_Springboot\CyRhSpringbootApplication.java` → Run As → Java Application

---

## Étape 3 : Connexion

1. Ouvrir son navigateur web et taper l'url : http://localhost:8080

**Compte Administrateur :**
- Email : `admin@cy-rh.local`
- Mot de passe : `admin123`

**Comptes Employés :**
- Email : `[prenom].[nom]@cy-rh.local` (ex: `maria.lopez@cy-rh.local`)
- Mot de passe : `pwd123`

---

## C'est prêt !

Bienvenue sur CY-RH-SpringBoot, vous êtes connecté.

L'application de gestion des ressources humaines est maintenant opérationnelle.
