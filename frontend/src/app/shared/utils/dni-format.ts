/**
 * Formatea un DNI argentino con puntos de miles.
 * Ej: "45462201" → "45.462.201"
 */
export function formatDni(dni: string | number | null | undefined): string {
  if (dni == null) {
    return '';
  }
  const digits = String(dni).replace(/\D/g, '');
  if (!digits) {
    return '';
  }
  return digits.replace(/\B(?=(\d{3})+(?!\d))/g, '.');
}
