package com.example.CY_RH_Springboot.controllers;

import com.example.CY_RH_Springboot.models.FicheDePaie;
import com.example.CY_RH_Springboot.models.Employee;
import com.example.CY_RH_Springboot.repositories.FicheDePaieRepository;
import com.example.CY_RH_Springboot.repositories.EmployeeRepository;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
import java.text.DecimalFormat;
import java.util.Locale;
import java.time.Month;

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
            // Chef de département voit les fiches de son département ET ses propres fiches
            String email = auth.getName();
            Optional<Employee> currentUser = employeeRepository.findByEmail(email);

            if (currentUser.isPresent()) {
                Integer currentUserId = currentUser.get().getId().intValue();
                Integer deptId = currentUser.get().getIdDepartement();

                List<Integer> employeeIds = employees.stream()
                        .filter(e -> {
                            // Inclure le chef lui-même
                            if (e.getId().intValue() == currentUserId) {
                                return true;
                            }
                            // Inclure les employés du département (si le chef a un département)
                            if (deptId != null && e.getIdDepartement() != null && e.getIdDepartement().equals(deptId)) {
                                return true;
                            }
                            return false;
                        })
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
    public String saveFichePaie(
            @Valid @ModelAttribute("fichePaie") FicheDePaie fichePaie,
            BindingResult bindingResult,
            Model model,
            Authentication auth,
            RedirectAttributes redirectAttributes
    ) {

        // === Vérification permissions modification ===
        if (fichePaie.getId() != null) {
            Optional<FicheDePaie> existing = ficheDePaieRepository.findById(fichePaie.getId());
            if (existing.isPresent() && !canModifyFichePaie(auth, existing.get())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Vous n'avez pas la permission de modifier cette fiche de paie");
                return "redirect:/fiches-paie";
            }
        } else {
            // Création
            if (!isAdmin(auth) && !isChefDept(auth)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Vous n'avez pas la permission de créer une fiche de paie");
                return "redirect:/fiches-paie";
            }
        }

        if (fichePaie.getSalaireBase() == null) {
            bindingResult.rejectValue("salaireBase", "error.salaireBase", "Le salaire de base est obligatoire.");
        }

        if (fichePaie.getMois() == null) {
            bindingResult.rejectValue("mois", "error.mois", "Vous devez choisir un mois.");
        }

        if (fichePaie.getAnnee() == null) {
            bindingResult.rejectValue("annee", "error.annee", "L'année est obligatoire.");
        }

        if (fichePaie.getDateGeneration() == null) {
            bindingResult.rejectValue("dateGeneration", "error.dateGeneration", "Veuillez choisir une date.");
        }

        if (fichePaie.getPrimes() == null) {
            fichePaie.setPrimes(BigDecimal.ZERO);
        }

        if (fichePaie.getDeductions() == null) {
            fichePaie.setDeductions(BigDecimal.ZERO);
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("employees", employeeRepository.findAll());
            return "fiches-paie/fiche_paie_form";
        }

        BigDecimal net = fichePaie.getSalaireBase()
                .add(fichePaie.getPrimes())
                .subtract(fichePaie.getDeductions());

        fichePaie.setNetAPayer(net);

        if (fichePaie.getId() == null) {
            fichePaie.setDateGeneration(LocalDate.now());
        }

        // === Sauvegarde ===
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

    @GetMapping("/export/{id}/pdf")
    public ResponseEntity<byte[]> exportFicheDePaieToPDF(@PathVariable Integer id) throws IOException {

        // 1. Récupération des données
        Optional<FicheDePaie> fichePaieOpt = ficheDePaieRepository.findById(id);
        if (fichePaieOpt.isEmpty()) {
            // Fiche de paie non trouvée, renvoie une erreur 404
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        FicheDePaie fichePaie = fichePaieOpt.get();

        // Récupération de l'employé associé
        Optional<Employee> employeeOpt = employeeRepository.findById(fichePaie.getIdEmployer());
        Employee employee = employeeOpt.orElse(null);

        // Déclaration de 'periode' déplacée ici (CORRECTION DE LA PORTÉE)
        String nomMois = Month.of(fichePaie.getMois()).getDisplayName(java.time.format.TextStyle.FULL_STANDALONE, Locale.FRANCE);
        final String periode = nomMois.toUpperCase() + " " + fichePaie.getAnnee();

        // Outil de formatage monétaire (ex: 1 234.56 €)
        DecimalFormat currencyFormat = new DecimalFormat("#,##0.00 €", new java.text.DecimalFormatSymbols(Locale.FRANCE));

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);

        try {
            PdfWriter.getInstance(document, bos);
            document.open();

            // --- Titre et Informations d'Entête ---

            // TITRE PRINCIPAL
            Font fontTitre = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
            fontTitre.setSize(22);
            fontTitre.setColor(new Color(60, 60, 160)); // Bleu foncé
            Paragraph pTitre = new Paragraph("FICHE DE PAIE", fontTitre);
            pTitre.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(pTitre);

            // Période concernée (utilise la variable 'periode' déclarée au-dessus)
            Font fontPeriode = FontFactory.getFont(FontFactory.HELVETICA);
            fontPeriode.setSize(14);
            Paragraph pPeriode = new Paragraph("Période : " + periode, fontPeriode);
            pPeriode.setAlignment(Paragraph.ALIGN_CENTER);
            pPeriode.setSpacingAfter(20);
            document.add(pPeriode);


            // --- BLOC INFORMATION EMPLOYE / EMPLOYEUR ---

            // Table pour diviser l'espace : 2 colonnes, 50% chacune
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setWidths(new float[]{50, 50});
            infoTable.setSpacingAfter(20);

            // Colonne Gauche (Employeur - Simplifié)
            PdfPCell employeurCell = new PdfPCell();
            employeurCell.setBorder(Rectangle.BOX);
            employeurCell.setBorderWidth(1.5f);
            employeurCell.setBackgroundColor(new Color(230, 230, 255)); // Bleu clair
            employeurCell.setPadding(10);
            employeurCell.addElement(new Paragraph("Employeur : CY-RH Project", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
            employeurCell.addElement(new Paragraph("123 Rue de l'Exemple, 75000 Paris", FontFactory.getFont(FontFactory.HELVETICA, 10)));
            infoTable.addCell(employeurCell);

            // Colonne Droite (Employé)
            PdfPCell employeCell = new PdfPCell();
            employeCell.setBorder(Rectangle.BOX);
            employeCell.setBorderWidth(1.5f);
            employeCell.setPadding(10);

            if (employee != null) {
                employeCell.addElement(new Paragraph("Employé : " + employee.getPrenom() + " " + employee.getNom().toUpperCase(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
                employeCell.addElement(new Paragraph("Poste : " + employee.getPoste(), FontFactory.getFont(FontFactory.HELVETICA, 10)));
                // ATTENTION : J'utilise getMatricule(), assurez-vous que cette méthode existe dans Employee.java
                employeCell.addElement(new Paragraph("Matricule : " + employee.getMatricule(), FontFactory.getFont(FontFactory.HELVETICA, 10)));
            } else {
                employeCell.addElement(new Paragraph("Employé : Données non disponibles (ID: " + fichePaie.getIdEmployer() + ")", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
            }
            infoTable.addCell(employeCell);

            document.add(infoTable);


            // --- TABLEAU DES ÉLÉMENTS DE PAIE ---

            document.add(new Paragraph("Détails des Éléments de Paie :", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
            document.add(new Paragraph(" "));

            // Tableau de 3 colonnes : Intitulé, Montant, Type
            PdfPTable paieTable = new PdfPTable(3);
            paieTable.setWidthPercentage(100);
            paieTable.setWidths(new float[]{5, 2, 3});

            // Entêtes
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            headerFont.setColor(Color.WHITE);
            Color headerColor = new Color(70, 70, 70); // Gris foncé

            String[] paieHeaders = {"Intitulé", "Montant", "Type"};
            for (String header : paieHeaders) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBackgroundColor(headerColor);
                cell.setPadding(5);
                paieTable.addCell(cell);
            }

            // Ligne Salaire de Base
            Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            paieTable.addCell(new Phrase("Salaire de Base", dataFont));
            paieTable.addCell(new Phrase(currencyFormat.format(fichePaie.getSalaireBase()), dataFont));
            paieTable.addCell(new Phrase("Gain", dataFont));

            // Ligne Primes
            paieTable.addCell(new Phrase("Primes (Exceptionnel ou Objectifs)", dataFont));
            paieTable.addCell(new Phrase(currencyFormat.format(fichePaie.getPrimes()), dataFont));
            paieTable.addCell(new Phrase("Gain", dataFont));

            // Ligne Déductions
            paieTable.addCell(new Phrase("Cotisations / Taxes (Simplifié)", dataFont));
            paieTable.addCell(new Phrase("-" + currencyFormat.format(fichePaie.getDeductions()), dataFont));
            paieTable.addCell(new Phrase("Retenue", dataFont));

            document.add(paieTable);


            // --- RÉCAPITULATIF (NET À PAYER) ---

            document.add(new Paragraph(" "));

            PdfPTable netTable = new PdfPTable(2);
            netTable.setWidthPercentage(100);
            netTable.setWidths(new float[]{7, 3});
            netTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

            // Cellule vide pour aligner à droite le net à payer
            PdfPCell emptyCell = new PdfPCell(new Phrase(" ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
            emptyCell.setBorder(Rectangle.NO_BORDER);
            netTable.addCell(emptyCell);

            // Cellule Net à Payer
            PdfPCell netCell = new PdfPCell(new Phrase("NET À PAYER : " + currencyFormat.format(fichePaie.getNetAPayer()), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
            netCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            netCell.setBackgroundColor(new Color(180, 255, 180)); // Vert clair
            netCell.setPadding(8);
            netTable.addCell(netCell);

            document.add(netTable);

            // --- Date de génération
            Paragraph pGenerated = new Paragraph("Fiche générée automatiquement le : " + LocalDate.now(), FontFactory.getFont(FontFactory.HELVETICA, 8));
            pGenerated.setAlignment(Paragraph.ALIGN_RIGHT);
            pGenerated.setSpacingBefore(10);
            document.add(pGenerated);

            document.close();

        } catch (DocumentException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // 3. Préparation du ResponseEntity
        byte[] pdfBytes = bos.toByteArray();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        // 'periode' est reconnu ici.
        String filename = "fiche_paie_" + periode.replace(" ", "_") + ".pdf";
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(pdfBytes.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
}