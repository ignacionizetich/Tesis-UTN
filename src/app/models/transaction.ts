export default interface Transaction {
  id: number;
  type: 'income' | 'expense';
  description: string;
  amount: number;
  date: Date;
  from?: string;
  to?: string;
  originId?: number;
  destinationId?: number;
  status?: 'COMPLETED' | 'FAILED';
  // Campos para transacciones USD
  currency?: 'ARS' | 'USD';
  exchangeRate?: number;
  amountInArs?: number;
  originalCurrency?: 'ARS' | 'USD';
}