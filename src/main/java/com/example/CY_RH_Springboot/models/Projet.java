package com.example.CY_RH_Springboot.models;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "projet")
public class Projet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "Nom_projet", nullable = false, length = 150)
    private String nomProjet;

    @Column(name = "Etat_projet", nullable = false, length = 30)
    private String etatProjet = "EN_COURS"; // EN_COURS, TERMINE, ANNULE

    @Column(name = "Date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "Date_fin_prevue")
    private LocalDate dateFinPrevue;

    @Column(name = "Date_fin_reelle")
    private LocalDate dateFinReelle;

    @Column(name = "Chef_projet")
    private Integer chefProjet;

    @Column(name = "Id_departement")
    private Integer idDepartement;

    public Projet() {}

    // Getters et Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNomProjet() {
        return nomProjet;
    }

    public void setNomProjet(String nomProjet) {
        this.nomProjet = nomProjet;
    }

    public String getEtatProjet() {
        return etatProjet;
    }

    public void setEtatProjet(String etatProjet) {
        this.etatProjet = etatProjet;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDate dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDate getDateFinPrevue() {
        return dateFinPrevue;
    }

    public void setDateFinPrevue(LocalDate dateFinPrevue) {
        this.dateFinPrevue = dateFinPrevue;
    }

    public LocalDate getDateFinReelle() {
        return dateFinReelle;
    }

    public void setDateFinReelle(LocalDate dateFinReelle) {
        this.dateFinReelle = dateFinReelle;
    }

    public Integer getChefProjet() {
        return chefProjet;
    }

    public void setChefProjet(Integer chefProjet) {
        this.chefProjet = chefProjet;
    }

    public Integer getIdDepartement() {
        return idDepartement;
    }

    public void setIdDepartement(Integer idDepartement) {
        this.idDepartement = idDepartement;
    }
}