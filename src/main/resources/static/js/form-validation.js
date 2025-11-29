/**
 * Système de validation des formulaires pour Spring Boot + Thymeleaf
 * Validation côté client pour améliorer l'expérience utilisateur
 *
 * IMPORTANT : Cette validation côté client ne remplace pas la validation côté serveur
 * Elle améliore seulement l'UX en donnant un feedback immédiat
 */

class FormValidator {
    constructor(formId) {
        this.form = document.getElementById(formId);
        if (!this.form) {
            console.error(`Formulaire avec id "${formId}" non trouvé`);
            return;
        }
        this.init();
    }

    init() {
        // Validation en temps réel sur les champs
        const inputs = this.form.querySelectorAll('input, select, textarea');
        inputs.forEach(input => {
            // Validation à la perte de focus
            input.addEventListener('blur', (e) => {
                this.validateField(e.target);
            });

            // Suppression de l'erreur lors de la saisie
            input.addEventListener('input', (e) => {
                this.clearFieldError(e.target);
            });
        });

        // Validation à la soumission
        this.form.addEventListener('submit', (e) => {
            if (!this.validateForm()) {
                e.preventDefault();
                this.showGlobalError('Veuillez corriger les erreurs avant de soumettre le formulaire');
            }
        });
    }

    validateForm() {
        let isValid = true;

        // Valider TOUS les champs (pas seulement les required)
        const inputs = this.form.querySelectorAll('input, select, textarea');

        inputs.forEach(input => {
            // Ignorer les champs hidden et les boutons
            if (input.type !== 'hidden' && input.type !== 'submit' && input.type !== 'button') {
                if (!this.validateField(input)) {
                    isValid = false;
                }
            }
        });

        // Validations spécifiques par type de formulaire
        const formType = this.detectFormType();
        if (formType) {
            const specificValidation = this[`validate${formType}`]();
            if (!specificValidation) {
                isValid = false;
            }
        }

        return isValid;
    }

