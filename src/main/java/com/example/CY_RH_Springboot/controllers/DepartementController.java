package com.example.CY_RH_Springboot.controllers;

import com.example.CY_RH_Springboot.models.Departement;
import com.example.CY_RH_Springboot.models.Employee;
import com.example.CY_RH_Springboot.repositories.DepartementRepository;
import com.example.CY_RH_Springboot.repositories.EmployeeRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;

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

    // Vérifier si l'utilisateur est chef de CE département
    private boolean isChefOfDepartement(Authentication auth, Departement departement) {
        if (isAdmin(auth)) return true;
        if (!isChefDept(auth)) return false;

        String email = auth.getName();
        Optional<Employee> currentUser = employeeRepository.findByEmail(email);

        if (currentUser.isPresent() && departement.getChefDepartement() != null) {
            return currentUser.get().getId().equals(departement.getChefDepartement().longValue());
        }

        return false;
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
    public String showAddForm(Model model, Authentication auth, RedirectAttributes redirectAttributes) {
        if (!isAdmin(auth)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Seul un administrateur peut ajouter un département");
            return "redirect:/departements";
        }

        List<Employee> employees = employeeRepository.findAll();

        model.addAttribute("departement", new Departement());
        model.addAttribute("employees", employees);
        return "departements/departement_form";
    }

    // Sauvegarde un département (ajout ou modification)
    @PostMapping("/save")
    public String saveDepartement(@ModelAttribute Departement departement,
                                  Authentication auth,
                                  RedirectAttributes redirectAttributes) {
        // Vérification pour modification
        if (departement.getId() != null) {
            Optional<Departement> existingDept = departementRepository.findById(departement.getId());
            if (existingDept.isPresent() && !isChefOfDepartement(auth, existingDept.get())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Vous n'avez pas la permission de modifier ce département");
                return "redirect:/departements";
            }
        } else {
            // Pour la création, seul l'admin peut
            if (!isAdmin(auth)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Seul un administrateur peut créer un département");
                return "redirect:/departements";
            }
        }

        departementRepository.save(departement);
        redirectAttributes.addFlashAttribute("successMessage", "Département enregistré avec succès");
        return "redirect:/departements";
    }

    // Affiche le formulaire pour modifier un département
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Integer id, Model model, Authentication auth, RedirectAttributes redirectAttributes) {
        Optional<Departement> departement = departementRepository.findById(id);

        if (departement.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Département introuvable");
            return "redirect:/departements";
        }

        if (!isChefOfDepartement(auth, departement.get())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vous n'avez pas la permission de modifier ce département");
            return "redirect:/departements";
        }

        List<Employee> employees = employeeRepository.findAll();

        model.addAttribute("departement", departement.get());
        model.addAttribute("employees", employees);
        return "departements/departement_form";
    }

    // Supprimer un département
    @GetMapping("/delete/{id}")
    public String deleteDepartement(@PathVariable Integer id, Authentication auth, RedirectAttributes redirectAttributes) {
        Optional<Departement> departement = departementRepository.findById(id);

        if (departement.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Département introuvable");
            return "redirect:/departements";
        }

        if (!isChefOfDepartement(auth, departement.get())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vous n'avez pas la permission de supprimer ce département");
            return "redirect:/departements";
        }

        departementRepository.delete(departement.get());
        redirectAttributes.addFlashAttribute("successMessage", "Département supprimé avec succès");
        return "redirect:/departements";
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportDepartementsToPDF() throws IOException {

        // 1. Récupération des données
        List<Departement> departements = departementRepository.findAll();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);

        // Initialisation du writer dans un bloc try-catch
        try {
            PdfWriter.getInstance(document, bos);
            document.open();

            // --- Titre du Document ---
            Font fontTitre = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
            fontTitre.setSize(20);
            Paragraph pTitre = new Paragraph("Rapport: Liste des Départements au " + LocalDate.now(), fontTitre);
            pTitre.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(pTitre);
            document.add(new Paragraph(" "));

            // --- Tableau des Départements ---

            // 3 colonnes : ID, Intitulé, Chef de Département
            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(80);
            table.setWidths(new float[] {1f, 3f, 3f});
            table.setSpacingBefore(10);

            // Entêtes du Tableau
            Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
            fontHeader.setSize(12);
            String[] tableHeaders = {"ID", "Intitulé", "Chef de Département"};

            for (String header : tableHeaders) {
                PdfPCell cell = new PdfPCell(new Phrase(header, fontHeader));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBackgroundColor(new Color(153, 204, 255)); // Bleu clair pour le département
                cell.setPadding(5);
                table.addCell(cell);
            }

            // Remplissage des Données
            Font fontData = FontFactory.getFont(FontFactory.HELVETICA, 10);
            for (Departement dept : departements) {
                // Ligne 1: ID
                table.addCell(new Phrase(dept.getId().toString(), fontData));

                // Ligne 2: Intitulé
                table.addCell(new Phrase(dept.getIntitule(), fontData));

                // Ligne 3: Chef de Département
                String chefName;
                if (dept.getChefDepartement() != null) {
                    // Recherche du nom complet de l'employé (Chef)
                    Optional<Employee> chefOpt = employeeRepository.findById(dept.getChefDepartement().longValue());
                    chefName = chefOpt.map(e -> e.getPrenom() + " " + e.getNom()).orElse("Non assigné");
                } else {
                    chefName = "Non assigné";
                }
                table.addCell(new Phrase(chefName, fontData));
            }

            document.add(table);
            document.close();

        } catch (DocumentException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // 2. Préparation du ResponseEntity
        byte[] pdfBytes = bos.toByteArray();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        String filename = "rapport_departements_" + LocalDate.now() + ".pdf";
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(pdfBytes.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
}