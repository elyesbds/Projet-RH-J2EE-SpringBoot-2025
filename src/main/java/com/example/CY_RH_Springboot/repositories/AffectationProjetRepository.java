package com.example.CY_RH_Springboot.repositories;

import com.example.CY_RH_Springboot.models.AffectationProjet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AffectationProjetRepository extends JpaRepository<AffectationProjet, Integer> {
    List<AffectationProjet> findByIdProjet(Integer idProjet);
    List<AffectationProjet> findByIdEmployer(Long idEmployer);
}