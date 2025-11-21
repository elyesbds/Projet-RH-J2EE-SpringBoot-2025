package com.example.CY_RH_Springboot.repositories;

import com.example.CY_RH_Springboot.models.Departement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartementRepository extends JpaRepository<Departement, Integer> {
}