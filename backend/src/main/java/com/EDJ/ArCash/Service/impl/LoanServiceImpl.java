package com.EDJ.ArCash.Service.impl;
import com.EDJ.ArCash.Service.interfaces.LoanRateConfigService;
import com.EDJ.ArCash.Service.interfaces.AccountService;
import com.EDJ.ArCash.Service.interfaces.LoanService;

import com.EDJ.ArCash.Models.Account;
import com.EDJ.ArCash.Models.Loan;
import com.EDJ.ArCash.Models.LoanInstallment;
import com.EDJ.ArCash.Models.Transaction;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Models.Imp.Currency;
import com.EDJ.ArCash.Models.Imp.LoanInstallmentStatus;
import com.EDJ.ArCash.Models.Imp.LoanStatus;
import com.EDJ.ArCash.Repository.AccountRepository;
import com.EDJ.ArCash.Repository.LoanInstallmentRepository;
import com.EDJ.ArCash.Repository.LoanRepository;
import com.EDJ.ArCash.Repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LoanServiceImpl implements LoanService {

    public static final double MIN_PRINCIPAL = 1_000.0;
    public static final double MAX_PRINCIPAL = 500_000.0;
    public static final Set<Integer> ALLOWED_INSTALLMENTS = Set.of(3, 6, 12);

    public static final String OP_LOAN_CREDIT = "LOAN_CREDIT";
    public static final String OP_LOAN_PAYMENT = "LOAN_PAYMENT";

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final LoanRepository loanRepository;
    private final LoanInstallmentRepository loanInstallmentRepository;
    private final AccountRepository accountRepository;
    private final AccountService accountService;
    private final TransactionRepository transactionRepository;
    private final LoanRateConfigService loanRateConfigService;



    public double monthlyRateFor(int installments) {
        return loanRateConfigService.monthlyRateFor(installments);
    }

    public SimulationResult simulate(double principal, int installments) {
        validateRequest(principal, installments);
        double monthlyRate = monthlyRateFor(installments);
        double cuota = round2(frenchPayment(principal, monthlyRate, installments));
        double total = round2(cuota * installments);
        List<InstallmentPlan> schedule = buildSchedule(cuota, installments, LocalDate.now());
        return new SimulationResult(principal, installments, monthlyRate, cuota, total, schedule);
    }

    @Transactional(readOnly = true)
    public List<Loan> listForUser(Long userId) {
        return loanRepository.findByUser_IdOrderByIdDesc(userId);
    }

    @Transactional(readOnly = true)
    public Optional<Loan> findOwned(Long loanId, Long userId) {
        return loanRepository.findByIdAndUser_Id(loanId, userId);
    }

    @Transactional
    public Loan accept(User user, double principal, int installments) {
        if (loanRepository.existsByUser_IdAndStatus(user.getId(), LoanStatus.ACTIVE)) {
            throw new IllegalStateException("Ya tenés un préstamo activo. Cancelalo pagando las cuotas primero.");
        }
        SimulationResult sim = simulate(principal, installments);
        Account ars = accountRepository.findArsAccountByUserId(user.getId(), Currency.ARS)
                .orElseThrow(() -> new IllegalStateException("No tenés cuenta en pesos."));

        // La acreditacion va antes de armar el prestamo porque el UPDATE atomico limpia el
        // contexto de persistencia: cualquier entidad cargada antes quedaria desasociada.
        if (!accountService.updateBalance(sim.principal(), ars.getIdAccount())) {
            throw new IllegalStateException("No se pudo acreditar el préstamo en tu cuenta.");
        }

        Loan loan = new Loan();
        loan.setUser(user);
        loan.setAccount(ars);
        loan.setPrincipal(sim.principal());
        loan.setMonthlyRate(sim.monthlyRate());
        loan.setInstallmentCount(sim.installments());
        loan.setInstallmentAmount(sim.installmentAmount());
        loan.setTotalAmount(sim.totalAmount());
        loan.setStatus(LoanStatus.ACTIVE);

        for (InstallmentPlan plan : sim.schedule()) {
            LoanInstallment row = new LoanInstallment();
            row.setLoan(loan);
            row.setInstallmentNumber(plan.number());
            row.setDueDate(plan.dueDate());
            row.setAmount(plan.amount());
            row.setStatus(LoanInstallmentStatus.PENDING);
            loan.getInstallments().add(row);
        }

        Loan saved = loanRepository.save(loan);
        // Releer cuenta con saldo actualizado para el movimiento
        Account refreshed = accountRepository.findByIdAccount(ars.getIdAccount()).orElse(ars);
        recordLedger(refreshed, refreshed, sim.principal(), OP_LOAN_CREDIT,
                "Préstamo " + sim.installments() + " cuotas");
        return saved;
    }

    @Transactional
    public Loan payNext(Loan loan) {
        if (loan.getStatus() != LoanStatus.ACTIVE) {
            throw new IllegalStateException("El préstamo ya está cancelado.");
        }
        LoanInstallment pendiente = loan.getInstallments().stream()
                .filter(i -> i.getStatus() == LoanInstallmentStatus.PENDING)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No hay cuotas pendientes."));

        int numeroCuota = pendiente.getInstallmentNumber();
        double monto = pendiente.getAmount();
        Long cuentaId = loan.getAccount().getIdAccount();

        // Reservar la cuota antes de debitar: dos pagos simultaneos pueden ver la misma cuota
        // como pendiente, pero solo uno logra marcarla, asi que nadie la paga dos veces.
        int reservada = loanInstallmentRepository.markAsPaidIfPending(
                pendiente.getId(),
                LocalDateTime.now().format(TS),
                LoanInstallmentStatus.PAID,
                LoanInstallmentStatus.PENDING);
        if (reservada == 0) {
            throw new IllegalStateException("La cuota ya fue pagada.");
        }

        // El debito comprueba el saldo dentro del propio UPDATE, asi que dos pagos simultaneos
        // de la misma cuota no pueden dejar la cuenta en negativo: el segundo no afecta filas.
        // Si falla, la excepcion revierte la transaccion y libera la reserva de la cuota.
        if (!accountService.updateBalance(-monto, cuentaId)) {
            throw new IllegalStateException("Saldo insuficiente para pagar la cuota.");
        }

        // Los UPDATE anteriores limpiaron el contexto: hay que releer el prestamo para mutarlo.
        Loan fresco = loanRepository.findById(loan.getId())
                .orElseThrow(() -> new IllegalStateException("El préstamo ya no existe."));

        boolean todasPagas = fresco.getInstallments().stream()
                .allMatch(i -> i.getStatus() == LoanInstallmentStatus.PAID);
        if (todasPagas) {
            fresco.setStatus(LoanStatus.PAID_OFF);
        }
        Loan saved = loanRepository.save(fresco);

        Account refreshed = accountRepository.findByIdAccount(cuentaId).orElseThrow();
        recordLedger(
                refreshed,
                refreshed,
                monto,
                OP_LOAN_PAYMENT,
                "Cuota " + numeroCuota + "/" + fresco.getInstallmentCount());
        return saved;
    }

    /**
     * Cuota fija del sistema frances.
     *
     * <p>Los guardas no son decorativos: con {@code n == 0} la division devolveria infinito y
     * con una tasa negativa saldria una cuota menor al capital prestado. Ninguno de los dos
     * casos llega por la API (las cuotas estan en una lista blanca y la tasa tiene limites al
     * configurarse), pero una tasa corrupta en la base entraria por aca sin avisar.
     */
    public static double frenchPayment(double principal, double monthlyRate, int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("La cantidad de cuotas debe ser mayor a cero.");
        }
        if (monthlyRate < 0) {
            throw new IllegalArgumentException("La tasa mensual no puede ser negativa.");
        }
        if (monthlyRate == 0) {
            return principal / n;
        }
        double factor = Math.pow(1 + monthlyRate, n);
        return principal * (monthlyRate * factor) / (factor - 1);
    }

    private void recordLedger(
            Account origin,
            Account destination,
            double amount,
            String operationType,
            String notes) {
        Transaction tx = new Transaction();
        tx.setIdOrigin(origin);
        tx.setIdDestination(destination);
        tx.setBalance(amount);
        tx.setState("COMPLETED");
        tx.setCurrency(Currency.ARS);
        tx.setOperationType(operationType);
        tx.setNotes(notes);
        transactionRepository.save(tx);
    }

    private void validateRequest(double principal, int installments) {
        if (principal < MIN_PRINCIPAL || principal > MAX_PRINCIPAL) {
            throw new IllegalArgumentException(
                    "El monto debe estar entre $" + (int) MIN_PRINCIPAL + " y $" + (int) MAX_PRINCIPAL);
        }
        if (!ALLOWED_INSTALLMENTS.contains(installments)) {
            throw new IllegalArgumentException("Solo se permiten 3, 6 o 12 cuotas.");
        }
    }

    private List<InstallmentPlan> buildSchedule(double cuota, int n, LocalDate start) {
        List<InstallmentPlan> list = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            LocalDate due = start.plusMonths(i);
            list.add(new InstallmentPlan(i, due.format(DAY), cuota));
        }
        return list;
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
