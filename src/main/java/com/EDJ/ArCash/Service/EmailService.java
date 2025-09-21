package com.EDJ.ArCash.Service;

import com.EDJ.ArCash.Models.User;
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

    public EmailService(WebClient resendWebClient, SpringTemplateEngine templateEngine) {
        this.webClient = resendWebClient;
        this.templateEngine = templateEngine;
    }

    @Async
    public void sendVerificationEmail(User user, String token) {
        sendEmail(user.getEmail(), "¡Bienvenido a ArCashApp, " + user.getName() + "!",
                "email", Map.of("username", user.getName(), "token", token));
    }

    @Async
    public void sendRecoverPasswordEmail(User user, String token) {
        sendEmail(user.getEmail(), "Recupera tu contraseña en ArCash",
                "email-recover", Map.of("username", user.getName(), "token", token));
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
                    .block();

            System.out.println("Mail enviado a " + to);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
