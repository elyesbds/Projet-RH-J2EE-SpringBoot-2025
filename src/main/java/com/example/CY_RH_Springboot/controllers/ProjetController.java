package com.example.CY_RH_Springboot.controllers;

import com.example.CY_RH_Springboot.models.Projet;
import com.example.CY_RH_Springboot.models.Employee;
import com.example.CY_RH_Springboot.models.Departement;
import com.example.CY_RH_Springboot.repositories.ProjetRepository;
import com.example.CY_RH_Springboot.repositories.EmployeeRepository;
import com.example.CY_RH_Springboot.repositories.DepartementRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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

    public ProjetController(ProjetRepository projetRepository,
                            EmployeeRepository employeeRepository,
                            DepartementRepository departementRepository) {
        this.projetRepository = projetRepository;
        this.employeeRepository = employeeRepository;
        this.departementRepository = departementRepository;
    }

    // Vérifier si l'utilisateur peut créer/modifier des projets
    private boolean canManageProjects(Authentication auth) {
        if (auth == null) return false;

        // Admin peut tout faire
        if (auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            return true;
        }

        // Chef de projet peut créer/modifier des projets
        if (auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_CHEF_PROJET"))) {
            return true;
        }

        return false;
    }

    // Vérifier si l'utilisateur peut gérer ce projet spécifique
    private boolean canManageSpecificProject(Authentication auth, Projet projet) {
        if (auth == null) return false;

        // Admin peut tout faire
        if (auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            return true;
        }

        // Chef de projet peut gérer son propre projet
        String email = auth.getName();
        Optional<Employee> currentUser = employeeRepository.findByEmail(email);

        if (currentUser.isPresent() && projet.getChefProjet() != null) {
            return currentUser.get().getId().equals(projet.getChefProjet().longValue());
        }

        return false;
    }

    // Liste des projets
    @GetMapping
    public String listProjets(Model model, Authentication auth) {
        List<Projet> projets;
        List<Employee> employees = employeeRepository.findAll();
        List<Departement> departements = departementRepository.findAll();

        // Admin et Chef de département voient tous les projets
        if (auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")) ||
                auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_CHEF_DEPT"))) {
            projets = projetRepository.findAll();
        } else {
            // Les autres employés ne voient que leurs projets
            String email = auth.getName();
            Optional<Employee> currentUser = employeeRepository.findByEmail(email);

            if (currentUser.isPresent()) {
                // Récupérer les projets auxquels l'employé est affecté
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

    // Affiche le formulaire pour ajouter
    @GetMapping("/add")
    public String showAddForm(Model model, Authentication auth, RedirectAttributes redirectAttributes) {
        if (!canManageProjects(auth)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vous n'avez pas la permission de créer un projet");
            return "redirect:/projets";
        }

        List<Employee> employees = employeeRepository.findAll();
        List<Departement> departements = departementRepository.findAll();

        model.addAttribute("projet", new Projet());
        model.addAttribute("employees", employees);
        model.addAttribute("departements", departements);
        return "projets/projet_form";
    }

    // Sauvegarde un projet (ajout ou modification)
    @PostMapping("/save")
    public String saveProjet(@ModelAttribute Projet projet, Authentication auth, RedirectAttributes redirectAttributes) {
        // Vérification pour modification
        if (projet.getId() != null) {
            Optional<Projet> existingProjet = projetRepository.findById(projet.getId());
            if (existingProjet.isPresent() && !canManageSpecificProject(auth, existingProjet.get())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Vous n'avez pas la permission de modifier ce projet");
                return "redirect:/projets";
            }
        } else {
            // Vérification pour création
            if (!canManageProjects(auth)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Vous n'avez pas la permission de créer un projet");
                return "redirect:/projets";
            }
        }

        projetRepository.save(projet);
        redirectAttributes.addFlashAttribute("successMessage", "Projet enregistré avec succès");
        return "redirect:/projets";
    }

    // Affiche le formulaire pour modifier un projet
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Integer id, Model model, Authentication auth, RedirectAttributes redirectAttributes) {
        Optional<Projet> projet = projetRepository.findById(id);

        if (projet.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Projet introuvable");
            return "redirect:/projets";
        }

        if (!canManageSpecificProject(auth, projet.get())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vous n'avez pas la permission de modifier ce projet");
            return "redirect:/projets";
        }

        List<Employee> employees = employeeRepository.findAll();
        List<Departement> departements = departementRepository.findAll();

        model.addAttribute("projet", projet.get());
        model.addAttribute("employees", employees);
        model.addAttribute("departements", departements);
        return "projets/projet_form";
    }

    // Supprimer un projet
    @GetMapping("/delete/{id}")
    public String deleteProjet(@PathVariable Integer id, Authentication auth, RedirectAttributes redirectAttributes) {
        Optional<Projet> projet = projetRepository.findById(id);

        if (projet.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Projet introuvable");
            return "redirect:/projets";
        }

        if (!canManageSpecificProject(auth, projet.get())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vous n'avez pas la permission de supprimer ce projet");
            return "redirect:/projets";
        }

        projetRepository.delete(projet.get());
        redirectAttributes.addFlashAttribute("successMessage", "Projet supprimé avec succès");
        return "redirect:/projets";
    }
}