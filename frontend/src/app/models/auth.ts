/**
 * Respuestas de los endpoints de autenticación, tal como las define el backend.
 *
 * Tenerlas tipadas evita el `post<any>` que dejaba pasar cualquier acceso: si el backend
 * renombra un campo, el compilador marca los usos en lugar de fallar en runtime con undefined.
 */

/** Respuesta de `POST /auth/login` (`LoginResponse`). */
export interface LoginResponse {
  success: boolean;
  message: string;
  accessToken: string;
  refreshToken: string;
  accountId: number;
  role: string;
}

/** Respuesta de `POST /auth/refresh` (`RefreshTokenResponse`). */
export interface RefreshTokenResponse {
  accessToken: string;
}

/** Respuesta de las operaciones que solo informan un resultado (`ApiMessageResponse`). */
export interface ApiMessageResponse {
  success: boolean;
  message: string;
}

/** Respuesta de `POST /user/create` (`RegisterResponse`). */
export interface RegisterResponse {
  success: boolean;
  message: string;
}
