package com.example.CY_RH_Springboot.models;

import jakarta.persistence.*;

@Entity
@Table(name = "departement")
public class Departement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "Intitule", nullable = false, unique = true, length = 100)
    private String intitule;

    @Column(name = "Chef_departement")
    private Integer chefDepartement;

    public Departement() {}

    // Getters et Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getIntitule() {
        return intitule;
    }

    public void setIntitule(String intitule) {
        this.intitule = intitule;
    }

    public Integer getChefDepartement() {
        return chefDepartement;
    }

    public void setChefDepartement(Integer chefDepartement) {
        this.chefDepartement = chefDepartement;
    }
}