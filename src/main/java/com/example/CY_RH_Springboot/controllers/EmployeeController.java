package com.example.CY_RH_Springboot.controllers;

import com.example.CY_RH_Springboot.models.Employee;
import com.example.CY_RH_Springboot.repositories.EmployeeRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/employees")
public class EmployeeController {

    private final EmployeeRepository employeeRepository;

    public EmployeeController(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    // Vérifier si l'utilisateur est admin
    private boolean isAdmin(Authentication auth) {
        if (auth == null) return false;
        return auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    // Liste des employés
    @GetMapping
    public String listEmployees(Model model) {
        List<Employee> employees = employeeRepository.findAll();
        model.addAttribute("employees", employees);
        return "employees/employees";
    }

    // Affiche le formulaire pour ajouter
    @GetMapping("/add")
    public String showAddForm(Model model, Authentication auth, RedirectAttributes redirectAttributes) {
        if (!isAdmin(auth)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Seul un administrateur peut ajouter un employé");
            return "redirect:/employees";
        }

        model.addAttribute("employee", new Employee());
        return "employees/employee_form";
    }

    // Sauvegarde un employé (ajout ou modification)
    @PostMapping("/save")
    public String saveEmployee(@ModelAttribute Employee employee, Authentication auth, RedirectAttributes redirectAttributes) {
        if (!isAdmin(auth)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Seul un administrateur peut modifier un employé");
            return "redirect:/employees";
        }

        employeeRepository.save(employee);
        redirectAttributes.addFlashAttribute("successMessage", "Employé enregistré avec succès");
        return "redirect:/employees";
    }

    // Affiche le formulaire pour modifier un employé
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, Authentication auth, RedirectAttributes redirectAttributes) {
        if (!isAdmin(auth)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Seul un administrateur peut modifier un employé");
            return "redirect:/employees";
        }

        Optional<Employee> employee = employeeRepository.findById(id);
        if (employee.isPresent()) {
            model.addAttribute("employee", employee.get());
            return "employees/employee_form";
        }
        return "redirect:/employees";
    }

    // Supprimer un employé
    @GetMapping("/delete/{id}")
    public String deleteEmployee(@PathVariable Long id, Authentication auth, RedirectAttributes redirectAttributes) {
        if (!isAdmin(auth)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Seul un administrateur peut supprimer un employé");
            return "redirect:/employees";
        }

        Optional<Employee> employee = employeeRepository.findById(id);
        employee.ifPresent(employeeRepository::delete);
        redirectAttributes.addFlashAttribute("successMessage", "Employé supprimé avec succès");
        return "redirect:/employees";
    }
}