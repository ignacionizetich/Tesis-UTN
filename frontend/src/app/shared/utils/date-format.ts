/** Fecha/hora corta es-AR. */
export function formatDateTime(date: Date | string | number): string {
  return new Intl.DateTimeFormat('es-AR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  }).format(new Date(date));
}

/** Fecha larga + hora (detalle de transacción). */
export function formatDateTimeDetailed(date: Date | string | number): string {
  const dateObj = new Date(date);
  const dateStr = dateObj.toLocaleDateString('es-AR', {
    weekday: 'long',
    day: '2-digit',
    month: 'long',
    year: 'numeric',
  });

  const timeStr = dateObj.toLocaleTimeString('es-AR', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  });

  return `${dateStr} a las ${timeStr}`;
}
