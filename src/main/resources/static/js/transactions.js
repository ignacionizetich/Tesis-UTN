document.addEventListener("DOMContentLoaded", function () {
    const lista = document.querySelector(".movimientos-lista");
    const userID = Number(localStorage.getItem("accountId"));
    const modal = document.getElementById("modalTransferencia");
    const cerrarModal = document.getElementById("cerrarModal");

    fetch(`/api/transactions/${userID}/getTransactions`)
        .then(response => response.json())
        .then(data => {
            lista.innerHTML = "";

            data.forEach(mov => {
                const li = document.createElement("li");

                const esSalida = mov.idOrigin === userID;
                const tipo = esSalida ? "negativo" : "positivo";
                const signo = esSalida ? "-" : "+";
                const montoFormateado = `${signo}$${Math.abs(mov.amount).toLocaleString("es-AR")}`;
                const usuarioRelacionado = esSalida ? mov.destinationUsername : mov.originUsername;
                const fechaObj = new Date(mov.date);
                const fechaFormateada = fechaObj.toLocaleDateString("es-AR", {
                    day: '2-digit',
                    month: '2-digit',
                    year: 'numeric'
                });

                li.innerHTML = `
                  <span class="fecha">${fechaFormateada}</span>
                  <span class="descripcion">transferencia con ${usuarioRelacionado}</span>
                  <span class="monto ${tipo}">${montoFormateado}</span>
                `;

                // Al hacer click, mostrar modal con detalles
                li.addEventListener("click", function() {
                    document.getElementById("modalOperacion").textContent = mov.idOperation;
                    document.getElementById("modalOrigen").textContent = mov.originUsername;
                    document.getElementById("modalDestino").textContent = mov.destinationUsername;
                    document.getElementById("modalMonto").textContent = "$" + mov.amount.toLocaleString("es-AR");
                    document.getElementById("modalEstado").textContent = mov.state;

                    // Formatear fecha para el modal
                    const fechaModal = fechaObj.toLocaleString("es-AR", {
                        day: '2-digit',
                        month: '2-digit',
                        year: 'numeric',
                        hour: 'numeric',
                        minute: 'numeric'
                    });
                    document.getElementById("modalFecha").textContent = fechaModal;

                    // Mostrar modal (versión corregida)
                    modal.classList.remove("hidden");
                });

                lista.appendChild(li);
            });
        })
        .catch(err => {
            console.error("Error al cargar los movimientos:", err);
            lista.innerHTML = "<li>Error al cargar los movimientos</li>";
        });

    // Cerrar modal (versión corregida)
    cerrarModal.addEventListener("click", function() {
        modal.classList.add("hidden");
    });

    // Cerrar al hacer clic fuera del modal
    modal.addEventListener("click", function(event) {
        if (event.target === modal) {
            modal.classList.add("hidden");
        }
    });
});