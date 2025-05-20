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
