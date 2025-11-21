package com.example.CY_RH_Springboot;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordEncoderUtility {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // Générer les hash pour vos mots de passe
        String hashAdmin = encoder.encode("admin123");
        String hashPwd = encoder.encode("pwd");

        System.out.println("=== COPIEZ CES REQUÊTES SQL ===\n");
        System.out.println("-- Pour admin@cy-rh.local (mot de passe: admin123)");
        System.out.println("UPDATE Employer SET Password = '" + hashAdmin + "' WHERE Email = 'admin@cy-rh.local';");
        System.out.println();
        System.out.println("-- Pour tous les autres (mot de passe: pwd)");
        System.out.println("UPDATE Employer SET Password = '" + hashPwd + "' WHERE Email != 'admin@cy-rh.local';");
        System.out.println();

        // Vérification
        System.out.println("=== VÉRIFICATION ===");
        System.out.println("'admin123' correspond au hash: " + encoder.matches("admin123", hashAdmin));
        System.out.println("'pwd' correspond au hash: " + encoder.matches("pwd", hashPwd));
        System.out.println();
        System.out.println("Longueur hash admin: " + hashAdmin.length() + " caractères");
        System.out.println("Longueur hash pwd: " + hashPwd.length() + " caractères");
    }
}