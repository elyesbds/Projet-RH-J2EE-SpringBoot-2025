package com.example.CY_RH_Springboot.controllers;

import com.example.CY_RH_Springboot.models.Departement;
import com.example.CY_RH_Springboot.models.Employee;
import com.example.CY_RH_Springboot.repositories.DepartementRepository;
import com.example.CY_RH_Springboot.repositories.EmployeeRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/departements")
public class DepartementController {

    private final DepartementRepository departementRepository;
    private final EmployeeRepository employeeRepository;

    public DepartementController(DepartementRepository departementRepository,
                                 EmployeeRepository employeeRepository) {
        this.departementRepository = departementRepository;
        this.employeeRepository = employeeRepository;
    }

    // Liste des départements
    @GetMapping
    public String listDepartements(Model model) {
        List<Departement> departements = departementRepository.findAll();
        List<Employee> employees = employeeRepository.findAll();

        model.addAttribute("departements", departements);
        model.addAttribute("employees", employees);
        return "departements/departements";
    }

    // Affiche le formulaire pour ajouter
    @GetMapping("/add")
    public String showAddForm(Model model) {
        List<Employee> employees = employeeRepository.findAll();

        model.addAttribute("departement", new Departement());
        model.addAttribute("employees", employees);
        return "departements/departement_form";
    }

    // Sauvegarde un département (ajout ou modification)
    @PostMapping("/save")
    public String saveDepartement(@ModelAttribute Departement departement) {
        departementRepository.save(departement);
        return "redirect:/departements";
    }

    // Affiche le formulaire pour modifier un département
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Integer id, Model model) {
        Optional<Departement> departement = departementRepository.findById(id);
        List<Employee> employees = employeeRepository.findAll();

        if (departement.isPresent()) {
            model.addAttribute("departement", departement.get());
            model.addAttribute("employees", employees);
            return "departements/departement_form";
        }
        return "redirect:/departements";
    }

    // Supprimer un département
    @GetMapping("/delete/{id}")
    public String deleteDepartement(@PathVariable Integer id) {
        Optional<Departement> departement = departementRepository.findById(id);
        departement.ifPresent(departementRepository::delete);
        return "redirect:/departements";
    }
}