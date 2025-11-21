package com.example.CY_RH_Springboot.controllers;

import com.example.CY_RH_Springboot.models.Projet;
import com.example.CY_RH_Springboot.models.Employee;
import com.example.CY_RH_Springboot.models.Departement;
import com.example.CY_RH_Springboot.repositories.ProjetRepository;
import com.example.CY_RH_Springboot.repositories.EmployeeRepository;
import com.example.CY_RH_Springboot.repositories.DepartementRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/projets")
public class ProjetController {

    private final ProjetRepository projetRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartementRepository departementRepository;

    public ProjetController(ProjetRepository projetRepository,
                            EmployeeRepository employeeRepository,
                            DepartementRepository departementRepository) {
        this.projetRepository = projetRepository;
        this.employeeRepository = employeeRepository;
        this.departementRepository = departementRepository;
    }

    // Liste des projets
    @GetMapping
    public String listProjets(Model model) {
        List<Projet> projets = projetRepository.findAll();
        List<Employee> employees = employeeRepository.findAll();
        List<Departement> departements = departementRepository.findAll();

        model.addAttribute("projets", projets);
        model.addAttribute("employees", employees);
        model.addAttribute("departements", departements);
        return "projets/projets";
    }

    // Affiche le formulaire pour ajouter
    @GetMapping("/add")
    public String showAddForm(Model model) {
        List<Employee> employees = employeeRepository.findAll();
        List<Departement> departements = departementRepository.findAll();

        model.addAttribute("projet", new Projet());
        model.addAttribute("employees", employees);
        model.addAttribute("departements", departements);
        return "projets/projet_form";
    }

    // Sauvegarde un projet (ajout ou modification)
    @PostMapping("/save")
    public String saveProjet(@ModelAttribute Projet projet) {
        projetRepository.save(projet);
        return "redirect:/projets";
    }

    // Affiche le formulaire pour modifier un projet
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Integer id, Model model) {
        Optional<Projet> projet = projetRepository.findById(id);
        List<Employee> employees = employeeRepository.findAll();
        List<Departement> departements = departementRepository.findAll();

        if (projet.isPresent()) {
            model.addAttribute("projet", projet.get());
            model.addAttribute("employees", employees);
            model.addAttribute("departements", departements);
            return "projets/projet_form";
        }
        return "redirect:/projets";
    }

    // Supprimer un projet
    @GetMapping("/delete/{id}")
    public String deleteProjet(@PathVariable Integer id) {
        Optional<Projet> projet = projetRepository.findById(id);
        projet.ifPresent(projetRepository::delete);
        return "redirect:/projets";
    }
}