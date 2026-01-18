package com.EDJ.ArCash.observer;

/**
 * Interfaz Observer del patrón Observer
 * Define el contrato para los observadores que reaccionan a eventos del sistema
 */
public interface EventObserver {

    /**
     * Método llamado cuando ocurre un evento
     * @param event Evento que ha ocurrido
     */
    void update(Event event);

    /**
     * Indica si este observador puede manejar el tipo de evento dado
     * @param eventType Tipo de evento
     * @return true si puede manejar el evento, false en caso contrario
     */
    boolean canHandle(EventType eventType);
}
