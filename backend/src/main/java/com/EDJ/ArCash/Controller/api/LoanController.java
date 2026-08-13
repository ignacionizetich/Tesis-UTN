package com.EDJ.ArCash.Controller.api;

import com.EDJ.ArCash.DTO.AuthDTO.*;
import com.EDJ.ArCash.Models.Loan;
import com.EDJ.ArCash.Models.LoanInstallment;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Models.Imp.LoanInstallmentStatus;
import com.EDJ.ArCash.Security.CustomUserDetails;
import com.EDJ.ArCash.Service.interfaces.LoanRateConfigService;
import com.EDJ.ArCash.Service.interfaces.LoanService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/loans", produces = "application/json")
@Tag(name = "Préstamos", description = "Préstamos simulados en ARS")
public class LoanController {

    private final LoanService loanService;
    private final LoanRateConfigService loanRateConfigService;

    public LoanController(LoanService loanService, LoanRateConfigService loanRateConfigService) {
        this.loanService = loanService;
        this.loanRateConfigService = loanRateConfigService;
    }

    @GetMapping
    public ResponseEntity<List<LoanSummaryResponse>> list(
            @AuthenticationPrincipal CustomUserDetails principal) {
        List<LoanSummaryResponse> loans = loanService.listForUser(principal.getUser().getId()).stream()
                .map(this::toSummary)
                .toList();
        return ResponseEntity.ok(loans);
    }

    @GetMapping("/rates")
    public ResponseEntity<LoanRatesResponse> rates() {
        return ResponseEntity.ok(loanRateConfigService.listRates());
    }

    @PostMapping("/simulate")
    public ResponseEntity<?> simulate(@RequestBody LoanSimulateRequest request) {
        try {
            LoanService.SimulationResult sim =
                    loanService.simulate(request.getPrincipal(), request.getInstallments());
            return ResponseEntity.ok(toSimulation(sim));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> accept(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody LoanSimulateRequest request) {
        try {
            Loan loan = loanService.accept(
                    principal.getUser(), request.getPrincipal(), request.getInstallments());
            return ResponseEntity.ok(toDetail(loan));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/{loanId}")
    public ResponseEntity<?> detail(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long loanId) {
        return loanService.findOwned(loanId, principal.getUser().getId())
                .<ResponseEntity<?>>map(loan -> ResponseEntity.ok(toDetail(loan)))
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "Préstamo no encontrado")));
    }

    @PostMapping("/{loanId}/pay")
    public ResponseEntity<?> payNext(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long loanId) {
        User user = principal.getUser();
        return loanService.findOwned(loanId, user.getId())
                .map(loan -> {
                    try {
                        Loan updated = loanService.payNext(loan);
                        return ResponseEntity.ok(toDetail(updated));
                    } catch (IllegalStateException ex) {
                        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
                    }
                })
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "Préstamo no encontrado")));
    }

    private LoanSimulationResponse toSimulation(LoanService.SimulationResult sim) {
        return LoanSimulationResponse.builder()
                .principal(sim.principal())
                .installments(sim.installments())
                .monthlyRate(sim.monthlyRate())
                .monthlyRatePercent(sim.monthlyRate() * 100)
                .installmentAmount(sim.installmentAmount())
                .totalAmount(sim.totalAmount())
                .totalInterest(sim.totalInterest())
                .schedule(sim.schedule().stream()
                        .map(p -> LoanInstallmentResponse.builder()
                                .number(p.number())
                                .dueDate(p.dueDate())
                                .amount(p.amount())
                                .status("PENDING")
                                .build())
                        .toList())
                .build();
    }

    private LoanSummaryResponse toSummary(Loan loan) {
        long paid = loan.getInstallments().stream()
                .filter(i -> i.getStatus() == LoanInstallmentStatus.PAID)
                .count();
        long pending = loan.getInstallments().size() - paid;
        LoanInstallment next = loan.getInstallments().stream()
                .filter(i -> i.getStatus() == LoanInstallmentStatus.PENDING)
                .findFirst()
                .orElse(null);
        return LoanSummaryResponse.builder()
                .id(loan.getId())
                .principal(loan.getPrincipal())
                .installmentCount(loan.getInstallmentCount())
                .installmentAmount(loan.getInstallmentAmount())
                .totalAmount(loan.getTotalAmount())
                .totalInterest(round2(loan.getTotalAmount() - loan.getPrincipal()))
                .monthlyRatePercent(loan.getMonthlyRate() * 100)
                .status(loan.getStatus().name())
                .createdAt(loan.getCreatedAt())
                .paidCount((int) paid)
                .pendingCount((int) pending)
                .nextInstallmentAmount(next != null ? next.getAmount() : null)
                .nextDueDate(next != null ? next.getDueDate() : null)
                .build();
    }

    private LoanDetailResponse toDetail(Loan loan) {
        return LoanDetailResponse.builder()
                .id(loan.getId())
                .principal(loan.getPrincipal())
                .installmentCount(loan.getInstallmentCount())
                .installmentAmount(loan.getInstallmentAmount())
                .totalAmount(loan.getTotalAmount())
                .totalInterest(round2(loan.getTotalAmount() - loan.getPrincipal()))
                .monthlyRatePercent(loan.getMonthlyRate() * 100)
                .status(loan.getStatus().name())
                .createdAt(loan.getCreatedAt())
                .installments(loan.getInstallments().stream()
                        .map(i -> LoanInstallmentResponse.builder()
                                .id(i.getId())
                                .number(i.getInstallmentNumber())
                                .dueDate(i.getDueDate())
                                .amount(i.getAmount())
                                .status(i.getStatus().name())
                                .paidAt(i.getPaidAt())
                                .build())
                        .toList())
                .build();
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
