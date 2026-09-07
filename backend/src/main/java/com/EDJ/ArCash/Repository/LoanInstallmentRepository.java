package com.EDJ.ArCash.Repository;

import com.EDJ.ArCash.Models.Imp.LoanInstallmentStatus;
import com.EDJ.ArCash.Models.LoanInstallment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LoanInstallmentRepository extends JpaRepository<LoanInstallment, Long> {

    /**
     * Marca la cuota como pagada solo si seguia pendiente, decidiendolo dentro del UPDATE.
     *
     * <p>Sirve para reservar la cuota antes de debitar: si dos pagos del mismo prestamo entran
     * a la vez, ambos pueden ver la misma cuota como PENDING, pero solo uno consigue afectar la
     * fila. El otro recibe 0 y aborta, de modo que el titular no paga dos veces la misma cuota.
     *
     * @return cantidad de filas afectadas: 0 si otro pago se adelanto.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE LoanInstallment i SET i.status = :paid, i.paidAt = :paidAt "
            + "WHERE i.id = :id AND i.status = :pending")
    int markAsPaidIfPending(@Param("id") Long id,
                            @Param("paidAt") String paidAt,
                            @Param("paid") LoanInstallmentStatus paid,
                            @Param("pending") LoanInstallmentStatus pending);
}
