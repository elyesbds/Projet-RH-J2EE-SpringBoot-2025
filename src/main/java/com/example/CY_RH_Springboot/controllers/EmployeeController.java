package com.example.CY_RH_Springboot.controllers;

import com.example.CY_RH_Springboot.models.Employee;
import com.example.CY_RH_Springboot.repositories.EmployeeRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
import java.util.List;
import java.util.Optional;

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

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportEmployeesToPDF() throws IOException {

        // 1. Récupération des données via votre repository
        List<Employee> employees = employeeRepository.findAll();

        // Création d'un flux de sortie en mémoire pour construire le PDF
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        // 2. Configuration et écriture du document PDF
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, bos); // Écriture dans le flux en mémoire
        document.open();

        // --- Contenu du PDF (Identique à la version précédente) ---

        // Titre du Document
        Font fontTitre = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
        fontTitre.setSize(20);
        Paragraph p = new Paragraph("Rapport: Liste des Employés au " + LocalDate.now(), fontTitre);
        p.setAlignment(Paragraph.ALIGN_CENTER);
        document.add(p);
        document.add(new Paragraph(" "));

        // Création du Tableau (6 colonnes)
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[] {2.5f, 2.5f, 4f, 2.5f, 3f, 2f});
        table.setSpacingBefore(10);

        // Entêtes du Tableau
        Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
        String[] tableHeaders = {"Nom", "Prénom", "Email", "Téléphone", "Poste", "Grade"};
        for (String header : tableHeaders) {
            PdfPCell cell = new PdfPCell(new Phrase(header, fontHeader));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBackgroundColor(new Color(200, 200, 200));
            table.addCell(cell);
        }

        // Remplissage des Données
        Font fontData = FontFactory.getFont(FontFactory.HELVETICA, 10);
        for (Employee employee : employees) {
            // NOTE : J'utilise ici les getters par convention (getNom(), getPrenom(), etc.)
            table.addCell(new Phrase(employee.getNom(), fontData));
            table.addCell(new Phrase(employee.getPrenom(), fontData));
            table.addCell(new Phrase(employee.getEmail(), fontData));
            table.addCell(new Phrase(employee.getTelephone(), fontData));
            table.addCell(new Phrase(employee.getPoste(), fontData));
            table.addCell(new Phrase(employee.getGrade(), fontData));
        }

        document.add(table);
        document.close();

        // 3. Préparation du ResponseEntity
        byte[] pdfBytes = bos.toByteArray();

        HttpHeaders headers = new HttpHeaders();
        // Indique que le contenu est un PDF
        headers.setContentType(MediaType.APPLICATION_PDF);
        // Force le navigateur à télécharger le fichier avec un nom spécifique
        String filename = "rapport_employes_" + LocalDate.now() + ".pdf";
        headers.setContentDispositionFormData("attachment", filename);
        // Indique la taille du fichier
        headers.setContentLength(pdfBytes.length);

        // Renvoie les bytes du PDF avec les en-têtes HTTP appropriés
        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

}