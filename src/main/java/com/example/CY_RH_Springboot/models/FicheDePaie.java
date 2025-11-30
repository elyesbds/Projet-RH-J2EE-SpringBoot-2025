package com.example.CY_RH_Springboot.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "fiche_de_paie")
public class FicheDePaie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull(message = "L'employé est obligatoire")
    @Column(name = "Id_employer", nullable = false)
    private Long idEmployer;

    @NotNull(message = "Le mois est obligatoire")
    @Min(value = 1, message = "Le mois doit être entre 1 et 12")
    @Max(value = 12, message = "Le mois doit être entre 1 et 12")
    @Column(name = "Mois", nullable = false)
    private Integer mois;

    @NotNull(message = "L'année est obligatoire")
    @Min(value = 2020, message = "L'année doit être au minimum 2020")
    @Max(value = 2100, message = "L'année est trop élevée")
    @Column(name = "Annee", nullable = false)
    private Integer annee;

    @NotNull(message = "Le salaire de base est obligatoire")
    @DecimalMin(value = "1000.00", message = "Le salaire de base doit être au minimum 1000€")
    @DecimalMax(value = "1000000.00", message = "Le salaire de base ne peut pas dépasser 1 000 000€")
    @Column(name = "Salaire_base", nullable = false)
    private BigDecimal salaireBase;

    @DecimalMin(value = "0.00", message = "Les primes doivent être positives")
    @DecimalMax(value = "100000.00", message = "Les primes ne peuvent pas dépasser 100 000€")
    @Column(name = "Primes", nullable = false)
    private BigDecimal primes = BigDecimal.ZERO;

    @DecimalMin(value = "0.00", message = "Les déductions doivent être positives")
    @DecimalMax(value = "100000.00", message = "Les déductions ne peuvent pas dépasser 100 000€")
    @Column(name = "Deductions", nullable = false)
    private BigDecimal deductions = BigDecimal.ZERO;

    @Column(name = "Net_a_payer", nullable = false)
    private BigDecimal netAPayer;

    @NotNull(message = "La date de génération est obligatoire")
    @PastOrPresent(message = "La date de génération ne peut pas être dans le futur")
    @Column(name = "Date_generation", nullable = false)
    private LocalDate dateGeneration;

    public FicheDePaie() {}

    // Getters et Setters (inchangés)
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Long getIdEmployer() { return idEmployer; }
    public void setIdEmployer(Long idEmployer) { this.idEmployer = idEmployer; }
    public Integer getMois() { return mois; }
    public void setMois(Integer mois) { this.mois = mois; }
    public Integer getAnnee() { return annee; }
    public void setAnnee(Integer annee) { this.annee = annee; }
    public BigDecimal getSalaireBase() { return salaireBase; }
    public void setSalaireBase(BigDecimal salaireBase) { this.salaireBase = salaireBase; }
    public BigDecimal getPrimes() { return primes; }
    public void setPrimes(BigDecimal primes) { this.primes = primes; }
    public BigDecimal getDeductions() { return deductions; }
    public void setDeductions(BigDecimal deductions) { this.deductions = deductions; }
    public BigDecimal getNetAPayer() { return netAPayer; }
    public void setNetAPayer(BigDecimal netAPayer) { this.netAPayer = netAPayer; }
    public LocalDate getDateGeneration() { return dateGeneration; }
    public void setDateGeneration(LocalDate dateGeneration) { this.dateGeneration = dateGeneration; }
}