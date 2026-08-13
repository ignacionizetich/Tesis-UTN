export type LoanStatus = 'ACTIVE' | 'PAID_OFF';
export type LoanInstallmentStatus = 'PENDING' | 'PAID';

export interface LoanInstallment {
  id?: number;
  number: number;
  dueDate: string;
  amount: number;
  status: LoanInstallmentStatus | string;
  paidAt?: string | null;
}

export interface LoanSimulation {
  principal: number;
  installments: number;
  monthlyRate: number;
  monthlyRatePercent: number;
  installmentAmount: number;
  totalAmount: number;
  totalInterest: number;
  schedule: LoanInstallment[];
}

export interface LoanSummary {
  id: number;
  principal: number;
  installmentCount: number;
  installmentAmount: number;
  totalAmount: number;
  totalInterest: number;
  monthlyRatePercent: number;
  status: LoanStatus | string;
  createdAt: string;
  paidCount: number;
  pendingCount: number;
  nextInstallmentAmount?: number | null;
  nextDueDate?: string | null;
}

export interface LoanDetail {
  id: number;
  principal: number;
  installmentCount: number;
  installmentAmount: number;
  totalAmount: number;
  totalInterest: number;
  monthlyRatePercent: number;
  status: LoanStatus | string;
  createdAt: string;
  installments: LoanInstallment[];
  paidCount?: number;
  pendingCount?: number;
}
