export interface LoanRateItem {
  installments: number;
  monthlyRate: number;
  monthlyRatePercent: number;
}

export interface LoanRatesResponse {
  rates: LoanRateItem[];
  updatedAt: string | null;
}

export interface LoanRatesUpdateRequest {
  rates: Array<{
    installments: number;
    monthlyRatePercent: number;
  }>;
}
