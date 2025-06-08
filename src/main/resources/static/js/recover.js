function censurarCorreo(email) {
    const [usuario, dominio] = email.split("@");
    if (usuario.length <= 2) {
        return usuario[0] + "***@" + dominio;
    }
    const visible = usuario.slice(0, 2);
    return visible + "***@" + dominio;
}

document.getElementById('recoverForm').addEventListener('submit', async function(e) {
    e.preventDefault();
    const email = document.getElementById('email').value;

    const response = await fetch('/api/auth/send-recover-mail', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email })
    });

    if (response.ok) {
        document.getElementById('censoredEmail').textContent = censurarCorreo(email);
        document.getElementById('modal').style.display = 'block';
    } else {
       showToast("No se encontro la cuenta.", "FAILED");
    }
});

document.getElementById('closeModal').onclick = function() {
    document.getElementById('modal').style.display = 'none';
};

window.onclick = function(event) {
    if (event.target == document.getElementById('modal')) {
        document.getElementById('modal').style.display = 'none';
    }
};