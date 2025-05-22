document.addEventListener("DOMContentLoaded",() => {
    const modal = document.getElementById("transfer-modal");
    const openButton = document.querySelector(".tranferir");
    const closeButton = document.querySelector(".close-button");
    const tranferForm = document.getElementById("transfer-form");
    const aliasModal = document.getElementById('alias-modal');
    const aliasButton = document.querySelector('.Alias-CVU');
    const closeAlias = document.querySelector('.close-alias');

    if (openButton){
        openButton.addEventListener('click', () => {
            modal.classList.remove('hidden');
        });
    }

    if (closeButton){
        closeButton.addEventListener('click',() =>{
            modal.classList.add('hidden');
            tranferForm.reset();
        });
    }

    window.addEventListener('click', (e) => {
        if (e.target === modal){
            modal.classList.add('hidden');
            tranferForm.reset();
        }
    });

    if (tranferForm){
        tranferForm.addEventListener('submit', (e) =>{
            const montoInput = document.getElementById("monto");
            const valor = parseInt(montoInput.value);

            if (isNaN(valor) || valor < 0){
                e.preventDefault();
                alert("Por Favor, ingresa un monto mayor o igual a $1");
                montoInput.focus();
            }else {
                e.preventDefault();
                alert("Transferencia Enviada con Exito");
                tranferForm.reset();
                modal.classList.add('hidden');
            }
        })
    }

    if (aliasButton) {
        aliasButton.addEventListener('click', () => {
            aliasModal.classList.remove('hidden');
        });
    }

    if (closeAlias) {
        closeAlias.addEventListener('click', () => {
            aliasModal.classList.add('hidden');
        });
    }

    window.addEventListener('click', (e) => {
        if (e.target === aliasModal) {
            aliasModal.classList.add('hidden');
        }
    });
})
/*script header pa*/
document.addEventListener("DOMContentLoaded", () => {
    const menuButton = document.getElementById("menu-toggle");
    const sidebar = document.getElementById("sidebar");

    if (menuButton && sidebar) {
        menuButton.addEventListener("click", () => {
            sidebar.classList.toggle("hidden");
        });
    }
});
document.addEventListener('DOMContentLoaded', () => {

    // Abrir modal
    document.getElementById('abrir-calculadora').addEventListener('click', (e) => {
        e.preventDefault();
        document.getElementById('tax-modal').classList.remove('hidden');
    });

    // Manejo selección calculadora
    document.querySelectorAll(".tax-select-button").forEach(button => {
        button.addEventListener("click", () => {
            const target = button.getAttribute("data-target");
            document.getElementById("tax-options").classList.add("hidden");
            document.querySelectorAll(".tax-form").forEach(form => form.classList.add("hidden"));
            document.getElementById(`${target}-form`).classList.remove("hidden");
        });
    });

    // Cerrar modal y resetear vista
    document.querySelector(".close-tax").addEventListener("click", () => {
        document.getElementById("tax-modal").classList.add("hidden");
        document.getElementById("tax-options").classList.remove("hidden");
        document.querySelectorAll(".tax-form").forEach(form => form.classList.add("hidden"));
    });

    // Calcular IVA
    document.getElementById('calcular-iva').addEventListener('click', () => {
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

    // Calcular Impuesto PAIS
    document.getElementById('calcular-pais').addEventListener('click', () => {
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

});

