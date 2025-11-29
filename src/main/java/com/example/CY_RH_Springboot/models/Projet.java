package com.example.CY_RH_Springboot.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

@Entity
@Table(name = "projet")
public class Projet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "Le nom du projet est obligatoire")
    @Size(min = 3, max = 150, message = "Le nom doit contenir entre 3 et 150 caractères")
    @Column(name = "Nom_projet", nullable = false, length = 150)
    private String nomProjet;

    @NotBlank(message = "L'état du projet est obligatoire")
    @Column(name = "Etat_projet", nullable = false, length = 30)
    private String etatProjet = "EN_COURS";

    @NotNull(message = "La date de début est obligatoire")
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

    // Getters et Setters (inchangés)
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNomProjet() { return nomProjet; }
    public void setNomProjet(String nomProjet) { this.nomProjet = nomProjet; }
    public String getEtatProjet() { return etatProjet; }
    public void setEtatProjet(String etatProjet) { this.etatProjet = etatProjet; }
    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }
    public LocalDate getDateFinPrevue() { return dateFinPrevue; }
    public void setDateFinPrevue(LocalDate dateFinPrevue) { this.dateFinPrevue = dateFinPrevue; }
    public LocalDate getDateFinReelle() { return dateFinReelle; }
    public void setDateFinReelle(LocalDate dateFinReelle) { this.dateFinReelle = dateFinReelle; }
    public Integer getChefProjet() { return chefProjet; }
    public void setChefProjet(Integer chefProjet) { this.chefProjet = chefProjet; }
    public Integer getIdDepartement() { return idDepartement; }
    public void setIdDepartement(Integer idDepartement) { this.idDepartement = idDepartement; }
}