package com.EDJ.ArCash.observer;

public interface EventObserver {

    /**
     * Método llamado cuando ocurre un evento
     * @param event Evento que ha ocurrido
     */
    void update(Event event);

    boolean canHandle(EventType eventType);
}
