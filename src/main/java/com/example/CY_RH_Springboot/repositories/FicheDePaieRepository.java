package com.example.CY_RH_Springboot.repositories;

import com.example.CY_RH_Springboot.models.FicheDePaie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FicheDePaieRepository extends JpaRepository<FicheDePaie, Integer> {
    List<FicheDePaie> findByIdEmployer(Integer idEmployer);
    void deleteByIdEmployer(Long idEmployer);
}