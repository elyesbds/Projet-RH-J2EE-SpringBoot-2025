package com.example.CY_RH_Springboot.repositories;

import com.example.CY_RH_Springboot.models.Projet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjetRepository extends JpaRepository<Projet, Integer> {
}