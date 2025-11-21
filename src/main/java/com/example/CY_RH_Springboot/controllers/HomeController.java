package com.example.CY_RH_Springboot.controllers;

import com.example.CY_RH_Springboot.repositories.DepartementRepository;
import com.example.CY_RH_Springboot.repositories.EmployeeRepository;
import com.example.CY_RH_Springboot.repositories.ProjetRepository;
import com.example.CY_RH_Springboot.repositories.FicheDePaieRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;

@Controller
public class HomeController {

    private final EmployeeRepository employeeRepository;
    private final DepartementRepository departementRepository;
    private final ProjetRepository projetRepository;
    private final FicheDePaieRepository ficheDePaieRepository;

    public HomeController(EmployeeRepository employeeRepository,
                          DepartementRepository departementRepository,
                          ProjetRepository projetRepository,
                          FicheDePaieRepository ficheDePaieRepository) {
        this.employeeRepository = employeeRepository;
        this.departementRepository = departementRepository;
        this.projetRepository = projetRepository;
        this.ficheDePaieRepository = ficheDePaieRepository;
    }

    // Page d'accueil vierge
    @GetMapping("/home")
    public String home(Model model) {
        // Initialiser les variables à false pour éviter les erreurs null
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
        model.addAttribute("showEmployees", true);
        return "home";
    }

    // Route pour afficher le dashboard (exemple)
    @GetMapping("/home/dashboard")
    public String showDashboard(Model model) {
        model.addAttribute("showDashboard", true);
        model.addAttribute("employeeCount", employeeRepository.count());
        return "home";
    }

    // Route pour afficher les statistiques (exemple)
    @GetMapping("/home/statistics")
    public String showStatistics(Model model) {
        model.addAttribute("showStatistics", true);
        return "home";
    }

    // Route pour afficher les départements dans la zone de contenu
    @GetMapping("/home/departements")
    public String showDepartements(Model model) {
        model.addAttribute("departements", departementRepository.findAll());
        model.addAttribute("showDepartements", true);
        return "home";
    }

    // Route pour afficher les projets dans la zone de contenu
    @GetMapping("/home/projets")
    public String showProjets(Model model) {
        model.addAttribute("projets", projetRepository.findAll());
        model.addAttribute("employees", employeeRepository.findAll());
        model.addAttribute("departements", departementRepository.findAll());
        model.addAttribute("showProjets", true);
        return "home";
    }

    // Route pour afficher les fiches de paie dans la zone de contenu
    @GetMapping("/home/fiches-paie")
    public String showFichesPaie(Model model) {
        model.addAttribute("fichesPaie", ficheDePaieRepository.findAll());
        model.addAttribute("employees", employeeRepository.findAll());
        model.addAttribute("showFichesPaie", true);
        return "home";
    }
}