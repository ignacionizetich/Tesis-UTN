package com.EDJ.ArCash.Service.impl;
import com.EDJ.ArCash.Service.interfaces.MetricsService;

import com.EDJ.ArCash.DTO.AuthDTO.AdminMetricsResponse;
import com.EDJ.ArCash.DTO.AuthDTO.AdminMetricsResponse.*;
import com.EDJ.ArCash.Models.*;
import com.EDJ.ArCash.Models.Imp.CardStatus;
import com.EDJ.ArCash.Models.Imp.Currency;
import com.EDJ.ArCash.Models.Imp.LoanStatus;
import com.EDJ.ArCash.Models.Imp.Permissions;
import com.EDJ.ArCash.Repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MetricsServiceImpl implements MetricsService {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int SERIES_DAYS = 14;

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LoanRepository loanRepository;
    private final VirtualCardRepository virtualCardRepository;

    public MetricsServiceImpl(
            UserRepository userRepository,
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            LoanRepository loanRepository,
            VirtualCardRepository virtualCardRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.loanRepository = loanRepository;
        this.virtualCardRepository = virtualCardRepository;
    }

    @Transactional(readOnly = true)
    public AdminMetricsResponse collect() {
        List<User> users = userRepository.findAll();
        List<Account> accounts = accountRepository.findAll();
        List<Transaction> transactions = transactionRepository.findAll();
        List<Loan> loans = loanRepository.findAll();
        List<VirtualCard> cards = virtualCardRepository.findAll();

        LocalDate today = LocalDate.now();
        LocalDate seriesStart = today.minusDays(SERIES_DAYS - 1L);

        long activeUsers = users.stream().filter(User::isActive).count();
        long inactiveUsers = users.size() - activeUsers;
        long enabledUsers = users.stream().filter(User::isEnabled).count();
        long adminUsers = users.stream()
                .filter(u -> u.getPermissions() == Permissions.ADMIN)
                .count();

        long arsAccounts = accounts.stream().filter(a -> a.getAccountType() == Currency.ARS).count();
        long usdAccounts = accounts.stream().filter(a -> a.getAccountType() == Currency.USD).count();
        double totalBalanceArs = accounts.stream()
                .filter(a -> a.getAccountType() == Currency.ARS)
                .mapToDouble(Account::getBalance)
                .sum();
        double totalBalanceUsd = accounts.stream()
                .filter(a -> a.getAccountType() == Currency.USD)
                .mapToDouble(Account::getBalance)
                .sum();

        double totalVolumeArs = transactions.stream()
                .filter(t -> resolveCurrency(t) == Currency.ARS)
                .mapToDouble(Transaction::getBalance)
                .sum();
        double totalVolumeUsd = transactions.stream()
                .filter(t -> resolveCurrency(t) == Currency.USD)
                .mapToDouble(Transaction::getBalance)
                .sum();

        long transactionsToday = transactions.stream()
                .map(this::parseTxDate)
                .filter(Objects::nonNull)
                .filter(d -> d.equals(today))
                .count();
        long newUsersToday = users.stream()
                .map(this::parseUserDate)
                .filter(Objects::nonNull)
                .filter(d -> d.equals(today))
                .count();

        long activeLoans = loans.stream().filter(l -> l.getStatus() == LoanStatus.ACTIVE).count();
        long paidLoans = loans.stream().filter(l -> l.getStatus() == LoanStatus.PAID_OFF).count();
        double loanPrincipalOutstanding = loans.stream()
                .filter(l -> l.getStatus() == LoanStatus.ACTIVE)
                .mapToDouble(Loan::getPrincipal)
                .sum();

        long activeCards = cards.stream().filter(c -> c.getStatus() == CardStatus.ACTIVE).count();

        MetricsSummary summary = new MetricsSummary(
                users.size(),
                activeUsers,
                inactiveUsers,
                enabledUsers,
                adminUsers,
                accounts.size(),
                arsAccounts,
                usdAccounts,
                round2(totalBalanceArs),
                round2(totalBalanceUsd),
                transactions.size(),
                round2(totalVolumeArs),
                round2(totalVolumeUsd),
                transactionsToday,
                newUsersToday,
                activeLoans,
                paidLoans,
                round2(loanPrincipalOutstanding),
                cards.size(),
                activeCards
        );

        List<NamedCount> usersByStatus = List.of(
                new NamedCount("Activos", activeUsers),
                new NamedCount("Inactivos", inactiveUsers)
        );

        long regularUsers = users.size() - adminUsers;
        List<NamedCount> usersByRole = List.of(
                new NamedCount("Usuarios", regularUsers),
                new NamedCount("Admins", adminUsers)
        );

        List<NamedCount> accountsByCurrency = List.of(
                new NamedCount("ARS", arsAccounts),
                new NamedCount("USD", usdAccounts)
        );

        Map<String, Long> txTypeCounts = new LinkedHashMap<>();
        Map<String, Double> txTypeVolume = new LinkedHashMap<>();
        for (Transaction tx : transactions) {
            String type = normalizeOpType(tx.getOperationType());
            txTypeCounts.merge(type, 1L, Long::sum);
            txTypeVolume.merge(type, tx.getBalance() != null ? tx.getBalance() : 0.0, Double::sum);
        }
        List<NamedCount> transactionsByType = txTypeCounts.entrySet().stream()
                .map(e -> new NamedCount(labelOpType(e.getKey()), e.getValue()))
                .collect(Collectors.toList());
        List<NamedMoney> volumeByType = txTypeVolume.entrySet().stream()
                .map(e -> new NamedMoney(labelOpType(e.getKey()), round2(e.getValue())))
                .collect(Collectors.toList());

        long txArs = transactions.stream().filter(t -> resolveCurrency(t) == Currency.ARS).count();
        long txUsd = transactions.stream().filter(t -> resolveCurrency(t) == Currency.USD).count();
        List<NamedCount> transactionsByCurrency = List.of(
                new NamedCount("ARS", txArs),
                new NamedCount("USD", txUsd)
        );

        Map<LocalDate, Long> regByDay = emptyDayMap(seriesStart, today);
        for (User user : users) {
            LocalDate d = parseUserDate(user);
            if (d != null && !d.isBefore(seriesStart) && !d.isAfter(today)) {
                regByDay.merge(d, 1L, Long::sum);
            }
        }

        Map<LocalDate, Long> txByDay = emptyDayMap(seriesStart, today);
        Map<LocalDate, Double> volumeByDay = emptyAmountDayMap(seriesStart, today);
        for (Transaction tx : transactions) {
            LocalDate d = parseTxDate(tx);
            if (d != null && !d.isBefore(seriesStart) && !d.isAfter(today)) {
                txByDay.merge(d, 1L, Long::sum);
                volumeByDay.merge(d, tx.getBalance() != null ? tx.getBalance() : 0.0, Double::sum);
            }
        }

        List<TimePoint> registrationsLast14Days = toTimePoints(regByDay, volumeByDay, false);
        List<TimePoint> transactionsLast14Days = toTimePoints(txByDay, volumeByDay, false);
        List<TimePoint> volumeLast14Days = toTimePoints(txByDay, volumeByDay, true);

        Map<LoanStatus, Long> loanStatusCounts = Arrays.stream(LoanStatus.values())
                .collect(Collectors.toMap(s -> s, s -> 0L, (a, b) -> a, LinkedHashMap::new));
        for (Loan loan : loans) {
            LoanStatus status = loan.getStatus() != null ? loan.getStatus() : LoanStatus.ACTIVE;
            loanStatusCounts.merge(status, 1L, Long::sum);
        }
        List<NamedCount> loansByStatus = loanStatusCounts.entrySet().stream()
                .map(e -> new NamedCount(labelLoanStatus(e.getKey()), e.getValue()))
                .collect(Collectors.toList());

        Map<CardStatus, Long> cardStatusCounts = Arrays.stream(CardStatus.values())
                .collect(Collectors.toMap(s -> s, s -> 0L, (a, b) -> a, LinkedHashMap::new));
        for (VirtualCard card : cards) {
            CardStatus status = card.getStatus() != null ? card.getStatus() : CardStatus.ACTIVE;
            cardStatusCounts.merge(status, 1L, Long::sum);
        }
        List<NamedCount> cardsByStatus = cardStatusCounts.entrySet().stream()
                .map(e -> new NamedCount(labelCardStatus(e.getKey()), e.getValue()))
                .collect(Collectors.toList());

        double principalActive = loans.stream()
                .filter(l -> l.getStatus() == LoanStatus.ACTIVE)
                .mapToDouble(Loan::getPrincipal)
                .sum();
        double principalTotal = loans.stream().mapToDouble(Loan::getPrincipal).sum();
        double installmentTotalActive = loans.stream()
                .filter(l -> l.getStatus() == LoanStatus.ACTIVE)
                .mapToDouble(l -> l.getInstallmentAmount() * l.getInstallmentCount())
                .sum();
        double estimatedInterestActive = Math.max(0, installmentTotalActive - principalActive);

        LoanMetrics loanMetrics = new LoanMetrics(
                loans.size(),
                activeLoans,
                paidLoans,
                round2(principalActive),
                round2(principalTotal),
                round2(installmentTotalActive),
                round2(estimatedInterestActive)
        );

        return new AdminMetricsResponse(
                summary,
                usersByStatus,
                usersByRole,
                accountsByCurrency,
                transactionsByType,
                transactionsByCurrency,
                volumeByType,
                registrationsLast14Days,
                transactionsLast14Days,
                volumeLast14Days,
                loansByStatus,
                cardsByStatus,
                loanMetrics,
                LocalDateTime.now().format(TS)
        );
    }

    private Map<LocalDate, Long> emptyDayMap(LocalDate from, LocalDate to) {
        Map<LocalDate, Long> map = new LinkedHashMap<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            map.put(d, 0L);
        }
        return map;
    }

    private Map<LocalDate, Double> emptyAmountDayMap(LocalDate from, LocalDate to) {
        Map<LocalDate, Double> map = new LinkedHashMap<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            map.put(d, 0.0);
        }
        return map;
    }

    private List<TimePoint> toTimePoints(
            Map<LocalDate, Long> counts,
            Map<LocalDate, Double> amounts,
            boolean preferAmount) {
        List<TimePoint> points = new ArrayList<>();
        for (LocalDate date : counts.keySet()) {
            long count = counts.getOrDefault(date, 0L);
            double amount = round2(amounts.getOrDefault(date, 0.0));
            points.add(new TimePoint(date.toString(), count, preferAmount ? amount : amount));
        }
        return points;
    }

    private Currency resolveCurrency(Transaction tx) {
        if (tx.getCurrency() != null) {
            return tx.getCurrency();
        }
        if (tx.getIdOrigin() != null && tx.getIdOrigin().getAccountType() != null) {
            return tx.getIdOrigin().getAccountType();
        }
        return Currency.ARS;
    }

    private String normalizeOpType(String raw) {
        if (raw == null || raw.isBlank()) {
            return "TRANSFER";
        }
        return raw.trim().toUpperCase(Locale.ROOT);
    }

    private String labelOpType(String type) {
        return switch (type) {
            case "TRANSFER" -> "Transferencias";
            case "BUY_USD" -> "Compra USD";
            case "SELL_USD" -> "Venta USD";
            case "LOAN_CREDIT" -> "Préstamos";
            case "LOAN_PAYMENT" -> "Pago de cuotas";
            default -> type;
        };
    }

    private String labelLoanStatus(LoanStatus status) {
        return switch (status) {
            case ACTIVE -> "Activos";
            case PAID_OFF -> "Cancelados";
        };
    }

    private String labelCardStatus(CardStatus status) {
        return switch (status) {
            case ACTIVE -> "Activas";
            case PAUSED -> "Pausadas";
            case CANCELLED -> "Canceladas";
        };
    }

    private LocalDate parseUserDate(User user) {
        return parseDate(user.getCreationDate());
    }

    private LocalDate parseTxDate(Transaction tx) {
        return parseDate(tx.getTransaction_date());
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            if (value.length() >= 19) {
                return LocalDateTime.parse(value.substring(0, 19), TS).toLocalDate();
            }
            if (value.length() >= 10) {
                return LocalDate.parse(value.substring(0, 10));
            }
        } catch (DateTimeParseException ignored) {
            return null;
        }
        return null;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
