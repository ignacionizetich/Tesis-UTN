/** Formato monetario compacto es-AR (sin símbolo de moneda). */
export function formatMoney(value: number | null | undefined): string {
  if (value == null || isNaN(value)) {
    return '0';
  }
  if (value % 1 === 0) {
    return value.toLocaleString('es-AR');
  }
  return value.toLocaleString('es-AR', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
  });
}

/** Formato currency ARS completo (Intl). */
export function formatCurrencyArs(amount: number): string {
  return new Intl.NumberFormat('es-AR', {
    style: 'currency',
    currency: 'ARS',
  }).format(amount);
}

/** Abrevia montos grandes (1.2K / 1.5M). */
export function formatCompactNumber(value: number): string {
  if (value >= 1_000_000) {
    return (value / 1_000_000).toFixed(1) + 'M';
  }
  if (value >= 1_000) {
    return (value / 1_000).toFixed(1) + 'K';
  }
  return value.toLocaleString('es-AR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}
