document.addEventListener("DOMContentLoaded", () => {
    actualizarSaldo();
    console.log("El DOM está cargado, ejecutando script");
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

            const userNameEl = document.getElementById("user-name");
            const userEmailEl = document.getElementById("user-email");
            const userAliasEl = document.getElementById("user-alias");
            const userBalanceEl = document.getElementById("user-balance");
            const userNameTopbar = document.getElementById("user-name-topbar");


            if (userNameEl) userNameEl.textContent = `${data.name} ${data.lastName} 👋`;
            if (userEmailEl) userEmailEl.textContent = data.email;
            if (userAliasEl) userAliasEl.textContent = `Alias: ${data.alias}`;
            if (userBalanceEl) userBalanceEl.textContent = data.balance.toFixed(2);
            if (userNameTopbar) userNameTopbar.textContent = data.alias;
            document.querySelector(".dashboard").classList.add("loaded");
        })
        .catch(err => {
            console.error("Error al validar token:", err);
            localStorage.removeItem("JWT");
            window.location.href = "/PreLogin";
        });

    // Ingresar Dinero
    const ingresarModal = document.getElementById("ingresar-modal");
    const ingresarButton = document.getElementById("btn-ingresar");

    const closeIngresar = document.querySelector(".close-ingresar");
    const ingresarForm = document.getElementById("ingresar-form");

    if (ingresarButton) {
        ingresarButton.addEventListener("click", () => {
            console.log("Click en boton ingresar");
            ingresarModal.classList.remove("hidden");
        });
    }

    if (closeIngresar) {
        closeIngresar.addEventListener("click", () => {
            ingresarModal.classList.add("hidden");
            ingresarForm.reset();
        });
    }

    window.addEventListener("click", (e) => {
        if (e.target === ingresarModal) {
            ingresarModal.classList.add("hidden");
            ingresarForm.reset();
        }
    });

    if (ingresarForm) {
        ingresarForm.addEventListener("submit", async (e) => {
            e.preventDefault();

            const monto = parseFloat(document.getElementById("ingresar-monto").value);
            if (isNaN(monto) || monto <= 0) {
                alert("Por favor ingrese un monto válido.");
                return;
            }

            const token = localStorage.getItem("JWT");
            const accountId = localStorage.getItem("accountId");

            try {
                const response = await fetch(`/api/accounts/${accountId}/balance`, {
                    method: "PUT",
                    headers: {
                        "Content-Type": "application/json",
                        "Authorization": "Bearer " + token
                    },
                    body: JSON.stringify({ balance: monto })
                });

                const result = await response.json();

                if (response.ok && result.success) {
                    alert("Ingreso exitoso: " + result.message);
                    ingresarForm.reset();
                    ingresarModal.classList.add("hidden");
                    actualizarSaldo();
                } else {
                    alert("Error: " + result.message);
                }
            } catch (err) {
                alert("Error al ingresar dinero.");
                console.error(err);
            }
        });
    }


    ///ACTUALIZAR DINERO DE LA CUENTA
    function actualizarSaldo() {
        const token = localStorage.getItem("JWT");
        const accountId = localStorage.getItem("accountId");

        if (!token || !accountId) return;

        fetch(`/api/accounts/${accountId}/showBalance`, {
            method: "GET",
            headers: {
                "Authorization": "Bearer " + token
            }
        })
            .then(res => {
                if (!res.ok) throw new Error("No autorizado o cuenta no encontrada");
                return res.json();
            })
            .then(data => {
                const saldoEl = document.getElementById("balance");
                if (saldoEl) saldoEl.textContent = `${data.balance.toFixed(2)}`;

                localStorage.setItem("alias", data.alias);
                localStorage.setItem("cvu", data.cvu);

            })
            .catch(err => {
                console.error("Error al obtener el saldo:", err);
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
            // Obtener alias y cvu del localStorage
            const alias = localStorage.getItem("alias") || "No disponible";
            const cvu = localStorage.getItem("cvu") || "No disponible";

            // Actualizar el texto en el modal
            document.getElementById("alias-value").textContent = alias;
            document.getElementById("cvu-value").textContent = cvu;
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

    let selectedCurrency = "ARS";

    const currencyButtons = document.querySelectorAll(".currency-button");
    const taxForm = document.getElementById("tax-form");
    const selectedCurrencyLabel = document.getElementById("selected-currency-label");
    const calcularBtn = document.getElementById("calcular-tax");
    const taxResult = document.getElementById("tax-result");
    const taxMonto = document.getElementById("tax-monto");

    currencyButtons.forEach(button => {
        button.addEventListener("click", () => {
            selectedCurrency = button.getAttribute("data-currency");
            selectedCurrencyLabel.textContent = selectedCurrency;
            taxForm.classList.remove("hidden");
            taxResult.textContent = "";
            taxMonto.value = "";
        });
    });

    calcularBtn.addEventListener("click", async () => {
        const monto = parseFloat(taxMonto.value);

        if (isNaN(monto) || monto <= 0) {
            alert("Por favor, ingrese un monto válido.");
            return;
        }

        let url = "";
        if (selectedCurrency === "ARS") {
            url = `/api/impuestos/calculateARS?montoARS=${monto}`;
        } else if (selectedCurrency === "USD") {
            url = `/api/impuestos/calculateUSD?montoUSD=${monto}`;
        } else {
            alert("Moneda no válida.");
            return;
        }

        try {
            const response = await fetch(url);
            if (!response.ok) {
                const errorText = await response.text();
                alert("Error al calcular impuestos: " + errorText);
                return;
            }

            const data = await response.json();
            console.log("Respuesta del servidor:", data);
            if (data && typeof data.totalFinal === "number") {
                let detalle = `<p><strong class="label">Monto sin impuestos:</strong> <span class="value">$${data.montoOriginal.toFixed(2)} ARS</span></p>`;

                if (selectedCurrency === "ARS") {
                    detalle += `<p><strong class="label">IVA 21%:</strong> <span class="value">$${data.iva.toFixed(2)} ARS</span></p>`;
                } else if (selectedCurrency === "USD") {
                    detalle += `<p><strong class="label">IVA 21%:</strong> <span class="value">$${data.iva.toFixed(2)} ARS</span></p>`;
                    detalle += `<p><strong class="label">Percepción Ganancias 30%:</strong> <span class="value">$${data.percepcionGanancias.toFixed(2)} ARS</span></p>`;
                    detalle += `<p><strong class="label">Cotización dólar oficial:</strong> <span class="value">$${data.precioDolar.toFixed(2)} ARS</span></p>`;
                }

                detalle += `<p><strong class="label">Total con impuestos:</strong> <span class="value strong">$${data.totalFinal.toFixed(2)} ARS</span></p>`;

                taxResult.innerHTML = detalle;
            } else {
                taxResult.textContent = "No se pudo obtener el resultado del cálculo.";
            }






        } catch (error) {
            alert("Error al conectar con el servidor.");
            console.error(error);
        }
    });


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