    validateField(field) {
        const value = field.value.trim();
        const fieldName = field.name || field.id;
        let isValid = true;
        let errorMessage = '';

        // Champs requis
        if (field.hasAttribute('required') && !value) {
            errorMessage = 'Ce champ est obligatoire';
            isValid = false;
        }

        // Si le champ est vide et non requis, ne pas valider le reste
        if (!value && !field.hasAttribute('required')) {
            this.clearFieldError(field);
            return true;
        }

        // Email
        if (field.type === 'email' && value) {
            const emailRegex = /^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
            if (!emailRegex.test(value)) {
                errorMessage = 'Format d\'email invalide (ex: prenom.nom@cy-rh.local)';
                isValid = false;
            }
        }

        // Téléphone
        if (field.type === 'tel' && value) {
            const phoneRegex = /^(?:(?:\+|00)33|0)\s*[1-9](?:[\s.-]*\d{2}){4}$/;
            if (!phoneRegex.test(value.replace(/\s/g, ''))) {
                errorMessage = 'Format de téléphone invalide (ex: 0612345678)';
                isValid = false;
            }
        }

        // Nom et Prénom - ne doivent pas contenir que des chiffres
        if ((fieldName.toLowerCase() === 'nom' || fieldName.toLowerCase() === 'prenom') && value) {
            const onlyNumbers = /^\d+$/;
            if (onlyNumbers.test(value)) {
                errorMessage = 'Le ' + (fieldName.toLowerCase() === 'nom' ? 'nom' : 'prénom') + ' ne peut pas contenir uniquement des chiffres';
                isValid = false;
            }

            // Doit contenir au moins une lettre
            const hasLetter = /[a-zA-ZÀ-ÿ]/.test(value);
            if (!hasLetter) {
                errorMessage = 'Le ' + (fieldName.toLowerCase() === 'nom' ? 'nom' : 'prénom') + ' doit contenir au moins une lettre';
                isValid = false;
            }

            // Vérifier les caractères autorisés (lettres, espaces, tirets, apostrophes)
            const validChars = /^[a-zA-ZÀ-ÿ\s\-']+$/;
            if (!validChars.test(value)) {
                errorMessage = 'Le ' + (fieldName.toLowerCase() === 'nom' ? 'nom' : 'prénom') + ' contient des caractères non autorisés';
                isValid = false;
            }
        }

        // Matricule - format spécifique
        if (fieldName.toLowerCase().includes('matricule') && value) {
            const matriculeRegex = /^[A-Z0-9]+$/;
            if (!matriculeRegex.test(value)) {
                errorMessage = 'Le matricule doit contenir uniquement des lettres majuscules et des chiffres (ex: EMP001)';
                isValid = false;
            }
            if (value.length < 3) {
                errorMessage = 'Le matricule doit contenir au moins 3 caractères';
                isValid = false;
            }
        }

        // Nombre
        if (field.type === 'number' && value) {
            const num = parseFloat(value);
            const min = parseFloat(field.min);
            const max = parseFloat(field.max);

            if (isNaN(num)) {
                errorMessage = 'Veuillez entrer un nombre valide';
                isValid = false;
            } else if (!isNaN(min) && num < min) {
                errorMessage = `La valeur doit être supérieure ou égale à ${min}`;
                isValid = false;
            } else if (!isNaN(max) && num > max) {
                errorMessage = `La valeur doit être inférieure ou égale à ${max}`;
                isValid = false;
            }
        }

        // Date
        if (field.type === 'date' && value) {
            const selectedDate = new Date(value);
            const today = new Date();
            today.setHours(0, 0, 0, 0);

            // Vérification spécifique selon le champ
            if (fieldName.toLowerCase().includes('embauche') ||
                fieldName.toLowerCase().includes('debut') ||
                fieldName.toLowerCase().includes('generation') ||
                fieldName.toLowerCase().includes('affectation')) {
                if (selectedDate > today) {
                    errorMessage = 'La date ne peut pas être dans le futur';
                    isValid = false;
                }
            }
        }

        // Salaire
        if (fieldName.toLowerCase().includes('salaire') && value) {
            const salaire = parseFloat(value);
            if (salaire < 1000) {
                errorMessage = 'Le salaire doit être d\'au moins 1000€';
                isValid = false;
            }
            if (salaire > 1000000) {
                errorMessage = 'Le salaire semble irréaliste';
                isValid = false;
            }
        }

        // Select (dropdown)
        if (field.tagName === 'SELECT' && field.hasAttribute('required') && !value) {
            errorMessage = 'Vous devez faire une sélection';
            isValid = false;
        }

        if (!isValid) {
            this.showFieldError(field, errorMessage);
        } else {
            this.clearFieldError(field);
        }

        return isValid;
    }

    detectFormType() {
        const formAction = this.form.action;
        if (formAction.includes('employees')) return 'Employee';
        if (formAction.includes('projets')) return 'Projet';
        if (formAction.includes('departements')) return 'Departement';
        if (formAction.includes('affectations')) return 'Affectation';
        if (formAction.includes('fiches-paie')) return 'FichePaie';
        return null;
    }

    // Validation spécifique pour les employés
    validateEmployee() {
        let isValid = true;

        // Nom et Prénom - validation renforcée
        const nom = this.form.querySelector('#nom');
        const prenom = this.form.querySelector('#prenom');

        if (nom && nom.value) {
            const nomValue = nom.value.trim();
            if (/^\d+$/.test(nomValue)) {
                this.showFieldError(nom, 'Le nom ne peut pas contenir uniquement des chiffres');
                isValid = false;
            }
        }

        if (prenom && prenom.value) {
            const prenomValue = prenom.value.trim();
            if (/^\d+$/.test(prenomValue)) {
                this.showFieldError(prenom, 'Le prénom ne peut pas contenir uniquement des chiffres');
                isValid = false;
            }
        }

        // Email - validation stricte
        const email = this.form.querySelector('#email');
        if (email && email.value) {
            const emailValue = email.value.trim();
            const emailRegex = /^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
            if (!emailRegex.test(emailValue)) {
                this.showFieldError(email, 'Format d\'email invalide (ex: prenom.nom@cy-rh.local)');
                isValid = false;
            }
        }

        // Matricule - validation stricte
        const matricule = this.form.querySelector('#matricule');
        if (matricule && matricule.value) {
            const matriculeValue = matricule.value.trim();
            if (!/^[A-Z0-9]+$/.test(matriculeValue)) {
                this.showFieldError(matricule, 'Le matricule doit contenir uniquement des lettres majuscules et des chiffres');
                isValid = false;
            }
        }

        // Date d'embauche
        const dateEmbauche = this.form.querySelector('#dateEmbauche');
        if (dateEmbauche && dateEmbauche.value) {
            const embauche = new Date(dateEmbauche.value);
            const today = new Date();
            today.setHours(0, 0, 0, 0);

            if (embauche > today) {
                this.showFieldError(dateEmbauche, 'La date d\'embauche ne peut pas être dans le futur');
                isValid = false;
            }

            // Date d'embauche trop ancienne (plus de 70 ans)
            const minDate = new Date();
            minDate.setFullYear(minDate.getFullYear() - 70);
            if (embauche < minDate) {
                this.showFieldError(dateEmbauche, 'La date d\'embauche semble trop ancienne (plus de 70 ans)');
                isValid = false;
            }
        }

        // Salaire de base
        const salaireBase = this.form.querySelector('#salaireBase');
        if (salaireBase && salaireBase.value) {
            const salaire = parseFloat(salaireBase.value);
            if (isNaN(salaire)) {
                this.showFieldError(salaireBase, 'Le salaire doit être un nombre valide');
                isValid = false;
            } else if (salaire < 1000) {
                this.showFieldError(salaireBase, 'Le salaire de base doit être d\'au moins 1000€');
                isValid = false;
            } else if (salaire > 1000000) {
                this.showFieldError(salaireBase, 'Le salaire ne peut pas dépasser 1 000 000€');
                isValid = false;
            }
        }

        // Grade - vérification de sélection
        const grade = this.form.querySelector('#grade');
        if (grade && !grade.value) {
            this.showFieldError(grade, 'Vous devez sélectionner un grade');
            isValid = false;
        }

        // Rôle - vérification de sélection
        const role = this.form.querySelector('#role');
        if (role && !role.value) {
            this.showFieldError(role, 'Vous devez sélectionner un rôle');
            isValid = false;
        }

        // Poste
        const poste = this.form.querySelector('#poste');
        if (poste && poste.value) {
            const posteValue = poste.value.trim();
            if (posteValue.length < 3) {
                this.showFieldError(poste, 'Le poste doit contenir au moins 3 caractères');
                isValid = false;
            }
        }

        return isValid;
    }

    // Validation spécifique pour les projets
    validateProjet() {
        let isValid = true;

        const dateDebut = this.form.querySelector('#dateDebut');
        const dateFinPrevue = this.form.querySelector('#dateFinPrevue');
        const dateFinReelle = this.form.querySelector('#dateFinReelle');

        // Date de début
        if (dateDebut && dateDebut.value) {
            const debut = new Date(dateDebut.value);
            const minDate = new Date('2020-01-01');
            if (debut < minDate) {
                this.showFieldError(dateDebut, 'La date de début semble incorrecte');
                isValid = false;
            }
        }

        // Date de fin prévue après date de début
        if (dateDebut && dateFinPrevue && dateDebut.value && dateFinPrevue.value) {
            const debut = new Date(dateDebut.value);
            const finPrevue = new Date(dateFinPrevue.value);

            if (finPrevue <= debut) {
                this.showFieldError(dateFinPrevue, 'La date de fin prévue doit être après la date de début');
                isValid = false;
            }
        }

        // Date de fin réelle après date de début
        if (dateDebut && dateFinReelle && dateDebut.value && dateFinReelle.value) {
            const debut = new Date(dateDebut.value);
            const finReelle = new Date(dateFinReelle.value);

            if (finReelle <= debut) {
                this.showFieldError(dateFinReelle, 'La date de fin réelle doit être après la date de début');
                isValid = false;
            }
        }

        return isValid;
    }

    // Validation spécifique pour les affectations
    validateAffectation() {
        let isValid = true;

        const dateAffectation = this.form.querySelector('#dateAffectation');
        const dateFinAffectation = this.form.querySelector('#dateFinAffectation');

        // Date d'affectation
        if (dateAffectation && dateAffectation.value) {
            const affectation = new Date(dateAffectation.value);
            const today = new Date();
            today.setHours(0, 0, 0, 0);

            if (affectation > today) {
                this.showFieldError(dateAffectation, 'La date d\'affectation ne peut pas être dans le futur');
                isValid = false;
            }
        }

        // Date de fin après date de début
        if (dateAffectation && dateFinAffectation &&
            dateAffectation.value && dateFinAffectation.value) {
            const debut = new Date(dateAffectation.value);
            const fin = new Date(dateFinAffectation.value);

            if (fin <= debut) {
                this.showFieldError(dateFinAffectation, 'La date de fin doit être après la date d\'affectation');
                isValid = false;
            }
        }

        return isValid;
    }

    // Validation spécifique pour les fiches de paie
    validateFichePaie() {
        let isValid = true;

        const mois = this.form.querySelector('#mois');
        const annee = this.form.querySelector('#annee');
        const salaireBase = this.form.querySelector('#salaireBase');
        const primes = this.form.querySelector('#primes');
        const deductions = this.form.querySelector('#deductions');
        const dateGeneration = this.form.querySelector('#dateGeneration');
        const idEmployer = this.form.querySelector('#idEmployer');

        // Validation employé sélectionné
        if (idEmployer) {
            if (!idEmployer.value || idEmployer.value === '') {
                this.showFieldError(idEmployer, 'Vous devez sélectionner un employé');
                isValid = false;
            }
        }

        // Validation mois
        if (mois) {
            if (!mois.value || mois.value === '') {
                this.showFieldError(mois, 'Le mois est obligatoire');
                isValid = false;
            }
        }

        // Validation année
        if (annee) {
            if (!annee.value || annee.value === '') {
                this.showFieldError(annee, 'L\'année est obligatoire');
                isValid = false;
            } else {
                const anneeValue = parseInt(annee.value);
                if (isNaN(anneeValue)) {
                    this.showFieldError(annee, 'L\'année doit être un nombre valide');
                    isValid = false;
                } else if (anneeValue < 2020) {
                    this.showFieldError(annee, 'L\'année doit être supérieure ou égale à 2020');
                    isValid = false;
                }
            }
        }

        // Validation mois/année (pas de mois futur)
        if (mois && annee && mois.value && annee.value) {
            const selectedMonth = parseInt(mois.value);
            const selectedYear = parseInt(annee.value);
            const today = new Date();
            const currentMonth = today.getMonth() + 1;
            const currentYear = today.getFullYear();

            if (selectedYear > currentYear ||
                (selectedYear === currentYear && selectedMonth > currentMonth)) {
                this.showFieldError(mois, 'Impossible de créer une fiche de paie pour un mois futur');
                isValid = false;
            }
        }

        // Salaire de base
        if (salaireBase) {
            if (!salaireBase.value || salaireBase.value === '') {
                this.showFieldError(salaireBase, 'Le salaire de base est obligatoire');
                isValid = false;
            } else {
                const salaire = parseFloat(salaireBase.value);
                if (isNaN(salaire)) {
                    this.showFieldError(salaireBase, 'Le salaire doit être un nombre valide');
                    isValid = false;
                } else if (salaire < 1000) {
                    this.showFieldError(salaireBase, 'Le salaire de base doit être d\'au moins 1000€');
                    isValid = false;
                } else if (salaire > 1000000) {
                    this.showFieldError(salaireBase, 'Le salaire de base ne peut pas dépasser 1 000 000€');
                    isValid = false;
                }
            }
        }

        // Primes
        if (primes && primes.value && primes.value !== '') {
            const primesValue = parseFloat(primes.value);
            if (isNaN(primesValue)) {
                this.showFieldError(primes, 'Les primes doivent être un nombre valide');
                isValid = false;
            } else if (primesValue < 0) {
                this.showFieldError(primes, 'Les primes ne peuvent pas être négatives');
                isValid = false;
            } else if (primesValue > 100000) {
                this.showFieldError(primes, 'Le montant des primes semble irréaliste (max 100 000€)');
                isValid = false;
            }
        }

        // Déductions
        if (deductions && deductions.value && deductions.value !== '') {
            const deductionsValue = parseFloat(deductions.value);
            if (isNaN(deductionsValue)) {
                this.showFieldError(deductions, 'Les déductions doivent être un nombre valide');
                isValid = false;
            } else if (deductionsValue < 0) {
                this.showFieldError(deductions, 'Les déductions ne peuvent pas être négatives');
                isValid = false;
            } else {
                // Les déductions ne peuvent pas dépasser le salaire de base + primes
                if (salaireBase && salaireBase.value) {
                    const salaire = parseFloat(salaireBase.value) || 0;
                    const primesVal = primes && primes.value ? parseFloat(primes.value) : 0;
                    const total = salaire + primesVal;

                    if (deductionsValue > total) {
                        this.showFieldError(deductions, `Les déductions (${deductionsValue.toFixed(2)}€) ne peuvent pas dépasser le salaire total (${total.toFixed(2)}€)`);
                        isValid = false;
                    }
                }
            }
        }

        // Date de génération
        if (dateGeneration) {
            if (!dateGeneration.value || dateGeneration.value === '') {
                this.showFieldError(dateGeneration, 'La date de génération est obligatoire');
                isValid = false;
            } else {
                const generation = new Date(dateGeneration.value);
                const today = new Date();
                today.setHours(0, 0, 0, 0);

                if (generation > today) {
                    this.showFieldError(dateGeneration, 'La date de génération ne peut pas être dans le futur');
                    isValid = false;
                }

                // Vérifier que la date de génération est cohérente avec le mois/année
                if (mois && annee && mois.value && annee.value) {
                    const selectedMonth = parseInt(mois.value);
                    const selectedYear = parseInt(annee.value);
                    const generationMonth = generation.getMonth() + 1;
                    const generationYear = generation.getFullYear();

                    // La date de génération devrait être dans le mois de la fiche ou après
                    if (generationYear < selectedYear ||
                        (generationYear === selectedYear && generationMonth < selectedMonth)) {
                        this.showFieldError(dateGeneration, 'La date de génération ne peut pas être avant le mois de la fiche de paie');
                        isValid = false;
                    }
                }

                // Vérifier que la date de génération n'est pas trop ancienne
                const minDate = new Date();
                minDate.setFullYear(minDate.getFullYear() - 5);
                if (generation < minDate) {
                    this.showFieldError(dateGeneration, 'La date de génération semble trop ancienne (plus de 5 ans)');
                    isValid = false;
                }
            }
        }

        return isValid;
    }

    // Validation spécifique pour les départements
    validateDepartement() {
        let isValid = true;

        const intitule = this.form.querySelector('#intitule');
        if (intitule && intitule.value) {
            const value = intitule.value.trim();
            if (value.length < 3) {
                this.showFieldError(intitule, 'L\'intitulé doit contenir au moins 3 caractères');
                isValid = false;
            }
        }

        return isValid;
    }

    showFieldError(field, message) {
        field.classList.add('error');

        // Trouver ou créer le span d'erreur
        let errorSpan = field.parentElement.querySelector('.error-message');
        if (!errorSpan) {
            errorSpan = document.createElement('span');
            errorSpan.className = 'error-message';
            field.parentElement.appendChild(errorSpan);
        }

        errorSpan.textContent = message;
        errorSpan.style.display = 'block';
    }

    clearFieldError(field) {
        field.classList.remove('error');
        const errorSpan = field.parentElement.querySelector('.error-message');
        if (errorSpan) {
            errorSpan.textContent = '';
            errorSpan.style.display = 'none';
        }
    }

    showGlobalError(message) {
        // Chercher s'il y a déjà une alerte globale
        let alertDiv = this.form.querySelector('.alert.alert-error');

        if (!alertDiv) {
            alertDiv = document.createElement('div');
            alertDiv.className = 'alert alert-error';
            this.form.insertBefore(alertDiv, this.form.firstChild);
        }

        alertDiv.innerHTML = `<p>${message}</p>`;
        alertDiv.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
}

// Fonction d'initialisation globale
function initFormValidation(formId) {
    return new FormValidator(formId);
}

// Auto-initialisation au chargement du DOM
document.addEventListener('DOMContentLoaded', function() {
    // Chercher tous les formulaires sur la page
    const forms = document.querySelectorAll('form[th\\:action], form[action]');
    forms.forEach(form => {
        if (form.id) {
            new FormValidator(form.id);
        }
    });
});