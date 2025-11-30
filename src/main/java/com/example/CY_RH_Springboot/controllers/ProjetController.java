package com.example.CY_RH_Springboot.controllers;

import com.example.CY_RH_Springboot.models.Projet;
import com.example.CY_RH_Springboot.models.Employee;
import com.example.CY_RH_Springboot.models.Departement;
import com.example.CY_RH_Springboot.repositories.ProjetRepository;
import com.example.CY_RH_Springboot.repositories.EmployeeRepository;
import com.example.CY_RH_Springboot.repositories.DepartementRepository;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/projets")
public class ProjetController {

    private final ProjetRepository projetRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartementRepository departementRepository;

    public ProjetController(
            ProjetRepository projetRepository,
            EmployeeRepository employeeRepository,
            DepartementRepository departementRepository
    ) {
        this.projetRepository = projetRepository;
        this.employeeRepository = employeeRepository;
        this.departementRepository = departementRepository;
    }

    // Permissions : inchangées
    private boolean canManageProjects(Authentication auth) {
        if (auth == null) return false;
        if (auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) return true;
        if (auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_CHEF_PROJET"))) return true;
        return false;
    }

    private boolean canManageSpecificProject(Authentication auth, Projet projet) {
        if (auth == null) return false;

        if (auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) return true;

        String email = auth.getName();
        Optional<Employee> currentUser = employeeRepository.findByEmail(email);

        if (currentUser.isPresent() && projet.getChefProjet() != null) {
            return currentUser.get().getId().equals(projet.getChefProjet().longValue());
        }
        return false;
    }

    // === LISTE ===
    @GetMapping
    public String listProjets(Model model, Authentication auth) {
        List<Projet> projets;
        List<Employee> employees = employeeRepository.findAll();
        List<Departement> departements = departementRepository.findAll();

        if (auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")) ||
                auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_CHEF_DEPT"))) {
            projets = projetRepository.findAll();
        } else {
            String email = auth.getName();
            Optional<Employee> currentUser = employeeRepository.findByEmail(email);

            if (currentUser.isPresent()) {
                projets = projetRepository.findProjetsByEmployeeId(currentUser.get().getId());
            } else {
                projets = List.of();
            }
        }

        model.addAttribute("projets", projets);
        model.addAttribute("employees", employees);
        model.addAttribute("departements", departements);

        return "projets/projets";
    }

    // === FORMULAIRE AJOUT ===
    @GetMapping("/add")
    public String showAddForm(Model model, Authentication auth, RedirectAttributes redirectAttributes) {

        if (!canManageProjects(auth)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vous n'avez pas la permission de créer un projet");
            return "redirect:/projets";
        }

        model.addAttribute("projet", new Projet());
        model.addAttribute("employees", employeeRepository.findAll());
        model.addAttribute("departements", departementRepository.findAll());

        return "projets/projet_form";
    }

    // === SAUVEGARDE AVEC VALIDATIONS ===
    @PostMapping("/save")
    public String saveProjet(
            @Valid @ModelAttribute Projet projet,
            BindingResult bindingResult,
            Authentication auth,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        // ✦ Vérification autorisations
        if (projet.getId() != null) {
            Optional<Projet> existing = projetRepository.findById(projet.getId());
            if (existing.isPresent() && !canManageSpecificProject(auth, existing.get())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Vous ne pouvez pas modifier ce projet");
                return "redirect:/projets";
            }
        } else {
            if (!canManageProjects(auth)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Vous ne pouvez pas créer de projet");
                return "redirect:/projets";
            }
        }

        // ✦ VALIDATIONS MÉTIER

        // → Date fin prévue >= date début
        if (projet.getDateFinPrevue() != null &&
                projet.getDateFinPrevue().isBefore(projet.getDateDebut())) {
            bindingResult.rejectValue("dateFinPrevue", "error.projet",
                    "La date de fin prévue doit être après la date de début");
        }

        // → Date fin réelle >= date début
        if (projet.getDateFinReelle() != null &&
                projet.getDateFinReelle().isBefore(projet.getDateDebut())) {
            bindingResult.rejectValue("dateFinReelle", "error.projet",
                    "La date de fin réelle doit être après la date de début");
        }

        // → Si projet TERMINE alors date fin réelle obligatoire
        if (projet.getEtatProjet().equals("TERMINE") && projet.getDateFinReelle() == null) {
            bindingResult.rejectValue("dateFinReelle", "error.projet",
                    "Vous devez renseigner la date de fin réelle pour un projet terminé");
        }

        // → Chef de projet doit exister (si renseigné)
        if (projet.getChefProjet() != null &&
                !employeeRepository.existsById(projet.getChefProjet().longValue())) {
            bindingResult.rejectValue("chefProjet", "error.projet", "Employé introuvable");
        }

        // → Département doit exister (si renseigné)
        if (projet.getIdDepartement() != null &&
                !departementRepository.existsById((int) projet.getIdDepartement().longValue())) {
            bindingResult.rejectValue("idDepartement", "error.projet", "Département introuvable");
        }

        // ✦ Si erreurs → on renvoie au formulaire
        if (bindingResult.hasErrors()) {
            model.addAttribute("employees", employeeRepository.findAll());
            model.addAttribute("departements", departementRepository.findAll());
            return "projets/projet_form";
        }

        // ✦ Sauvegarde
        projetRepository.save(projet);

        // === Mise à jour du rôle chef de projet ===
        employeeRepository.findAll().forEach(emp -> {
            if (emp.getRole().equals("CHEF_PROJET") &&
                    (projet.getChefProjet() == null ||
                            !emp.getId().equals(projet.getChefProjet().longValue()))) {
                emp.setRole("EMPLOYE");
                employeeRepository.save(emp);
            }
        });

        if (projet.getChefProjet() != null) {
            employeeRepository.findById(projet.getChefProjet().longValue()).ifPresent(emp -> {
                emp.setRole("CHEF_PROJET");
                employeeRepository.save(emp);
            });
        }

        redirectAttributes.addFlashAttribute("successMessage", "Projet enregistré avec succès");
        return "redirect:/projets";
    }

    // === FORMULAIRE ÉDITION ===
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Integer id, Model model, Authentication auth, RedirectAttributes redirectAttributes) {

        Optional<Projet> projet = projetRepository.findById(id);

        if (projet.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Projet introuvable");
            return "redirect:/projets";
        }

        if (!canManageSpecificProject(auth, projet.get())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vous ne pouvez pas modifier ce projet");
            return "redirect:/projets";
        }

        model.addAttribute("projet", projet.get());
        model.addAttribute("employees", employeeRepository.findAll());
        model.addAttribute("departements", departementRepository.findAll());

        return "projets/projet_form";
    }

    // === SUPPRESSION ===
    @GetMapping("/delete/{id}")
    public String deleteProjet(@PathVariable Integer id, Authentication auth, RedirectAttributes redirectAttributes) {

        Optional<Projet> projet = projetRepository.findById(id);

        if (projet.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Projet introuvable");
            return "redirect:/projets";
        }

        if (!canManageSpecificProject(auth, projet.get())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vous ne pouvez pas supprimer ce projet");
            return "redirect:/projets";
        }

        projetRepository.delete(projet.get());

        redirectAttributes.addFlashAttribute("successMessage", "Projet supprimé avec succès");
        return "redirect:/projets";
    }
}
