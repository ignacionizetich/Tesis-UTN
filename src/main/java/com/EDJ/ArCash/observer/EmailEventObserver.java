package com.EDJ.ArCash.observer;

import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Service.EmailService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;


@Component
public class EmailEventObserver implements EventObserver {

    private final EmailService emailService;
    private final EventPublisher eventPublisher;

    // Tipos de eventos que este observer puede manejar
    private static final Set<EventType> HANDLED_EVENTS = Set.of(
            EventType.USER_REGISTERED,
            EventType.PASSWORD_RECOVERY_REQUESTED,
            EventType.TRANSACTION_COMPLETED,
            EventType.ACCOUNT_CREATED,
            EventType.USD_ACCOUNT_CREATED,
            EventType.ALIAS_CHANGED,
            EventType.PASSWORD_CHANGED
    );

    @Autowired
    public EmailEventObserver(EmailService emailService, EventPublisher eventPublisher) {
        this.emailService = emailService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Se ejecuta después de la construcción del bean
     * Registra este observer en el publisher
     */
    @PostConstruct
    public void init() {
        eventPublisher.subscribe(this);
    }

    @Override
    public void update(Event event) {
        System.out.println("EmailEventObserver recibió evento: " + event.getEventType());

        switch (event.getEventType()) {
            case USER_REGISTERED:
                handleUserRegistered(event);
                break;
            case PASSWORD_RECOVERY_REQUESTED:
                handlePasswordRecovery(event);
                break;
            case TRANSACTION_COMPLETED:
                handleTransactionCompleted(event);
                break;
            case ACCOUNT_CREATED:
                handleAccountCreated(event);
                break;
            case ALIAS_CHANGED:
                handleAliasChanged(event);
                break;
            case PASSWORD_CHANGED:
                handlePasswordChanged(event);
                break;

            case USD_ACCOUNT_CREATED:
                handleUsdAccountCreated(event);
                break;
            default:
                System.out.println("Evento no manejado: " + event.getEventType());
        }
    }

    @Override
    public boolean canHandle(EventType eventType) {
        return HANDLED_EVENTS.contains(eventType);
    }

    /**
     * Maneja el evento de registro de usuario
     * @param event Evento con datos del usuario y token
     */
    private void handleUserRegistered(Event event) {
        User user = (User) event.getData("user");
        String token = (String) event.getData("token");

        if (user != null && token != null) {
            emailService.sendVerificationEmail(user, token);

        } else {
            System.err.println("Error: Datos incompletos para USER_REGISTERED");
        }
    }

    /**
     * Maneja el evento de recuperación de contraseña
     * @param event Evento con datos del usuario y token
     */
    private void handlePasswordRecovery(Event event) {
        User user = (User) event.getData("user");
        String token = (String) event.getData("token");

        if (user != null && token != null) {
            emailService.sendRecoverPasswordEmail(user, token);

        } else {
            System.err.println("Error: Datos incompletos para PASSWORD_RECOVERY_REQUESTED");
        }
    }

    /**
     * Maneja el evento de transacción completada
     * @param event Evento con datos del usuario, monto y alias destino
     */
    private void handleTransactionCompleted(Event event) {
        User user = (User) event.getData("user");
        Double amount = (Double) event.getData("amount");
        String destinationAlias = (String) event.getData("destinationAlias");
        String currency = (String) event.getData("currency");
        Boolean converted = (Boolean) event.getData("converted");
        Double amountUsd = (Double) event.getData("amountUsd");
        Double exchangeRate = (Double) event.getData("exchangeRate");
        Double taxAmount = (Double) event.getData("taxAmount");
        Double taxPercentage = (Double) event.getData("taxPercentage");
        Double totalDebitado = (Double) event.getData("totalDebitado");

        if (user != null && amount != null && destinationAlias != null) {
            emailService.sendTransactionCompletedEmail(user, amount, destinationAlias, currency, 
                    converted != null ? converted : false, amountUsd, exchangeRate, 
                    taxAmount, taxPercentage, totalDebitado);

        } else {
            System.err.println("Error: Datos incompletos para TRANSACTION_COMPLETED");
        }
    }

    /**
     * Maneja el evento de cuenta creada
     * @param event Evento con datos del usuario, alias y CVU
     */
    private void handleAccountCreated(Event event) {
        User user = (User) event.getData("user");
        String accountAlias = (String) event.getData("accountAlias");
        String accountCvu = (String) event.getData("accountCvu");

        if (user != null && accountAlias != null && accountCvu != null) {
            emailService.sendAccountCreatedEmail(user, accountAlias, accountCvu);

        } else {
            System.err.println("Error: Datos incompletos para ACCOUNT_CREATED");
        }
    }

    private void handleUsdAccountCreated(Event event){
        User user = (User) event.getData("user");
        String accountAlias = (String) event.getData("accountAlias");
        String accountCvu = (String) event.getData("accountCvu");

        if(user != null && accountAlias != null && accountCvu != null){
            emailService.sendUsdAccountCreatedEmail(user,accountAlias,accountCvu);
        }
    }

    /**
     * Maneja el evento de cambio de alias
     * @param event Evento con datos del usuario, alias viejo y nuevo
     */
    private void handleAliasChanged(Event event) {
        User user = (User) event.getData("user");
        String oldAlias = (String) event.getData("oldAlias");
        String newAlias = (String) event.getData("newAlias");

        if (user != null && oldAlias != null && newAlias != null) {
            emailService.sendAliasChangedEmail(user, oldAlias, newAlias);

        } else {
            System.err.println("Error: Datos incompletos para ALIAS_CHANGED");
        }
    }

    /**
     * Maneja el evento de cambio de contraseña
     * @param event Evento con datos del usuario
     */
    private void handlePasswordChanged(Event event) {
        User user = (User) event.getData("user");

        if (user != null) {
            emailService.sendPasswordChangedEmail(user);

        } else {
            System.err.println("Error: Datos incompletos para PASSWORD_CHANGED");
        }
    }
}
