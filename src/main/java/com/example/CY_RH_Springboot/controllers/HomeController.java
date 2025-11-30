package com.example.CY_RH_Springboot.controllers;

import com.example.CY_RH_Springboot.repositories.DepartementRepository;
import com.example.CY_RH_Springboot.repositories.EmployeeRepository;
import com.example.CY_RH_Springboot.repositories.ProjetRepository;
import com.example.CY_RH_Springboot.repositories.FicheDePaieRepository;
import com.example.CY_RH_Springboot.repositories.AffectationProjetRepository;
import com.example.CY_RH_Springboot.models.Departement;
import com.example.CY_RH_Springboot.models.Employee;
import com.example.CY_RH_Springboot.models.Projet;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    private final EmployeeRepository employeeRepository;
    private final DepartementRepository departementRepository;
    private final ProjetRepository projetRepository;
    private final FicheDePaieRepository ficheDePaieRepository;
    private final AffectationProjetRepository affectationRepository;

    public HomeController(EmployeeRepository employeeRepository,
            DepartementRepository departementRepository,
            ProjetRepository projetRepository,
            FicheDePaieRepository ficheDePaieRepository,
            AffectationProjetRepository affectationRepository) {
        this.employeeRepository = employeeRepository;
        this.departementRepository = departementRepository;
        this.projetRepository = projetRepository;
        this.ficheDePaieRepository = ficheDePaieRepository;
        this.affectationRepository = affectationRepository;
    }

    // Méthodes helper pour vérifier les rôles
    private boolean isAdmin(Authentication auth) {
        if (auth == null)
            return false;
        return auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private boolean isChefDept(Authentication auth) {
        if (auth == null)
            return false;
        return auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_CHEF_DEPT"));
    }

    // Page d'accueil vierge
    @GetMapping("/home")
    public String home(Model model) {
        model.addAttribute("showEmployees", false);
        model.addAttribute("showDashboard", false);
        model.addAttribute("showStatistics", false);
        model.addAttribute("showDepartements", false);
        model.addAttribute("showProjets", false);
        model.addAttribute("showFichesPaie", false);
        return "home";
    }

    // Route pour afficher les employés dans la zone de contenu
    @GetMapping("/home/employees")
    public String showEmployees(Model model) {
        model.addAttribute("employees", employeeRepository.findAll());
        model.addAttribute("departements", departementRepository.findAll());
        model.addAttribute("showEmployees", true);
        return "home";
    }

    // Route pour afficher le dashboard
    @GetMapping("/home/dashboard")
    public String showDashboard(Model model) {
        model.addAttribute("showDashboard", true);
        model.addAttribute("employeeCount", employeeRepository.count());
        return "home";
    }

    // Route pour afficher les départements dans la zone de contenu
    @GetMapping("/home/departements")
    public String showDepartements(Model model) {
        model.addAttribute("departements", departementRepository.findAll());
        model.addAttribute("employees", employeeRepository.findAll());
        model.addAttribute("showDepartements", true);
        return "home";
    }

    // Route pour afficher les projets (AVEC FILTRE PAR EMPLOYÉ)
    @GetMapping("/home/projets")
    public String showProjets(Model model, Authentication auth) {
        List<Projet> projets;

        if (auth != null && auth.isAuthenticated()) {
            String email = auth.getName();
            Optional<Employee> currentEmployee = employeeRepository.findByEmail(email);

            if (currentEmployee.isPresent()) {
                // Si ADMIN ou CHEF_DEPT : voir tous les projets
                if (isAdmin(auth) || isChefDept(auth)) {
                    projets = projetRepository.findAll();
                } else {
                    // Sinon : voir uniquement les projets auxquels l'employé est affecté
                    projets = projetRepository.findProjetsByEmployeeId(currentEmployee.get().getId());
                }
            } else {
                projets = new ArrayList<>();
            }
        } else {
            projets = new ArrayList<>();
        }

        model.addAttribute("projets", projets);
        model.addAttribute("employees", employeeRepository.findAll());
        model.addAttribute("departements", departementRepository.findAll());
        model.addAttribute("showProjets", true);
        return "home";
    }

    // Route pour afficher les fiches de paie dans la zone de contenu
    @GetMapping("/home/fiches-paie")
    public String showFichesPaie(Model model, Authentication auth) {
        List<com.example.CY_RH_Springboot.models.FicheDePaie> fichesPaie;
        List<Employee> employees = employeeRepository.findAll();

        boolean isAdmin = isAdmin(auth);
        boolean isChefDept = isChefDept(auth);

        if (isAdmin) {
            // Admin voit toutes les fiches
            fichesPaie = ficheDePaieRepository.findAll();
        } else if (isChefDept) {
            // Chef de département voit les fiches de son département ET ses propres fiches
            String email = auth.getName();
            Optional<Employee> currentUser = employeeRepository.findByEmail(email);

            if (currentUser.isPresent()) {
                Long currentUserId = currentUser.get().getId();
                Integer deptId = currentUser.get().getIdDepartement();

                List<Long> employeeIds = employees.stream()
                        .filter(e -> {
                            // Inclure le chef lui-même
                            if (e.getId().equals(currentUserId)) {
                                return true;
                            }
                            // Inclure les employés du département (si le chef a un département)
                            if (deptId != null && e.getIdDepartement() != null && e.getIdDepartement().equals(deptId)) {
                                return true;
                            }
                            return false;
                        })
                        .map(e -> e.getId())
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
        model.addAttribute("showFichesPaie", true);
        return "home";
    }

    // Route pour afficher les statistiques
    @GetMapping("/home/statistics")
    public String showStatistics(Model model) {
        // 1. Nombre total d'employés et projets
        long totalEmployees = employeeRepository.count();
        long totalProjets = projetRepository.count();
        long totalDepartements = departementRepository.count();

        // 2. Nombre d'employés par département
        List<Employee> allEmployees = employeeRepository.findAll();
        List<Departement> allDepartements = departementRepository.findAll();

        Map<String, Long> employeesPerDepartment = allEmployees.stream()
                .filter(e -> e.getIdDepartement() != null)
                .collect(Collectors.groupingBy(
                        e -> allDepartements.stream()
                                .filter(d -> d.getId().equals(e.getIdDepartement()))
                                .findFirst()
                                .map(Departement::getIntitule)
                                .orElse("Sans département"),
                        Collectors.counting()));

        // 3. Nombre d'employés par grade
        Map<String, Long> employeesPerGrade = allEmployees.stream()
                .filter(e -> e.getGrade() != null && !e.getGrade().isEmpty())
                .collect(Collectors.groupingBy(Employee::getGrade, Collectors.counting()));

        // 4. Statistiques des projets
        List<Projet> allProjets = projetRepository.findAll();
        Map<String, Long> projetsPerEtat = allProjets.stream()
                .collect(Collectors.groupingBy(Projet::getEtatProjet, Collectors.counting()));

        // 5. Nombre d'employés par projet (via affectations)
        Map<String, Long> employeesPerProjet = new LinkedHashMap<>();
        for (Projet projet : allProjets) {
            long count = affectationRepository.findByIdProjet(projet.getId()).stream()
                    .filter(a -> a.getDateFinAffectation() == null) // Seulement les affectations actives
                    .count();
            employeesPerProjet.put(projet.getNomProjet(), count);
        }

        model.addAttribute("showStatistics", true);
        model.addAttribute("totalEmployees", totalEmployees);
        model.addAttribute("totalProjets", totalProjets);
        model.addAttribute("totalDepartements", totalDepartements);
        model.addAttribute("employeesPerDepartment", employeesPerDepartment);
        model.addAttribute("employeesPerGrade", employeesPerGrade);
        model.addAttribute("projetsPerEtat", projetsPerEtat);
        model.addAttribute("employeesPerProjet", employeesPerProjet);

        return "home";
    }
}