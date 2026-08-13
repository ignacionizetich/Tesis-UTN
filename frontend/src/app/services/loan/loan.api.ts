import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { lastValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LoanDetail, LoanSimulation, LoanSummary } from '../../models/loan';
import { LoanRatesResponse } from '../../models/loan-rates';
import { logger } from '../../shared/utils/logger';

@Injectable({
  providedIn: 'root',
})
export class LoanApi {
  private readonly baseUrl = `${environment.apiUrl}/loans`;

  constructor(private http: HttpClient) {}

  list(): Promise<LoanSummary[]> {
    return lastValueFrom(this.http.get<LoanSummary[]>(this.baseUrl));
  }

  rates(): Promise<LoanRatesResponse> {
    return lastValueFrom(this.http.get<LoanRatesResponse>(`${this.baseUrl}/rates`));
  }

  simulate(principal: number, installments: number): Promise<LoanSimulation> {
    return lastValueFrom(
      this.http.post<LoanSimulation>(`${this.baseUrl}/simulate`, { principal, installments })
    );
  }

  accept(principal: number, installments: number): Promise<LoanDetail> {
    return lastValueFrom(
      this.http.post<LoanDetail>(this.baseUrl, { principal, installments })
    );
  }

  detail(loanId: number): Promise<LoanDetail> {
    return lastValueFrom(this.http.get<LoanDetail>(`${this.baseUrl}/${loanId}`));
  }

  payNext(loanId: number): Promise<LoanDetail> {
    return lastValueFrom(this.http.post<LoanDetail>(`${this.baseUrl}/${loanId}/pay`, {}));
  }

  handleError(error: unknown, fallback: string): string {
    logger.error(fallback, error);
    const err = error as { error?: { message?: string; error?: string }; message?: string };
    return err?.error?.error || err?.error?.message || err?.message || fallback;
  }
}
