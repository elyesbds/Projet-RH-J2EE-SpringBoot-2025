package com.example.CY_RH_Springboot.controllers;

import com.example.CY_RH_Springboot.models.Departement;
import com.example.CY_RH_Springboot.models.Employee;
import com.example.CY_RH_Springboot.repositories.DepartementRepository;
import com.example.CY_RH_Springboot.repositories.EmployeeRepository;

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

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.http.*;

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

    // Vérifier si ADMIN
    private boolean isAdmin(Authentication auth) {
        return auth != null && auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    // Vérifier si CHEF_DEPT
    private boolean isChefDept(Authentication auth) {
        return auth != null && auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_CHEF_DEPT"));
    }

    // Vérifier si chef du département
    private boolean isChefOfDepartement(Authentication auth, Departement departement) {
        if (isAdmin(auth)) return true;
        if (!isChefDept(auth)) return false;

        String email = auth.getName();
        Optional<Employee> currentUser = employeeRepository.findByEmail(email);

        return currentUser.isPresent()
                && departement.getChefDepartement() != null
                && currentUser.get().getId().equals(departement.getChefDepartement().longValue());
    }

    // Liste des départements
    @GetMapping
    public String listDepartements(Model model) {
        model.addAttribute("departements", departementRepository.findAll());
        model.addAttribute("employees", employeeRepository.findAll());
        return "departements/departements";
    }

    // Formulaire ajout
    @GetMapping("/add")
    public String showAddForm(Model model, Authentication auth, RedirectAttributes ra) {
        if (!isAdmin(auth)) {
            ra.addFlashAttribute("errorMessage", "Seul un administrateur peut ajouter un département");
            return "redirect:/departements";
        }

        model.addAttribute("departement", new Departement());
        List<Employee> employesSansDepartement =
                employeeRepository.findByIdDepartementIsNull();

        model.addAttribute("employees", employesSansDepartement);
        return "departements/departement_form";
    }

    // Sauvegarde
    @PostMapping("/save")
    public String saveDepartement(
            @Valid @ModelAttribute Departement departement,
            BindingResult result,
            Authentication auth,
            Model model,
            RedirectAttributes ra
    ) {

        // Vérification des permissions
        if (departement.getId() != null) {
            Optional<Departement> existing = departementRepository.findById(departement.getId());
            if (existing.isPresent() && !isChefOfDepartement(auth, existing.get())) {
                ra.addFlashAttribute("errorMessage", "Vous n'avez pas la permission de modifier ce département");
                return "redirect:/departements";
            }
        } else if (!isAdmin(auth)) {
            ra.addFlashAttribute("errorMessage", "Seul un administrateur peut créer un département");
            return "redirect:/departements";
        }

        // === RÈGLE 1 : Vérifier unicité de l’intitulé ===
        Optional<Departement> deptWithSameName =
                departementRepository.findByIntitule(departement.getIntitule());

        if (deptWithSameName.isPresent()
                && !deptWithSameName.get().getId().equals(departement.getId())) {

            result.rejectValue("intitule", "duplicate", "Cet intitulé existe déjà");
        }

        // === RÈGLE 2 : Vérifier qu’un employé n’est chef que d’un seul département ===
        if (departement.getChefDepartement() != null) {
            List<Departement> all = departementRepository.findAll();
            boolean employeeIsAlreadyChef = all.stream()
                    .anyMatch(d -> d.getChefDepartement() != null
                            && d.getChefDepartement().equals(departement.getChefDepartement())
                            && !d.getId().equals(departement.getId()));

            if (employeeIsAlreadyChef) {
                result.rejectValue("chefDepartement", "duplicateChef",
                        "Cet employé est déjà chef d'un autre département");
            }
        }

        // Si erreur → retour au formulaire
        if (result.hasErrors()) {
            model.addAttribute("employees", employeeRepository.findAll());
            return "departements/departement_form";
        }

        // Sauvegarde
        departementRepository.save(departement);
        // === MISE À JOUR DU RÔLE DU CHEF DE DÉPARTEMENT ===
        employeeRepository.findAll().forEach(emp -> {
            // Si l'employé était chef mais ne l'est plus → rôle normal
            if (emp.getRole().equals("CHEF_DEPT")
                    && (departement.getChefDepartement() == null
                    || !emp.getId().equals(departement.getChefDepartement().longValue()))) {
                emp.setRole("EMPLOYE");
                employeeRepository.save(emp);
            }
        });

// Si un chef est défini, lui attribuer le bon rôle
        if (departement.getChefDepartement() != null) {
            employeeRepository.findById(departement.getChefDepartement().longValue()).ifPresent(emp -> {
                emp.setRole("CHEF_DEPT");
                employeeRepository.save(emp);
            });
        }
        ra.addFlashAttribute("successMessage", "Département enregistré avec succès");
        return "redirect:/departements";
    }

    // Formulaire modification
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Integer id,
                               Model model,
                               Authentication auth,
                               RedirectAttributes ra) {

        Optional<Departement> departement = departementRepository.findById(id);
        if (departement.isEmpty()) {
            ra.addFlashAttribute("errorMessage", "Département introuvable");
            return "redirect:/departements";
        }

        if (!isChefOfDepartement(auth, departement.get())) {
            ra.addFlashAttribute("errorMessage", "Vous n'avez pas la permission de modifier ce département");
            return "redirect:/departements";
        }

        model.addAttribute("departement", departement.get());
        List<Employee> employesDuDepartement =
                employeeRepository.findByIdDepartement(id);

        model.addAttribute("employees", employesDuDepartement);
        return "departements/departement_form";
    }

    // Suppression
    @GetMapping("/delete/{id}")
    public String deleteDepartement(@PathVariable Integer id,
                                    Authentication auth,
                                    RedirectAttributes ra) {

        Optional<Departement> departement = departementRepository.findById(id);
        if (departement.isEmpty()) {
            ra.addFlashAttribute("errorMessage", "Département introuvable");
            return "redirect:/departements";
        }

        if (!isChefOfDepartement(auth, departement.get())) {
            ra.addFlashAttribute("errorMessage", "Vous n'avez pas la permission de supprimer ce département");
            return "redirect:/departements";
        }

        departementRepository.delete(departement.get());
        ra.addFlashAttribute("successMessage", "Département supprimé avec succès");
        return "redirect:/departements";
    }

    // Export PDF (inchangé)
    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportDepartementsToPDF() throws IOException {

        List<Departement> departements = departementRepository.findAll();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);

        try {
            PdfWriter.getInstance(document, bos);
            document.open();

            Font fontTitre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
            Paragraph pTitre = new Paragraph("Rapport: Liste des Départements au " + LocalDate.now(), fontTitre);
            pTitre.setAlignment(Paragraph.ALIGN_CENTER);

            document.add(pTitre);
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(80);
            table.setWidths(new float[]{1f, 3f, 3f});

            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            String[] headers = {"ID", "Intitulé", "Chef de Département"};

            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                cell.setBackgroundColor(new Color(153, 204, 255));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(5);
                table.addCell(cell);
            }

            Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            for (Departement dept : departements) {
                table.addCell(new Phrase(dept.getId().toString(), dataFont));
                table.addCell(new Phrase(dept.getIntitule(), dataFont));

                String chefName = "-";
                if (dept.getChefDepartement() != null) {
                    employeeRepository.findById(dept.getChefDepartement().longValue())
                            .ifPresent(emp ->
                                    table.addCell(new Phrase(emp.getPrenom() + " " + emp.getNom(), dataFont)));
                } else {
                    table.addCell(new Phrase("-", dataFont));
                }
            }

            document.add(table);
            document.close();

        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "rapport_departements_" + LocalDate.now() + ".pdf");

        return ResponseEntity.ok().headers(headers).body(bos.toByteArray());
    }
}
