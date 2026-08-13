package com.EDJ.ArCash.Service.interfaces;

import com.EDJ.ArCash.Models.User;

public interface EmailService {
    public void sendVerificationEmail(User user, String token);

    public void sendRecoverPasswordEmail(User user, String token);

    public void sendTransactionCompletedEmail(User user, double amount, String destinationAlias,
                                                 String currency, boolean converted, Double amountUsd, Double exchangeRate,
                                                 Double taxAmount, Double taxPercentage, Double totalDebitado,
                                                 String operationType);

    public void sendAccountCreatedEmail(User user, String accountAlias, String accountCvu);

    public void sendUsdAccountCreatedEmail(User user, String accountAlias, String accountCvu);

    public void sendAliasChangedEmail(User user, String oldAlias, String newAlias);

    public void sendPasswordChangedEmail(User user);

}
