package com.EDJ.ArCash.Service.impl;
import com.EDJ.ArCash.Service.interfaces.EmailService;

import com.EDJ.ArCash.Models.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value; // <-- ¡IMPORTADO!
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {
    private final WebClient webClient;
    private final SpringTemplateEngine templateEngine;

    // === 1. INYECTAR LA URL DE PRODUCCIÓN ===
    @Value("${app.frontend.url}")
    private String frontendUrl;



    @Async
    public void sendVerificationEmail(User user, String token) {
        // === 2. CREAR LAS VARIABLES Y LA URL COMPLETA ===
        Map<String, Object> variables = Map.of(
                "username", user.getName(),
                "token", token,

                "validationUrl", frontendUrl + "/validate?token=" + token
        );

        sendEmail(user.getEmail(), "¡Bienvenido a ArCashApp, " + user.getName() + "!",
                "email", variables);
    }

    @Async
    public void sendRecoverPasswordEmail(User user, String token) {
        // === 2. CREAR LAS VARIABLES Y LA URL COMPLETA ===
        Map<String, Object> variables = Map.of(
                "username", user.getName(),
                "token", token,
                // Armamos la URL completa aquí, en el backend
                "recoverUrl", frontendUrl + "/reset-password?token=" + token
        );

        sendEmail(user.getEmail(), "Recupera tu contraseña en ArCash",
                "email-recover", variables); // "email-recover" es tu plantilla (email-recover.html)
    }

    @Async
    public void sendTransactionCompletedEmail(User user, double amount, String destinationAlias,
                                             String currency, boolean converted, Double amountUsd, Double exchangeRate,
                                             Double taxAmount, Double taxPercentage, Double totalDebitado,
                                             String operationType) {
        Map<String, Object> variables = new java.util.HashMap<>();
        variables.put("username", user.getName());
        variables.put("amount", amount);
        variables.put("destinationAlias", destinationAlias);
        variables.put("transactionDate", java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        variables.put("currency", currency != null ? currency : "ARS");
        variables.put("converted", converted);
        variables.put("amountUsd", amountUsd);
        variables.put("exchangeRate", exchangeRate);
        variables.put("taxAmount", taxAmount);
        variables.put("taxPercentage", taxPercentage);
        variables.put("totalDebitado", totalDebitado);
        variables.put("operationType", operationType != null ? operationType : "TRANSFER");

        boolean ownOperation = isOwnOperation(operationType);
        String template = ownOperation ? "email-own-operation" : "email-transaction";
        String subject = ownOperationSubject(operationType);

        sendEmail(user.getEmail(), subject, template, variables);
    }

    private static boolean isOwnOperation(String operationType) {
        return "BUY_USD".equals(operationType)
                || "SELL_USD".equals(operationType)
                || "CONVERSION".equals(operationType);
    }

    private static String ownOperationSubject(String operationType) {
        if ("BUY_USD".equals(operationType)) {
            return "Compra de dólares completada";
        }
        if ("SELL_USD".equals(operationType)) {
            return "Venta de dólares completada";
        }
        if ("CONVERSION".equals(operationType)) {
            return "Conversión completada";
        }
        return "Transacción completada exitosamente";
    }

    @Async
    public void sendAccountCreatedEmail(User user, String accountAlias, String accountCvu) {
        Map<String, Object> variables = Map.of(
                "username", user.getName(),
                "accountAlias", accountAlias,
                "accountCvu", accountCvu,
                "dashboardUrl", frontendUrl + "/dashboard"
        );

        sendEmail(user.getEmail(), "¡Tu cuenta ArCash ha sido creada!",
                "email-account-created", variables);
    }

    @Async
    public void sendUsdAccountCreatedEmail(User user, String accountAlias, String accountCvu){
        Map<String, Object> variables = Map.of(
                "username", user.getName(),
                "accountAlias", accountAlias,
                "accountCvu", accountCvu,
                "dashboardUrl", frontendUrl + "/dashboard"
        );

        sendEmail(user.getEmail(), "Tu cuenta en dolares ha sido creada!",
                "email-usd-account-created", variables);
    }

    @Async
    public void sendAliasChangedEmail(User user, String oldAlias, String newAlias) {
        Map<String, Object> variables = Map.of(
                "username", user.getName(),
                "oldAlias", oldAlias,
                "newAlias", newAlias
        );

        sendEmail(user.getEmail(), "Alias de cuenta actualizado",
                "email-alias-changed", variables);
    }

    @Async
    public void sendPasswordChangedEmail(User user) {
        Map<String, Object> variables = Map.of(
                "username", user.getName()
        );

        sendEmail(user.getEmail(), "Contraseña actualizada",
                "email-password-changed", variables);
    }

    private void sendEmail(String to, String subject, String template, Map<String, Object> variables) {
        try {
            Context context = new Context();
            context.setVariables(variables);
            String html = templateEngine.process(template, context);

            Map<String, Object> body = Map.of(
                    "from", "no-reply@arcash.me",
                    "to", List.of(to),
                    "subject", subject,
                    "html", html
            );

            webClient.post()
                    .uri("/emails")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnError(e -> System.err.println("Error enviando mail: " + e.getMessage()))
                    .block(); // Usar block aquí puede no ser ideal en un método @Async,
            // pero es funcional para este ejemplo.
            // Considera usar .subscribe() si quieres mantenerlo 100% reactivo.

        } catch (Exception e) {
            e.printStackTrace(); // Manejo de errores simple, considera un logger
        }
    }
}
