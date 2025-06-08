document.addEventListener("DOMContentLoaded", function () {
    const form = document.getElementById('login');
    const usuarioInput = form.querySelector('input[name="username"]');
    const passwordInput = form.querySelector('input[name="password"]');
// Verificar si hay una sesión activa
    const token = localStorage.getItem('JWT');
    if (token) {
        // Si hay un token, redirigir al dashboard
        window.location.href = '/dashboard';
        return;
    }

    form.addEventListener('submit', function (event) {
        event.preventDefault();

        // Limpiar mensajes de error previos
        removeErrorMessages();

        // Validar campos
        let valid = true;

        if (!usuarioInput.value.trim()) {
            showError(usuarioInput, "El campo Usuario es obligatorio.");
            valid = false;
        }

        if (!passwordInput.value.trim()) {
            showError(passwordInput, "El campo Contraseña es obligatorio.");
            valid = false;
        }

        // Si todo es válido, realizar el login
        if (valid) {
            const loginData = {
                username: usuarioInput.value.trim(),
                password: passwordInput.value.trim()
            };
            login(loginData);
        }
    });

    // Función para mostrar mensaje de error
    function showError(input, message) {
        const error = document.createElement('div');
        error.classList.add('error-message');
        error.textContent = message;
        input.parentElement.appendChild(error);
    }

    // Función para eliminar mensajes de error
    function removeErrorMessages() {
        const errorMessages = document.querySelectorAll('.error-message');
        errorMessages.forEach(error => error.remove());
    }

    // Función para hacer la solicitud de inicio de sesión
    function login(loginData) {
        fetch('/api/auth/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'

            },
            body: JSON.stringify(loginData)
        })
            .then(async response => {
                const contentType = response.headers.get("content-type");
                if (!response.ok) {
                    const errorText = await response.text();
                    throw new Error(`Error del servidor: ${response.status} - ${errorText}`);
                }

                if (!contentType || !contentType.includes("application/json")) {
                    throw new Error("La respuesta no es JSON.");
                }

                return response.json();
            })
            .then(data => {

                if (data.success) {
                    showToast("Inicio de sesion exitoso","success")
                    form.reset();
                    localStorage.setItem('JWT', data.accessToken);
                    localStorage.setItem('accountId', data.accountId);
                    console.log('Redirigiendo al dashboard...'); // 🔍
                    window.location.href = '/dashboard';
                } else {
                    showToast(data.message || 'Credenciales incorrectas', 'error');
                }
            })
            .catch(error => {
                console.error('Error en la solicitud:', error);
                alert('Hubo un error al intentar iniciar sesión.');
            });
    }
});