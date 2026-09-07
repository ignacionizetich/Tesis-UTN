package com.EDJ.ArCash.Service.result;

/**
 * Saldo de una cuenta propia, tal como lo consume el frontend.
 *
 * <p>Se serializa directamente a JSON: los componentes del record son las claves de la
 * respuesta ({@code balance}, {@code alias}, {@code cvu}).
 */
public record AccountBalanceView(double balance, String alias, String cvu) {
}
