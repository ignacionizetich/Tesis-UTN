import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { lastValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import { logger } from '../../shared/utils/logger';

export interface TaxCalculationResult {
  montoOriginal: number;
  moneda?: string;
  iva: number;
  alicuotaIva?: number;
  totalFinal: number;
  /** Solo USD */
  montoUsd?: number;
  precioDolar?: number;
  dolarCompra?: number;
  dolarVenta?: number;
  nombreCotizacion?: string | null;
  casa?: string | null;
  fechaActualizacion?: string | null;
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
        this.http.get<Record<string, unknown>>(
          `${this.baseUrl}/impuestos/calculateARS?montoARS=${amount}`
        )
      );
      if (!response) {
        throw new Error('No se recibió respuesta');
      }
      return this.mapResponse(response);
    } catch (error) {
      logger.error('Error calculando impuestos ARS:', error);
      throw error;
    }
  }

  async calculateTaxesUSD(amount: number): Promise<TaxCalculationResult> {
    try {
      const response = await lastValueFrom(
        this.http.get<Record<string, unknown>>(
          `${this.baseUrl}/impuestos/calculateUSD?montoUSD=${amount}`
        )
      );
      if (!response) {
        throw new Error('No se recibió respuesta');
      }
      return this.mapResponse(response);
    } catch (error) {
      logger.error('Error calculando impuestos USD:', error);
      throw error;
    }
  }

  private mapResponse(response: Record<string, unknown>): TaxCalculationResult {
    const num = (key: string): number | undefined => {
      const v = response[key];
      return typeof v === 'number' ? v : undefined;
    };
    const str = (key: string): string | null | undefined => {
      const v = response[key];
      if (v == null) {
        return v as null | undefined;
      }
      return typeof v === 'string' ? v : String(v);
    };

    return {
      montoOriginal: num('montoOriginal') ?? 0,
      moneda: str('moneda') ?? undefined,
      iva: num('iva') ?? num('IVA') ?? 0,
      alicuotaIva: num('alicuotaIva'),
      totalFinal: num('totalFinal') ?? 0,
      montoUsd: num('montoUsd'),
      precioDolar: num('precioDolar'),
      dolarCompra: num('dolarCompra'),
      dolarVenta: num('dolarVenta') ?? num('precioDolar'),
      nombreCotizacion: str('nombreCotizacion'),
      casa: str('casa'),
      fechaActualizacion: str('fechaActualizacion'),
    };
  }
}
