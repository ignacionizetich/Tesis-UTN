export interface NamedCount {
  name: string;
  value: number;
}

export interface NamedMoney {
  name: string;
  amount: number;
}

export interface TimePoint {
  date: string;
  count: number;
  amount: number;
}

export interface MetricsSummary {
  totalUsers: number;
  activeUsers: number;
  inactiveUsers: number;
  enabledUsers: number;
  adminUsers: number;
  totalAccounts: number;
  arsAccounts: number;
  usdAccounts: number;
  totalBalanceArs: number;
  totalBalanceUsd: number;
  totalTransactions: number;
  totalVolumeArs: number;
  totalVolumeUsd: number;
  transactionsToday: number;
  newUsersToday: number;
  activeLoans: number;
  paidLoans: number;
  loanPrincipalOutstanding: number;
  totalCards: number;
  activeCards: number;
}

export interface LoanMetrics {
  total: number;
  active: number;
  paidOff: number;
  principalActive: number;
  principalTotal: number;
  installmentTotalActive: number;
  estimatedInterestActive: number;
}

export interface AdminMetrics {
  summary: MetricsSummary;
  usersByStatus: NamedCount[];
  usersByRole: NamedCount[];
  accountsByCurrency: NamedCount[];
  transactionsByType: NamedCount[];
  transactionsByCurrency: NamedCount[];
  volumeByType: NamedMoney[];
  registrationsLast14Days: TimePoint[];
  transactionsLast14Days: TimePoint[];
  volumeLast14Days: TimePoint[];
  loansByStatus: NamedCount[];
  cardsByStatus: NamedCount[];
  loans: LoanMetrics;
  generatedAt: string;
}
