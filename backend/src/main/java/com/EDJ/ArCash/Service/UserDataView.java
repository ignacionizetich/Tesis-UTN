package com.EDJ.ArCash.Service;

/**
 * Perfil del autenticado para GET /api/user/data.
 * username = User.alias; alias = Account.nickname (contrato historico).
 */
public final class UserDataView {

    private final String name;
    private final String lastName;
    private final String dni;
    private final String email;
    private final String username;
    private final String alias;
    private final Long idAccount;
    private final String cvu;
    private final double balance;

    public UserDataView(String name, String lastName, String dni, String email,
                        String username, String alias, Long idAccount, String cvu, double balance) {
        this.name = name;
        this.lastName = lastName;
        this.dni = dni;
        this.email = email;
        this.username = username;
        this.alias = alias;
        this.idAccount = idAccount;
        this.cvu = cvu;
        this.balance = balance;
    }

    public String getName() {
        return name;
    }

    public String getLastName() {
        return lastName;
    }

    public String getDni() {
        return dni;
    }

    public String getEmail() {
        return email;
    }

    public String getUsername() {
        return username;
    }

    public String getAlias() {
        return alias;
    }

    public Long getIdAccount() {
        return idAccount;
    }

    public String getCvu() {
        return cvu;
    }

    public double getBalance() {
        return balance;
    }
}
