package com.example.CY_RH_Springboot.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "employer")
public class Employee {

    public interface OnCreate {}
    public interface OnUpdate {}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le matricule est obligatoire", groups = {OnCreate.class, OnUpdate.class})
    @Size(min = 3, max = 30, message = "Le matricule doit contenir entre 3 et 30 caractères", groups = {OnCreate.class, OnUpdate.class})
    @Pattern(regexp = "^[A-Z0-9]+$", message = "Le matricule ne doit contenir que des lettres majuscules et chiffres", groups = {OnCreate.class, OnUpdate.class})
    @Column(name = "Matricule")
    private String matricule;

    @NotBlank(message = "Le nom de l'employé est obligatoire", groups = {OnCreate.class, OnUpdate.class})
    @Pattern(regexp = "^[\\p{L} .'-]+$", message = "Le nom ne doit contenir que des lettres", groups = {OnCreate.class, OnUpdate.class})
    @Size(min = 2, max = 50, message = "Le nom doit contenir entre 2 et 50 caractères", groups = {OnCreate.class, OnUpdate.class})
    @Column(name = "Nom")
    private String nom;

    @NotBlank(message = "Le prénom de l'employé est obligatoire", groups = {OnCreate.class, OnUpdate.class})
    @Pattern(regexp = "^[\\p{L} .'-]+$", message = "Le prénom ne doit contenir que des lettres", groups = {OnCreate.class, OnUpdate.class})
    @Size(min = 2, max = 50, message = "Le prénom doit contenir entre 2 et 50 caractères", groups = {OnCreate.class, OnUpdate.class})
    @Column(name = "Prenom")
    private String prenom;

    @NotBlank(message = "L'email est obligatoire", groups = {OnCreate.class, OnUpdate.class})
    @Email(message = "L'email doit être valide", groups = {OnCreate.class, OnUpdate.class})
    @Size(max = 100, message = "L'email ne doit pas dépasser 100 caractères", groups = {OnCreate.class, OnUpdate.class})
    @Column(name = "Email")
    private String email;

    @Pattern(regexp = "^(?:(?:\\+|00)33|0)\\s*[1-9](?:[\\s.-]*\\d{2}){4}$",
            message = "Le numéro de téléphone doit être au format français valide",
            groups = {OnCreate.class, OnUpdate.class})
    @Column(name = "Telephone")
    private String telephone;

    @Size(min = 6, max = 255, message = "Le mot de passe doit contenir au moins 6 caractères", groups = OnCreate.class)
    @Column(name = "Password")
    private String password;

    @NotBlank(message = "Le poste est obligatoire", groups = {OnCreate.class, OnUpdate.class})
    @Size(min = 3, max = 100, message = "Le poste doit contenir entre 3 et 100 caractères", groups = {OnCreate.class, OnUpdate.class})
    @Column(name = "Poste")
    private String poste;

    @NotBlank(message = "Le grade est obligatoire", groups = {OnCreate.class, OnUpdate.class})
    @Column(name = "Grade")
    private String grade;

    @NotNull(message = "Le salaire de base est obligatoire", groups = {OnCreate.class, OnUpdate.class})
    @DecimalMin(value = "1000.00", message = "Le salaire de base doit être au minimum 1000€", groups = {OnCreate.class, OnUpdate.class})
    @DecimalMax(value = "1000000.00", message = "Le salaire de base ne peut pas dépasser 1 000 000€", groups = {OnCreate.class, OnUpdate.class})
    @Column(name = "Salaire_base")
    private BigDecimal salaireBase;

    @NotNull(message = "La date d'embauche est obligatoire", groups = {OnCreate.class, OnUpdate.class})
    @PastOrPresent(message = "La date d'embauche ne peut pas être dans le futur", groups = {OnCreate.class, OnUpdate.class})
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "Date_embauche")
    private LocalDate dateEmbauche;

    @Column(name = "Id_departement")
    private Integer idDepartement;

    @Column(name = "Role")
    private String role;

    public Employee() {}

    // Getters et Setters (inchangés)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMatricule() { return matricule; }
    public void setMatricule(String matricule) { this.matricule = matricule; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getPoste() { return poste; }
    public void setPoste(String poste) { this.poste = poste; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public BigDecimal getSalaireBase() { return salaireBase; }
    public void setSalaireBase(BigDecimal salaireBase) { this.salaireBase = salaireBase; }
    public LocalDate getDateEmbauche() { return dateEmbauche; }
    public void setDateEmbauche(LocalDate dateEmbauche) { this.dateEmbauche = dateEmbauche; }
    public Integer getIdDepartement() { return idDepartement; }
    public void setIdDepartement(Integer idDepartement) { this.idDepartement = idDepartement; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
