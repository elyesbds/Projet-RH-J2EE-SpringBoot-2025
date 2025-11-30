package com.example.CY_RH_Springboot.controllers;

import com.example.CY_RH_Springboot.models.Projet;
import com.example.CY_RH_Springboot.models.Employee;
import com.example.CY_RH_Springboot.models.Departement;
import com.example.CY_RH_Springboot.repositories.ProjetRepository;
import com.example.CY_RH_Springboot.repositories.EmployeeRepository;
import com.example.CY_RH_Springboot.repositories.DepartementRepository;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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

    // Méthodes helper pour les permissions
    private boolean isAdmin(Authentication auth) {
        if (auth == null) return false;
        return auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private boolean isChefDept(Authentication auth) {
        if (auth == null) return false;
        return auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_CHEF_DEPT"));
    }

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

    // === LISTE (AVEC FILTRE PAR EMPLOYÉ) ===
    @GetMapping
    public String listProjets(Model model, Authentication auth) {

        List<Projet> projets;
        List<Employee> employees = employeeRepository.findAll();
        List<Departement> departements = departementRepository.findAll();

        // Seul ADMIN voit tous les projets
        if (isAdmin(auth)) {
            projets = projetRepository.findAll();
        } else {
            // Tous les autres employés ne voient que leurs projets affectés
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

        // Vérification autorisations
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

        // VALIDATIONS MÉTIER

        // Date fin prévue >= date début
        if (projet.getDateFinPrevue() != null &&
                projet.getDateFinPrevue().isBefore(projet.getDateDebut())) {
            bindingResult.rejectValue("dateFinPrevue", "error.projet",
                    "La date de fin prévue doit être après la date de début");
        }

        // Date fin réelle >= date début
        if (projet.getDateFinReelle() != null &&
                projet.getDateFinReelle().isBefore(projet.getDateDebut())) {
            bindingResult.rejectValue("dateFinReelle", "error.projet",
                    "La date de fin réelle doit être après la date de début");
        }

        // Si projet TERMINE alors date fin réelle obligatoire
        if (projet.getEtatProjet().equals("TERMINE") && projet.getDateFinReelle() == null) {
            bindingResult.rejectValue("dateFinReelle", "error.projet",
                    "Vous devez renseigner la date de fin réelle pour un projet terminé");
        }

        // Chef de projet doit exister (si renseigné)
        if (projet.getChefProjet() != null &&
                !employeeRepository.existsById(projet.getChefProjet().longValue())) {
            bindingResult.rejectValue("chefProjet", "error.projet", "Employé introuvable");
        }

        // Département doit exister (si renseigné)
        if (projet.getIdDepartement() != null &&
                !departementRepository.existsById((int) projet.getIdDepartement().longValue())) {
            bindingResult.rejectValue("idDepartement", "error.projet", "Département introuvable");
        }

        // Si erreurs → on renvoie au formulaire
        if (bindingResult.hasErrors()) {
            model.addAttribute("employees", employeeRepository.findAll());
            model.addAttribute("departements", departementRepository.findAll());
            return "projets/projet_form";
        }

        // Sauvegarde
        projetRepository.save(projet);

        // Mise à jour du rôle chef de projet
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

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportProjetsToPDF() throws IOException {
        List<Projet> projets = projetRepository.findAll();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate());
        try {
            PdfWriter.getInstance(document, bos);
            document.open();
            Font fontTitre = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
            fontTitre.setSize(20);
            Paragraph pTitre = new Paragraph("Rapport: Liste des Projets en cours au " + LocalDate.now(), fontTitre);
            pTitre.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(pTitre);
            document.add(new Paragraph(" "));
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[] {1f, 4f, 2f, 2f, 2f});
            table.setSpacingBefore(10);
            Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
            fontHeader.setSize(12);
            String[] tableHeaders = {"ID", "Nom du Projet", "Date Début", "Date Fin Prévue", "Statut"};
            for (String header : tableHeaders) {
                PdfPCell cell = new PdfPCell(new Phrase(header, fontHeader));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBackgroundColor(new Color(255, 223, 186));
                cell.setPadding(5);
                table.addCell(cell);
            }
            Font fontData = FontFactory.getFont(FontFactory.HELVETICA, 10);
            for (Projet projet : projets) {
                table.addCell(new Phrase(projet.getId().toString(), fontData));
                table.addCell(new Phrase(projet.getNomProjet(), fontData));
                table.addCell(new Phrase(projet.getDateDebut().format(dateFormatter), fontData));
                table.addCell(new Phrase(projet.getDateFinPrevue().format(dateFormatter), fontData));
                table.addCell(new Phrase(projet.getEtatProjet(), fontData));
            }
            document.add(table);
            document.close();
        } catch (DocumentException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        byte[] pdfBytes = bos.toByteArray();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        String filename = "rapport_projets_" + LocalDate.now() + ".pdf";
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(pdfBytes.length);
        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }
}