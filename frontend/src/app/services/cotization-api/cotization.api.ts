import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { lastValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import { logger } from '../../shared/utils/logger';

/** Respuesta de GET /api/cotizacion/dolar (dolarapi oficial). */
export interface CotizacionDolar {
  moneda: string;
  casa: string | null;
  nombre: string | null;
  compra: number;
  venta: number;
  fechaActualizacion: string | null;
}

@Injectable({
  providedIn: 'root',
})
export class CotizationApi {
  private readonly baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  async getDolarOficial(): Promise<CotizacionDolar> {
    try {
      const response = await lastValueFrom(
        this.http.get<CotizacionDolar>(`${this.baseUrl}/cotizacion/dolar`)
      );
      if (!response || !(response.venta > 0)) {
        throw new Error('Cotización inválida');
      }
      return {
        moneda: response.moneda || 'USD',
        casa: response.casa ?? null,
        nombre: response.nombre ?? null,
        compra: response.compra ?? 0,
        venta: response.venta,
        fechaActualizacion: response.fechaActualizacion ?? null,
      };
    } catch (error) {
      logger.error('Error obteniendo cotización del dólar:', error);
      throw error;
    }
  }
}
