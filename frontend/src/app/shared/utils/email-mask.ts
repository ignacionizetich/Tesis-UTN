/**
 * Enmascara un email mostrando solo los primeros caracteres del local-part.
 * Ejemplo: ab***@dominio.com
 */
export function maskEmail(email: string): string {
  const [usuario, dominio] = email.split('@');
  if (!usuario || !dominio) {
    return email;
  }
  if (usuario.length <= 2) {
    return `${usuario[0]}***@${dominio}`;
  }
  return `${usuario.slice(0, 2)}***@${dominio}`;
}

/** @deprecated Usar maskEmail. Alias legacy. */
export function censurarCorreo(email: string): string {
  return maskEmail(email);
}
