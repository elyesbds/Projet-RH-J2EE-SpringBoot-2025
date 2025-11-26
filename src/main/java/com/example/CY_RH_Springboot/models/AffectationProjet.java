package com.example.CY_RH_Springboot.models;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "affectation_projet")
public class AffectationProjet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "Id_employer", nullable = false)
    private Long idEmployer;

    @Column(name = "Id_projet", nullable = false)
    private Integer idProjet;

    @Column(name = "Date_affectation", nullable = false)
    private LocalDate dateAffectation;

    @Column(name = "Date_fin_affectation")
    private LocalDate dateFinAffectation;

    public AffectationProjet() {}

    // Getters et Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Long getIdEmployer() {
        return idEmployer;
    }

    public void setIdEmployer(Long idEmployer) {
        this.idEmployer = idEmployer;
    }

    public Integer getIdProjet() {
        return idProjet;
    }

    public void setIdProjet(Integer idProjet) {
        this.idProjet = idProjet;
    }

    public LocalDate getDateAffectation() {
        return dateAffectation;
    }

    public void setDateAffectation(LocalDate dateAffectation) {
        this.dateAffectation = dateAffectation;
    }

    public LocalDate getDateFinAffectation() {
        return dateFinAffectation;
    }

    public void setDateFinAffectation(LocalDate dateFinAffectation) {
        this.dateFinAffectation = dateFinAffectation;
    }
}