package com.example.CY_RH_Springboot.controllers;

import com.example.CY_RH_Springboot.models.FicheDePaie;
import com.example.CY_RH_Springboot.models.Employee;
import com.example.CY_RH_Springboot.repositories.FicheDePaieRepository;
import com.example.CY_RH_Springboot.repositories.EmployeeRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/fiches-paie")
public class FicheDePaieController {

    private final FicheDePaieRepository ficheDePaieRepository;
    private final EmployeeRepository employeeRepository;

    public FicheDePaieController(FicheDePaieRepository ficheDePaieRepository,
                                 EmployeeRepository employeeRepository) {
        this.ficheDePaieRepository = ficheDePaieRepository;
        this.employeeRepository = employeeRepository;
    }

    // Liste des fiches de paie
    @GetMapping
    public String listFichesDePaie(Model model) {
        List<FicheDePaie> fichesPaie = ficheDePaieRepository.findAll();
        List<Employee> employees = employeeRepository.findAll();

        model.addAttribute("fichesPaie", fichesPaie);
        model.addAttribute("employees", employees);
        return "fiches-paie/fiches_paie";
    }

    // Affiche le formulaire pour ajouter
    @GetMapping("/add")
    public String showAddForm(Model model) {
        List<Employee> employees = employeeRepository.findAll();

        FicheDePaie fichePaie = new FicheDePaie();
        fichePaie.setDateGeneration(LocalDate.now());
        fichePaie.setPrimes(BigDecimal.ZERO);
        fichePaie.setDeductions(BigDecimal.ZERO);

        model.addAttribute("fichePaie", fichePaie);
        model.addAttribute("employees", employees);
        return "fiches-paie/fiche_paie_form";
    }

    // Sauvegarde une fiche de paie (ajout ou modification)
    @PostMapping("/save")
    public String saveFichePaie(@ModelAttribute FicheDePaie fichePaie) {
        // Calculer le net à payer automatiquement
        BigDecimal netAPayer = fichePaie.getSalaireBase()
                .add(fichePaie.getPrimes())
                .subtract(fichePaie.getDeductions());
        fichePaie.setNetAPayer(netAPayer);

        ficheDePaieRepository.save(fichePaie);
        return "redirect:/fiches-paie";
    }

    // Affiche le formulaire pour modifier une fiche de paie
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Integer id, Model model) {
        Optional<FicheDePaie> fichePaie = ficheDePaieRepository.findById(id);
        List<Employee> employees = employeeRepository.findAll();

        if (fichePaie.isPresent()) {
            model.addAttribute("fichePaie", fichePaie.get());
            model.addAttribute("employees", employees);
            return "fiches-paie/fiche_paie_form";
        }
        return "redirect:/fiches-paie";
    }

    // Supprimer une fiche de paie
    @GetMapping("/delete/{id}")
    public String deleteFichePaie(@PathVariable Integer id) {
        Optional<FicheDePaie> fichePaie = ficheDePaieRepository.findById(id);
        fichePaie.ifPresent(ficheDePaieRepository::delete);
        return "redirect:/fiches-paie";
    }
}