/** Movimiento de la billetera (mapeado desde TransactionDTO). */
export type TransactionKind =
  | 'transfer'
  | 'buy_usd'
  | 'sell_usd'
  | 'loan_credit'
  | 'loan_payment';

export default interface Transaction {
  id: number;
  type: 'income' | 'expense';
  kind: TransactionKind;
  /** Título principal de la fila / detalle. */
  description: string;
  /** Línea secundaria (fecha aparte; acá cotización, alias, etc.). */
  subtitle?: string;
  amount: number;
  date: Date;
  from?: string;
  to?: string;
  counterpartyName?: string;
  originId?: number;
  destinationId?: number;
  status?: 'COMPLETED' | 'FAILED';
  currency?: 'ARS' | 'USD';
  exchangeRate?: number;
  originalAmount?: number;
  originalCurrency?: 'ARS' | 'USD';
  amountInArs?: number;
  taxAmount?: number;
  taxPercentage?: number;
  converted?: boolean;
  sameOwner?: boolean;
  idOperation?: string;
  operationType?: string;
  notes?: string;
}
