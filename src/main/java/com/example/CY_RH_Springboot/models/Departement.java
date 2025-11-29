package com.example.CY_RH_Springboot.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "departement")
public class Departement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "L'intitulé est obligatoire")
    @Size(min = 3, max = 100, message = "L'intitulé doit contenir entre 3 et 100 caractères")
    @Column(name = "Intitule", nullable = false, unique = true, length = 100)
    private String intitule;

    @Column(name = "Chef_departement")
    private Integer chefDepartement;

    public Departement() {}

    // Getters et Setters (inchangés)
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getIntitule() { return intitule; }
    public void setIntitule(String intitule) { this.intitule = intitule; }
    public Integer getChefDepartement() { return chefDepartement; }
    public void setChefDepartement(Integer chefDepartement) { this.chefDepartement = chefDepartement; }
}