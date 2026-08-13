package com.EDJ.ArCash.Service;

/**
 * Vista de saldo de una cuenta propia (showBalance).
 */
public record AccountBalanceView(double balance, String alias, String cvu) {
}
