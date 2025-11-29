package com.example.CY_RH_Springboot.controllers;

import com.example.CY_RH_Springboot.models.Employee;
import com.example.CY_RH_Springboot.models.Departement;
import com.example.CY_RH_Springboot.repositories.EmployeeRepository;
import com.example.CY_RH_Springboot.repositories.DepartementRepository;
import com.example.CY_RH_Springboot.services.PasswordEncoderService;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.springframework.validation.BindingResult;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Controller
@RequestMapping("/employees")
public class EmployeeController {

    private final EmployeeRepository employeeRepository;
    private final DepartementRepository departmentRepository;
    private final PasswordEncoderService passwordEncoder; // service proposé (bean)

    public EmployeeController(EmployeeRepository employeeRepository,
                              DepartementRepository departmentRepository,
                              PasswordEncoderService passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
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
        model.addAttribute("departements", departmentRepository.findAll());
        return "employees/employee_form";
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
            model.addAttribute("departements", departmentRepository.findAll());
            return "employees/employee_form";
        }
        redirectAttributes.addFlashAttribute("errorMessage", "Employé introuvable");
        return "redirect:/employees";
    }

    // Sauvegarde un employé (ajout ou modification)
    @PostMapping("/save")
    public String saveEmployee(@ModelAttribute("employee") Employee employee,
                               BindingResult result,
                               Model model,
                               Authentication auth,
                               RedirectAttributes redirectAttributes) {

        if (!isAdmin(auth)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Seul un administrateur peut modifier un employé");
            return "redirect:/employees";
        }

        boolean isNew = (employee.getId() == null);

        // --- VALIDATION CONDITIONNELLE (Groupes OnCreate / OnUpdate) ---
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        Set<ConstraintViolation<Employee>> violations;
        if (isNew) {
            violations = validator.validate(employee, Employee.OnCreate.class);
        } else {
            violations = validator.validate(employee, Employee.OnUpdate.class);
        }
        // transformer les violations en BindingResult (pour affichage Thymeleaf)
        for (ConstraintViolation<Employee> v : violations) {
            String propertyPath = v.getPropertyPath().toString();
            String message = v.getMessage();
            result.rejectValue(propertyPath, null, message);
        }

        // Exemple de validation manuelle supplémentaire : si modification et password non vide mais < 6
        if (!isNew && employee.getPassword() != null && !employee.getPassword().isEmpty() && employee.getPassword().length() < 6) {
            result.rejectValue("password", null, "Le mot de passe doit contenir au moins 6 caractères");
        }

        // Si erreurs → renvoyer le formulaire AVEC la liste des départements
        if (result.hasErrors()) {
            model.addAttribute("departements", departmentRepository.findAll());
            model.addAttribute("employee", employee);
            return "employees/employee_form";
        }

        // --- GESTION DU MOT DE PASSE ---
        if (isNew) {
            // création : on doit avoir un mot de passe (validé par le groupe OnCreate)
            // encoder le mot de passe avant sauvegarde
            // Utilise le service passwordEncoder (bean). Si tu préfères, remplace par :
            // String hashed = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(employee.getPassword());
            String hashed = passwordEncoder.encode(employee.getPassword());
            employee.setPassword(hashed);
        } else {
            // modification : si le champ password est vide -> conserver l'ancien
            Optional<Employee> oldOpt = employeeRepository.findById(employee.getId());
            if (oldOpt.isPresent()) {
                Employee old = oldOpt.get();
                if (employee.getPassword() == null || employee.getPassword().isEmpty()) {
                    employee.setPassword(old.getPassword());
                } else {
                    // nouveau mot de passe -> encoder
                    String hashed = passwordEncoder.encode(employee.getPassword());
                    employee.setPassword(hashed);
                }
            } else {
                // cas improbable : garder logique défensive
                if (employee.getPassword() != null && !employee.getPassword().isEmpty()) {
                    String hashed = passwordEncoder.encode(employee.getPassword());
                    employee.setPassword(hashed);
                }
            }
        }

        // Optionnel : vérifier la cohérence idDepartement -> exiger qu'il existe si non null
        if (employee.getIdDepartement() != null) {
            boolean exists = departmentRepository.findById(employee.getIdDepartement()).isPresent();
            if (!exists) {
                redirectAttributes.addFlashAttribute("errorMessage", "Le département sélectionné est invalide");
                return "redirect:/employees/add";
            }
        }

        employeeRepository.save(employee);
        redirectAttributes.addFlashAttribute("successMessage", "Employé enregistré avec succès");
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

    // Export PDF et autres méthodes inchangées...
}
