package com.EDJ.ArCash.DTO.AuthDTO;

import java.util.List;

public record AdminMetricsResponse(
        MetricsSummary summary,
        List<NamedCount> usersByStatus,
        List<NamedCount> usersByRole,
        List<NamedCount> accountsByCurrency,
        List<NamedCount> transactionsByType,
        List<NamedCount> transactionsByCurrency,
        List<NamedMoney> volumeByType,
        List<TimePoint> registrationsLast14Days,
        List<TimePoint> transactionsLast14Days,
        List<TimePoint> volumeLast14Days,
        List<NamedCount> loansByStatus,
        List<NamedCount> cardsByStatus,
        LoanMetrics loans,
        String generatedAt
) {
    public record MetricsSummary(
            long totalUsers,
            long activeUsers,
            long inactiveUsers,
            long enabledUsers,
            long adminUsers,
            long totalAccounts,
            long arsAccounts,
            long usdAccounts,
            double totalBalanceArs,
            double totalBalanceUsd,
            long totalTransactions,
            double totalVolumeArs,
            double totalVolumeUsd,
            long transactionsToday,
            long newUsersToday,
            long activeLoans,
            long paidLoans,
            double loanPrincipalOutstanding,
            long totalCards,
            long activeCards
    ) {}

    public record NamedCount(String name, long value) {}

    public record NamedMoney(String name, double amount) {}

    public record TimePoint(String date, long count, double amount) {}

    public record LoanMetrics(
            long total,
            long active,
            long paidOff,
            double principalActive,
            double principalTotal,
            double installmentTotalActive,
            double estimatedInterestActive
    ) {}
}
