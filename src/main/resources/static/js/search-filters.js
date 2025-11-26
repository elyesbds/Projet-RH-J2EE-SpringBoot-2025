/**
 * Système de recherche et filtres dynamiques pour Spring Boot + Thymeleaf
 * Compatible avec les tableaux générés par Thymeleaf
 *
 * Utilisation :
 * 1. Ajouter une barre de recherche avec id="searchInput"
 * 2. Ajouter un conteneur pour les filtres avec id="filterContainer"
 * 3. Votre tableau doit avoir un id unique (ex: id="employeesTable")
 * 4. Appeler initTableSearchFilter('tableId', 'searchInput', 'filterContainer')
 */

class TableSearchFilter {
    constructor(tableId, searchInputId, filterContainerId) {
        this.table = document.getElementById(tableId);
        this.searchInput = document.getElementById(searchInputId);
        this.filterContainer = document.getElementById(filterContainerId);
        this.tbody = this.table ? this.table.querySelector('tbody') : null;
        this.rows = this.tbody ? Array.from(this.tbody.querySelectorAll('tr')) : [];
        this.filters = {};

        if (this.table && this.searchInput) {
            this.init();
        } else {
            console.warn('TableSearchFilter: Éléments requis non trouvés', {
                table: !!this.table,
                searchInput: !!this.searchInput,
                filterContainer: !!this.filterContainer
            });
        }
    }

    init() {
        console.log('Initialisation du système de recherche et filtres...');

        // Initialiser la recherche
        this.searchInput.addEventListener('input', () => this.applyFilters());

        // Créer les filtres automatiquement basés sur les colonnes
        this.createFilters();

        // Ajouter les écouteurs pour les filtres
        this.attachFilterListeners();

        console.log(`Système initialisé avec ${this.rows.length} lignes`);
    }

    createFilters() {
        if (!this.filterContainer) {
            console.warn('Conteneur de filtres non trouvé');
            return;
        }

        const headers = this.table.querySelectorAll('thead th');
        const filterHTML = [];

        headers.forEach((header, index) => {
            const headerText = header.textContent.trim();

            // Ignorer la colonne "Actions" et "ID"
            if (headerText.toLowerCase() === 'actions' ||
                headerText.toLowerCase() === 'id') {
                return;
            }

            // Récupérer les valeurs uniques pour cette colonne
            const uniqueValues = this.getUniqueColumnValues(index);

            // Créer un filtre seulement si on a entre 2 et 20 valeurs uniques
            if (uniqueValues.length > 1 && uniqueValues.length <= 20) {
                filterHTML.push(`
                    <div class="filter-group">
                        <label for="filter-${index}">${headerText}</label>
                        <select id="filter-${index}" class="filter-select" data-column="${index}">
                            <option value="">Tous</option>
                            ${uniqueValues.map(val => `<option value="${this.escapeHtml(val)}">${this.escapeHtml(val)}</option>`).join('')}
                        </select>
                    </div>
                `);
            }
        });

        if (filterHTML.length > 0) {
            this.filterContainer.innerHTML = `
                <div class="filters-wrapper">
                    ${filterHTML.join('')}
                    <button class="btn-reset-filters" type="button">
                        🔄 Réinitialiser
                    </button>
                </div>
            `;

            // Attacher l'événement au bouton reset
            const resetBtn = this.filterContainer.querySelector('.btn-reset-filters');
            if (resetBtn) {
                resetBtn.addEventListener('click', () => this.resetFilters());
            }
        } else {
            console.log('Aucun filtre généré (pas assez de valeurs uniques)');
        }
    }

    getUniqueColumnValues(columnIndex) {
        const values = new Set();

        this.rows.forEach(row => {
            const cell = row.cells[columnIndex];
            if (cell) {
                let value = cell.textContent.trim();
                // Nettoyer les valeurs (enlever les emojis et espaces multiples)
                value = value.replace(/[\u{1F300}-\u{1F9FF}]/gu, '').replace(/\s+/g, ' ').trim();

                if (value && value !== '-' && value !== '') {
                    values.add(value);
                }
            }
        });

        return Array.from(values).sort();
    }

