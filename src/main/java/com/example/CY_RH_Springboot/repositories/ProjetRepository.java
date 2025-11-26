package com.example.CY_RH_Springboot.repositories;

import com.example.CY_RH_Springboot.models.Projet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjetRepository extends JpaRepository<Projet, Integer> {

    @Query("SELECT DISTINCT p FROM Projet p JOIN AffectationProjet a ON p.id = a.idProjet WHERE a.idEmployer = :employeeId")
    List<Projet> findProjetsByEmployeeId(@Param("employeeId") Long employeeId);
}