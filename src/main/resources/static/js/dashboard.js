document.addEventListener("DOMContentLoaded", () => {
    // 🔐 Validar JWT y cargar datos del usuario
    const token = localStorage.getItem("JWT");

    if (!token) {
        window.location.href = "/PreLogin";
        return;
    }

    fetch("/api/user/data", {
        method: "GET",
        headers: {
            "Authorization": "Bearer " + token
        }
    })
        .then(async response => {
            if (!response.ok) {
                localStorage.removeItem("JWT");
                window.location.href = "/PreLogin";
                return;
            }

            const data = await response.json();

            // Mostrar datos en el DOM si querés
            const userNameEl = document.getElementById("user-name");
            const userEmailEl = document.getElementById("user-email");
            const userAliasEl = document.getElementById("user-alias");

            if (userNameEl) userNameEl.textContent = `${data.name} ${data.lastName}`;
            if (userEmailEl) userEmailEl.textContent = data.email;
            if (userAliasEl) userAliasEl.textContent = data.alias;
        })
        .catch(err => {
            console.error("Error al validar token:", err);
            localStorage.removeItem("JWT");
            window.location.href = "/PreLogin";
        });

    // Logout
    const logoutButton = document.getElementById("logout-button");
    if (logoutButton) {
        logoutButton.addEventListener("click", () => {
            localStorage.removeItem("JWT");
            window.location.href = "/PreLogin";
        });
    }

    // Transferencia
    const modal = document.getElementById("transfer-modal");
    const openButton = document.querySelector(".tranferir");
    const closeButton = document.querySelector(".close-button");
    const transferForm = document.getElementById("transfer-form");

    if (openButton) {
        openButton.addEventListener("click", () => {
            modal.classList.remove("hidden");
        });
    }

    if (closeButton) {
        closeButton.addEventListener("click", () => {
            modal.classList.add("hidden");
            transferForm.reset();
        });
    }

    window.addEventListener("click", (e) => {
        if (e.target === modal) {
            modal.classList.add("hidden");
            transferForm.reset();
        }
    });

    if (transferForm) {
        transferForm.addEventListener("submit", (e) => {
            e.preventDefault();

            const montoInput = document.getElementById("monto");
            const valor = parseFloat(montoInput.value);

            if (isNaN(valor) || valor <= 0) {
                alert("Por favor, ingresa un monto mayor o igual a $1");
                montoInput.focus();
            } else {
                alert("Transferencia enviada con éxito");
                transferForm.reset();
                modal.classList.add("hidden");
            }
        });
    }

    // Alias Modal
    const aliasModal = document.getElementById("alias-modal");
    const aliasButton = document.querySelector(".Alias-CVU");
    const closeAlias = document.querySelector(".close-alias");

    if (aliasButton) {
        aliasButton.addEventListener("click", () => {
            aliasModal.classList.remove("hidden");
        });
    }

    if (closeAlias) {
        closeAlias.addEventListener("click", () => {
            aliasModal.classList.add("hidden");
        });
    }

    window.addEventListener("click", (e) => {
        if (e.target === aliasModal) {
            aliasModal.classList.add("hidden");
        }
    });

    // Sidebar toggle
    const menuButton = document.getElementById("menu-toggle");
    const sidebar = document.getElementById("sidebar");

    if (menuButton && sidebar) {
        menuButton.addEventListener("click", () => {
            sidebar.classList.toggle("hidden");
        });
    }

    // Calculadora Impuestos
    const abrirCalculadora = document.getElementById('abrir-calculadora');
    const taxModal = document.getElementById('tax-modal');
    const closeTax = document.querySelector(".close-tax");

    if (abrirCalculadora) {
        abrirCalculadora.addEventListener("click", (e) => {
            e.preventDefault();
            taxModal.classList.remove("hidden");
        });
    }

    document.querySelectorAll(".tax-select-button").forEach(button => {
        button.addEventListener("click", () => {
            const target = button.getAttribute("data-target");
            document.getElementById("tax-options").classList.add("hidden");
            document.querySelectorAll(".tax-form").forEach(form => form.classList.add("hidden"));
            document.getElementById(`${target}-form`).classList.remove("hidden");
        });
    });

    if (closeTax) {
        closeTax.addEventListener("click", () => {
            taxModal.classList.add("hidden");
            document.getElementById("tax-options").classList.remove("hidden");
            document.querySelectorAll(".tax-form").forEach(form => form.classList.add("hidden"));
        });
    }

    // IVA
    const calcularIva = document.getElementById('calcular-iva');
    if (calcularIva) {
        calcularIva.addEventListener("click", () => {
            const monto = parseFloat(document.getElementById('iva-monto').value);
            if (isNaN(monto) || monto < 0) {
                alert("Por favor ingrese un monto válido.");
                return;
            }
            const iva = monto * 0.21;
            const total = monto + iva;
            document.getElementById('ivaResultado').textContent = iva.toFixed(2);
            document.getElementById('ivaTotal').textContent = total.toFixed(2);
        });
    }

    // Impuesto PAIS
    const calcularPais = document.getElementById('calcular-pais');
    if (calcularPais) {
        calcularPais.addEventListener("click", () => {
            const monto = parseFloat(document.getElementById('pais-monto').value);
            if (isNaN(monto) || monto < 0) {
                alert("Por favor ingrese un monto válido.");
                return;
            }
            const impuesto = monto * 0.40;
            const total = monto + impuesto;
            document.getElementById('paisResultado').textContent = impuesto.toFixed(2);
            document.getElementById('paisTotal').textContent = total.toFixed(2);
        });
    }

    // Perfil dropdown
    const profilePhoto = document.querySelector('.profile-photo');
    const profileMenu = document.getElementById('profileMenu');

    if (profilePhoto && profileMenu) {
        profilePhoto.addEventListener("click", (e) => {
            e.stopPropagation();
            profileMenu.style.display = (profileMenu.style.display === 'flex') ? 'none' : 'flex';
        });

        document.addEventListener("click", (e) => {
            if (!profilePhoto.contains(e.target) && !profileMenu.contains(e.target)) {
                profileMenu.style.display = 'none';
            }
        });
    }
});
