document.addEventListener('DOMContentLoaded', () => {
    const loginBtn = document.getElementById('btn-login');
    const registerBtn = document.getElementById('btn-register');
    const forgotLink = document.getElementById('link-forgot');

    if (loginBtn) {
        loginBtn.addEventListener('click', () => {
            window.location.href = '/login';
        });
    }

    if (registerBtn) {
        registerBtn.addEventListener('click', () => {
            window.location.href = '/register';
        });
    }

    if (forgotLink) {
        forgotLink.addEventListener('click', () => {
            window.location.href = '/forgot';
        });
    }
});
