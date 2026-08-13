export interface TransferData {
  idaccount: string | number;
  alias: string;
  cvu: string;
  currency?: 'ARS' | 'USD';
  user: {
    nombre: string;
    apellido: string;
    dni: string;
    email?: string;
  };
  isFromFavorite?: boolean;
  favoriteId?: number;
}

export interface TransferRequest {
  accountId: number;
  amount: number;
}

export interface TransferValidation {
  isValid: boolean;
  message?: string;
  accountId?: number;
}

export interface TaxCalculation {
  montoOriginal: number;
  iva: number;
  totalFinal: number;
  precioDolar?: number;
}
