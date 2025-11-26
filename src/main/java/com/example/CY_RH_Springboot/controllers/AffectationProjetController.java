package com.example.CY_RH_Springboot.controllers;

import com.example.CY_RH_Springboot.models.AffectationProjet;
import com.example.CY_RH_Springboot.models.Employee;
import com.example.CY_RH_Springboot.models.Projet;
import com.example.CY_RH_Springboot.repositories.AffectationProjetRepository;
import com.example.CY_RH_Springboot.repositories.EmployeeRepository;
import com.example.CY_RH_Springboot.repositories.ProjetRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/affectations")
public class AffectationProjetController {

    private final AffectationProjetRepository affectationRepository;
    private final ProjetRepository projetRepository;
    private final EmployeeRepository employeeRepository;

    public AffectationProjetController(AffectationProjetRepository affectationRepository,
                                       ProjetRepository projetRepository,
                                       EmployeeRepository employeeRepository) {
        this.affectationRepository = affectationRepository;
        this.projetRepository = projetRepository;
        this.employeeRepository = employeeRepository;
    }

    // Vérifier si l'utilisateur peut gérer ce projet
    private boolean canManageProject(Authentication auth, Projet projet) {
        if (auth == null) return false;

        // Admin peut tout faire
        if (auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            return true;
        }

        // Chef de projet peut gérer son projet
        String email = auth.getName();
        Optional<Employee> currentUser = employeeRepository.findByEmail(email);

        if (currentUser.isPresent() && projet.getChefProjet() != null) {
            return currentUser.get().getId().equals(projet.getChefProjet().longValue());
        }

        return false;
    }

    // Liste des affectations d'un projet
    @GetMapping("/projet/{idProjet}")
    public String listAffectations(@PathVariable Integer idProjet,
                                   Model model,
                                   Authentication auth,
                                   RedirectAttributes redirectAttributes) {
        Optional<Projet> projetOpt = projetRepository.findById(idProjet);

        if (projetOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Projet introuvable");
            return "redirect:/projets";
        }

        Projet projet = projetOpt.get();

        // Vérifier les permissions
        if (!canManageProject(auth, projet)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vous n'avez pas la permission de gérer ce projet");
            return "redirect:/projets";
        }

        List<AffectationProjet> affectations = affectationRepository.findByIdProjet(idProjet);
        List<Employee> allEmployees = employeeRepository.findAll();

        model.addAttribute("projet", projet);
        model.addAttribute("affectations", affectations);
        model.addAttribute("employees", allEmployees);

        return "affectations/affectations";
    }

    // Afficher le formulaire pour ajouter une affectation
    @GetMapping("/projet/{idProjet}/add")
    public String showAddForm(@PathVariable Integer idProjet,
                              Model model,
                              Authentication auth,
                              RedirectAttributes redirectAttributes) {
        Optional<Projet> projetOpt = projetRepository.findById(idProjet);

        if (projetOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Projet introuvable");
            return "redirect:/projets";
        }

        Projet projet = projetOpt.get();

        if (!canManageProject(auth, projet)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vous n'avez pas la permission de gérer ce projet");
            return "redirect:/projets";
        }

        List<Employee> allEmployees = employeeRepository.findAll();
        AffectationProjet affectation = new AffectationProjet();
        affectation.setIdProjet(idProjet);
        affectation.setDateAffectation(LocalDate.now());

        model.addAttribute("projet", projet);
        model.addAttribute("affectation", affectation);
        model.addAttribute("employees", allEmployees);

        return "affectations/affectation_form";
    }

    // Sauvegarder une affectation
    @PostMapping("/save")
    public String saveAffectation(@ModelAttribute AffectationProjet affectation,
                                  Authentication auth,
                                  RedirectAttributes redirectAttributes) {
        Optional<Projet> projetOpt = projetRepository.findById(affectation.getIdProjet());

        if (projetOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Projet introuvable");
            return "redirect:/projets";
        }

        Projet projet = projetOpt.get();

        if (!canManageProject(auth, projet)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vous n'avez pas la permission de gérer ce projet");
            return "redirect:/projets";
        }

        affectationRepository.save(affectation);
        redirectAttributes.addFlashAttribute("successMessage", "Affectation enregistrée avec succès");
        return "redirect:/affectations/projet/" + affectation.getIdProjet();
    }

    // Afficher le formulaire de modification
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Integer id,
                               Model model,
                               Authentication auth,
                               RedirectAttributes redirectAttributes) {
        Optional<AffectationProjet> affectationOpt = affectationRepository.findById(id);

        if (affectationOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Affectation introuvable");
            return "redirect:/projets";
        }

        AffectationProjet affectation = affectationOpt.get();
        Optional<Projet> projetOpt = projetRepository.findById(affectation.getIdProjet());

        if (projetOpt.isEmpty() || !canManageProject(auth, projetOpt.get())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vous n'avez pas la permission de modifier cette affectation");
            return "redirect:/projets";
        }

        List<Employee> allEmployees = employeeRepository.findAll();

        model.addAttribute("projet", projetOpt.get());
        model.addAttribute("affectation", affectation);
        model.addAttribute("employees", allEmployees);

        return "affectations/affectation_form";
    }

    // Supprimer une affectation
    @GetMapping("/delete/{id}")
    public String deleteAffectation(@PathVariable Integer id,
                                    Authentication auth,
                                    RedirectAttributes redirectAttributes) {
        Optional<AffectationProjet> affectationOpt = affectationRepository.findById(id);

        if (affectationOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Affectation introuvable");
            return "redirect:/projets";
        }

        AffectationProjet affectation = affectationOpt.get();
        Optional<Projet> projetOpt = projetRepository.findById(affectation.getIdProjet());

        if (projetOpt.isEmpty() || !canManageProject(auth, projetOpt.get())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vous n'avez pas la permission de supprimer cette affectation");
            return "redirect:/projets";
        }

        Integer idProjet = affectation.getIdProjet();
        affectationRepository.delete(affectation);
        redirectAttributes.addFlashAttribute("successMessage", "Affectation supprimée avec succès");

        return "redirect:/affectations/projet/" + idProjet;
    }
}