    attachFilterListeners() {
        const filterSelects = this.filterContainer.querySelectorAll('.filter-select');
        filterSelects.forEach(select => {
            select.addEventListener('change', (e) => {
                const column = e.target.dataset.column;
                const value = e.target.value;

                if (value) {
                    this.filters[column] = value;
                } else {
                    delete this.filters[column];
                }

                this.applyFilters();
            });
        });
    }

    applyFilters() {
        const searchTerm = this.searchInput.value.toLowerCase().trim();
        let visibleCount = 0;

        this.rows.forEach(row => {
            let showRow = true;

            // Appliquer la recherche textuelle globale
            if (searchTerm) {
                const rowText = row.textContent.toLowerCase();
                if (!rowText.includes(searchTerm)) {
                    showRow = false;
                }
            }

            // Appliquer les filtres de colonnes spécifiques
            if (showRow) {
                for (const [column, filterValue] of Object.entries(this.filters)) {
                    const cell = row.cells[column];
                    if (cell) {
                        let cellText = cell.textContent.trim();
                        // Nettoyer le texte de la cellule
                        cellText = cellText.replace(/[\u{1F300}-\u{1F9FF}]/gu, '').replace(/\s+/g, ' ').trim();

                        if (!cellText.includes(filterValue)) {
                            showRow = false;
                            break;
                        }
                    }
                }
            }

            // Afficher ou masquer la ligne
            if (showRow) {
                row.style.display = '';
                visibleCount++;
            } else {
                row.style.display = 'none';
            }
        });

        // Afficher un message si aucun résultat
        this.updateNoResultsMessage(visibleCount);

        console.log(`Filtres appliqués: ${visibleCount}/${this.rows.length} lignes visibles`);
    }

    updateNoResultsMessage(visibleCount) {
        const tableContainer = this.table.parentElement;
        let noResultsMsg = tableContainer.querySelector('.no-results-message');

        if (visibleCount === 0) {
            if (!noResultsMsg) {
                noResultsMsg = document.createElement('div');
                noResultsMsg.className = 'no-results-message';
                noResultsMsg.innerHTML = `
                    <div style="text-align: center; padding: 40px; background: white; border-radius: 10px; margin-top: 20px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                        <div style="font-size: 4em; margin-bottom: 20px;">🔍</div>
                        <h3 style="color: #666; margin-bottom: 10px;">Aucun résultat trouvé</h3>
                        <p style="color: #999;">Essayez de modifier vos critères de recherche ou de réinitialiser les filtres</p>
                    </div>
                `;
                tableContainer.appendChild(noResultsMsg);
            }
            noResultsMsg.style.display = 'block';
            this.table.style.display = 'none';
        } else {
            if (noResultsMsg) {
                noResultsMsg.style.display = 'none';
            }
            this.table.style.display = '';
        }
    }

    resetFilters() {
        console.log('Réinitialisation de tous les filtres...');

        // Réinitialiser la recherche
        this.searchInput.value = '';

        // Réinitialiser tous les filtres
        this.filters = {};
        const filterSelects = this.filterContainer.querySelectorAll('.filter-select');
        filterSelects.forEach(select => {
            select.value = '';
        });

        // Réappliquer (tout afficher)
        this.applyFilters();
    }

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
}

// Fonction d'initialisation globale pour Spring Boot
function initTableSearchFilter(tableId, searchInputId, filterContainerId) {
    console.log('Initialisation du filtre de tableau:', { tableId, searchInputId, filterContainerId });
    window.tableFilter = new TableSearchFilter(tableId, searchInputId, filterContainerId);
}

// Initialisation automatique au chargement du DOM
document.addEventListener('DOMContentLoaded', function() {
    console.log('DOM chargé - Recherche de tableaux à filtrer...');
});