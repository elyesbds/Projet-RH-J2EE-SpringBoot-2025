package com.example.CY_RH_Springboot.models;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "fiche_de_paie")
public class FicheDePaie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "Id_employer", nullable = false)
    private Long idEmployer;

    @Column(name = "Mois", nullable = false)
    private Integer mois;

    @Column(name = "Annee", nullable = false)
    private Integer annee;

    @Column(name = "Salaire_base", nullable = false)
    private BigDecimal salaireBase;

    @Column(name = "Primes", nullable = false)
    private BigDecimal primes = BigDecimal.ZERO;

    @Column(name = "Deductions", nullable = false)
    private BigDecimal deductions = BigDecimal.ZERO;

    @Column(name = "Net_a_payer", nullable = false)
    private BigDecimal netAPayer;

    @Column(name = "Date_generation", nullable = false)
    private LocalDate dateGeneration;

    public FicheDePaie() {}

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

    public Integer getMois() {
        return mois;
    }

    public void setMois(Integer mois) {
        this.mois = mois;
    }

    public Integer getAnnee() {
        return annee;
    }

    public void setAnnee(Integer annee) {
        this.annee = annee;
    }

    public BigDecimal getSalaireBase() {
        return salaireBase;
    }

    public void setSalaireBase(BigDecimal salaireBase) {
        this.salaireBase = salaireBase;
    }

    public BigDecimal getPrimes() {
        return primes;
    }

    public void setPrimes(BigDecimal primes) {
        this.primes = primes;
    }

    public BigDecimal getDeductions() {
        return deductions;
    }

    public void setDeductions(BigDecimal deductions) {
        this.deductions = deductions;
    }

    public BigDecimal getNetAPayer() {
        return netAPayer;
    }

    public void setNetAPayer(BigDecimal netAPayer) {
        this.netAPayer = netAPayer;
    }

    public LocalDate getDateGeneration() {
        return dateGeneration;
    }

    public void setDateGeneration(LocalDate dateGeneration) {
        this.dateGeneration = dateGeneration;
    }
}