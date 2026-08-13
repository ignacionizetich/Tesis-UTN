import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { lastValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import { logger } from '../../shared/utils/logger';

export interface TaxCalculationResult {
  montoOriginal: number;
  iva: number;
  precioDolar?: number;
  totalFinal: number;
}

@Injectable({
  providedIn: 'root',
})
export class TaxApi {
  private readonly baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  async calculateTaxesARS(amount: number): Promise<TaxCalculationResult> {
    try {
      const response = await lastValueFrom(
        this.http.get<any>(`${this.baseUrl}/impuestos/calculateARS?montoARS=${amount}`)
      );
      if (!response) {
        throw new Error('No se recibió respuesta');
      }
      return {
        montoOriginal: response.montoOriginal,
        iva: response.iva,
        totalFinal: response.totalFinal,
      };
    } catch (error) {
      logger.error('Error calculando impuestos ARS:', error);
      throw error;
    }
  }

  async calculateTaxesUSD(amount: number): Promise<TaxCalculationResult> {
    try {
      const response = await lastValueFrom(
        this.http.get<any>(`${this.baseUrl}/impuestos/calculateUSD?montoUSD=${amount}`)
      );
      if (!response) {
        throw new Error('No se recibió respuesta');
      }
      return {
        montoOriginal: response.montoOriginal,
        iva: response.iva,
        precioDolar: response.precioDolar,
        totalFinal: response.totalFinal,
      };
    } catch (error) {
      logger.error('Error calculando impuestos USD:', error);
      throw error;
    }
  }
}
