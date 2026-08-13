import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import qrData from '../../models/qrData';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class QrApi {
  private readonly baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getMyQrData(accountId: number): Observable<qrData> {
    return this.http.get<qrData>(`${this.baseUrl}/accounts/${accountId}/qr-data`);
  }
}
