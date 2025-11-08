package com.EDJ.ArCash.Service;

import com.EDJ.ArCash.Models.User;
import org.springframework.beans.factory.annotation.Value; // <-- ¡IMPORTADO!
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import java.util.List;
import java.util.Map;


@Service
public class EmailService {
    private final WebClient webClient;
    private final SpringTemplateEngine templateEngine;

    // === 1. INYECTAR LA URL DE PRODUCCIÓN ===
    // (Asegúrate de tener 'app.frontend.url=https://arcash.me' en tu application.properties)
    @Value("${app.frontend.url}")
    private String frontendUrl;

    public EmailService(WebClient resendWebClient, SpringTemplateEngine templateEngine) {
        this.webClient = resendWebClient;
        this.templateEngine = templateEngine;
    }

    @Async
    public void sendVerificationEmail(User user, String token) {
        // === 2. CREAR LAS VARIABLES Y LA URL COMPLETA ===
        Map<String, Object> variables = Map.of(
                "username", user.getName(),
                "token", token,
                // Armamos la URL completa aquí, en el backend
                "validationUrl", frontendUrl + "/validate?token=" + token
        );

        sendEmail(user.getEmail(), "¡Bienvenido a ArCashApp, " + user.getName() + "!",
                "email", variables); // "email" es el nombre de tu plantilla (email.html)
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