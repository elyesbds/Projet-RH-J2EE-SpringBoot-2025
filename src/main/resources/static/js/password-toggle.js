document.addEventListener('DOMContentLoaded', function() {
    const togglePassword = document.getElementById('togglePassword');
    const password = document.getElementById('password');

    if (togglePassword && password) {
        togglePassword.addEventListener('click', function() {
            // Basculer le type du champ entre 'password' et 'text'
            const type = password.getAttribute('type') === 'password' ? 'text' : 'password';
            password.setAttribute('type', type);

            // Optionnel : Changer l'icône de l'œil pour indiquer l'état (ou ajouter une classe)
            // Si vous utilisez l'emoji :
            this.textContent = (type === 'password') ? '👁️' : '🙈';

            // Si vous utilisez des classes pour changer l'icône (ex: avec Font Awesome) :
            // this.classList.toggle('fa-eye');
            // this.classList.toggle('fa-eye-slash');
        });
    }
});