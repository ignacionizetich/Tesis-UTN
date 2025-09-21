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
        const censurado = censurarCorreo(email);
        showToast(`Se han enviado las instrucciones a ${censurado}.`, "SUCCESS");
    } else {
        showToast("El correo ingresado no se asocia a una cuenta existente.", "FAILED");
    }

    /// logica para volver atras
    document.getElementById("back-button").addEventListener("click", () =>{
        window.history.back()
    })
});

