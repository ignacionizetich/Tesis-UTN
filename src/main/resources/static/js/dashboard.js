document.addEventListener("DOMContentLoaded",() => {
    const modal = document.getElementById("transfer-modal")
    const openButton = document.querySelector(".tranferir")
    const closeButton = document.querySelector(".close-button")
    const tranferForm = document.getElementById("transfer-form")

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

            if (isNaN(valor) || valor <= 100){
                e.preventDefault();
                alert("por favor, ingresa un monto mayor a $100");
                montoInput.focus();
            }else {
                e.preventDefault();
                alert("transferencia enviada con exito");
                tranferForm.reset();
                modal.classList.add('hidden');
            }
        })
    }
})