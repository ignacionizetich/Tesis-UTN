package com.EDJ.ArCash.Service.result;

/**
 * Vista de saldo de una cuenta propia (showBalance).
 */
public record AccountBalanceView(double balance, String alias, String cvu) {

    public java.util.Map<String, Object> toResponseMap() {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("balance", balance);
        response.put("alias", alias);
        response.put("cvu", cvu);
        return response;
    }
}
