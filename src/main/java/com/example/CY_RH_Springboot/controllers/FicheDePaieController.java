package com.example.CY_RH_Springboot.controllers;

import com.example.CY_RH_Springboot.models.FicheDePaie;
import com.example.CY_RH_Springboot.models.Employee;
import com.example.CY_RH_Springboot.repositories.FicheDePaieRepository;
import com.example.CY_RH_Springboot.repositories.EmployeeRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    // Vérifier si l'utilisateur est admin
    private boolean isAdmin(Authentication auth) {
        if (auth == null) return false;
        return auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    // Vérifier si l'utilisateur est chef de département
    private boolean isChefDept(Authentication auth) {
        if (auth == null) return false;
        return auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_CHEF_DEPT"));
    }

    // Vérifier si l'utilisateur peut modifier une fiche de paie
    private boolean canModifyFichePaie(Authentication auth, FicheDePaie fiche) {
        if (isAdmin(auth)) return true;

        String email = auth.getName();
        Optional<Employee> currentUser = employeeRepository.findByEmail(email);

        if (currentUser.isEmpty()) return false;

        // Chef de département peut modifier les fiches de son département
        if (isChefDept(auth)) {
            Optional<Employee> ficheEmployee = employeeRepository.findById(fiche.getIdEmployer());
            if (ficheEmployee.isPresent() && ficheEmployee.get().getIdDepartement() != null && currentUser.get().getIdDepartement() != null) {
                // Comparer les ID de département
                Integer chefDeptId = currentUser.get().getIdDepartement();
                Integer employeeDeptId = ficheEmployee.get().getIdDepartement();
                return chefDeptId.equals(employeeDeptId);
            }
        }

        return false;
    }

    // Liste des fiches de paie
    @GetMapping
    public String listFichesDePaie(Model model, Authentication auth) {
        List<FicheDePaie> fichesPaie;
        List<Employee> employees = employeeRepository.findAll();

        if (isAdmin(auth)) {
            // Admin voit toutes les fiches
            fichesPaie = ficheDePaieRepository.findAll();
        } else if (isChefDept(auth)) {
            // Chef de département voit les fiches de son département
            String email = auth.getName();
            Optional<Employee> currentUser = employeeRepository.findByEmail(email);

            if (currentUser.isPresent() && currentUser.get().getIdDepartement() != null) {
                Integer deptId = currentUser.get().getIdDepartement();
                List<Integer> employeeIds = employees.stream()
                        .filter(e -> e.getIdDepartement() != null && e.getIdDepartement().equals(deptId))
                        .map(e -> e.getId().intValue())
                        .collect(Collectors.toList());

                fichesPaie = ficheDePaieRepository.findAll().stream()
                        .filter(f -> employeeIds.contains(f.getIdEmployer()))
                        .collect(Collectors.toList());
            } else {
                fichesPaie = List.of();
            }
        } else {
            // Employé normal ne voit que ses propres fiches
            String email = auth.getName();
            Optional<Employee> currentUser = employeeRepository.findByEmail(email);

            if (currentUser.isPresent()) {
                fichesPaie = ficheDePaieRepository.findByIdEmployer(currentUser.get().getId().intValue());
            } else {
                fichesPaie = List.of();
            }
        }

        model.addAttribute("fichesPaie", fichesPaie);
        model.addAttribute("employees", employees);
        return "fiches-paie/fiches_paie";
    }

    // Affiche le formulaire pour ajouter
    @GetMapping("/add")
    public String showAddForm(Model model, Authentication auth, RedirectAttributes redirectAttributes) {
        if (!isAdmin(auth) && !isChefDept(auth)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vous n'avez pas la permission de créer une fiche de paie");
            return "redirect:/fiches-paie";
        }

        List<Employee> employees;

        if (isAdmin(auth)) {
            employees = employeeRepository.findAll();
        } else {
            // Chef de département ne peut créer que pour son département
            String email = auth.getName();
            Optional<Employee> currentUser = employeeRepository.findByEmail(email);

            if (currentUser.isPresent() && currentUser.get().getIdDepartement() != null) {
                Integer deptId = currentUser.get().getIdDepartement();
                employees = employeeRepository.findAll().stream()
                        .filter(e -> e.getIdDepartement() != null && e.getIdDepartement().equals(deptId))
                        .collect(Collectors.toList());
            } else {
                employees = List.of();
            }
        }

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
    public String saveFichePaie(@ModelAttribute FicheDePaie fichePaie, Authentication auth, RedirectAttributes redirectAttributes) {
        // Vérification pour modification
        if (fichePaie.getId() != null) {
            Optional<FicheDePaie> existingFiche = ficheDePaieRepository.findById(fichePaie.getId());
            if (existingFiche.isPresent() && !canModifyFichePaie(auth, existingFiche.get())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Vous n'avez pas la permission de modifier cette fiche de paie");
                return "redirect:/fiches-paie";
            }
        } else {
            // Pour la création
            if (!isAdmin(auth) && !isChefDept(auth)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Vous n'avez pas la permission de créer une fiche de paie");
                return "redirect:/fiches-paie";
            }
        }

        // Calculer le net à payer automatiquement
        BigDecimal netAPayer = fichePaie.getSalaireBase()
                .add(fichePaie.getPrimes())
                .subtract(fichePaie.getDeductions());
        fichePaie.setNetAPayer(netAPayer);

        ficheDePaieRepository.save(fichePaie);
        redirectAttributes.addFlashAttribute("successMessage", "Fiche de paie enregistrée avec succès");
        return "redirect:/fiches-paie";
    }

    // Affiche le formulaire pour modifier une fiche de paie
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Integer id, Model model, Authentication auth, RedirectAttributes redirectAttributes) {
        Optional<FicheDePaie> fichePaie = ficheDePaieRepository.findById(id);

        if (fichePaie.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Fiche de paie introuvable");
            return "redirect:/fiches-paie";
        }

        if (!canModifyFichePaie(auth, fichePaie.get())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vous n'avez pas la permission de modifier cette fiche de paie");
            return "redirect:/fiches-paie";
        }

        List<Employee> employees = employeeRepository.findAll();

        model.addAttribute("fichePaie", fichePaie.get());
        model.addAttribute("employees", employees);
        return "fiches-paie/fiche_paie_form";
    }

    // Supprimer une fiche de paie
    @GetMapping("/delete/{id}")
    public String deleteFichePaie(@PathVariable Integer id, Authentication auth, RedirectAttributes redirectAttributes) {
        Optional<FicheDePaie> fichePaie = ficheDePaieRepository.findById(id);

        if (fichePaie.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Fiche de paie introuvable");
            return "redirect:/fiches-paie";
        }

        if (!canModifyFichePaie(auth, fichePaie.get())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vous n'avez pas la permission de supprimer cette fiche de paie");
            return "redirect:/fiches-paie";
        }

        ficheDePaieRepository.delete(fichePaie.get());
        redirectAttributes.addFlashAttribute("successMessage", "Fiche de paie supprimée avec succès");
        return "redirect:/fiches-paie";
    }
}