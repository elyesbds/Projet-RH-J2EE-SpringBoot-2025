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

---

## Étape 2 : Application

1. Télécharger le dossier `Projet-RH-J2EE-SpringBoot-2025`
2. Importer dans Eclipse
   - File → Open Projects from File System
   - Directory → Sélectionner le dossier `Projet-RH-J2EE-SpringBoot-2025`
   - Finish
3. Mettre à jour Maven
   - Clic droit sur le projet → Maven → Update Project → OK
4. Mettre à jour la base de donnée
   - Clic droit sur le projet → Run As → Java Application
   - Sélectionner PasswordEncoderUtility → Finish
   - Executer les commandes affichés dans MySQL
5. Lancer l'application
   - Clic droit sur le projet → Run As → Java Application
   - Sélectionner CyRhSpringbootApplication → Finish

---

## Étape 3 : Connexion

**Compte Administrateur :**
- Email : `admin@cy-rh.local`
- Mot de passe : `admin123`

**Comptes Employés :**
- Email : `[prenom].[nom]@cy-rh.local` (ex: `maria.lopez@cy-rh.local`)
- Mot de passe : `password`

---

