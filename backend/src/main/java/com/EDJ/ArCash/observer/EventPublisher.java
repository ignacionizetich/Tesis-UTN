package com.EDJ.ArCash.observer;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class EventPublisher {

    private final List<EventObserver> observers = new ArrayList<>();

    /**
     * Registra un observador
     * @param observer Observador a registrar
     */
    public void subscribe(EventObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
            System.out.println("Observer registrado: " + observer.getClass().getSimpleName());
        }
    }

    /**
     * Elimina un observador
     * @param observer Observador a eliminar
     */
    public void unsubscribe(EventObserver observer) {
        observers.remove(observer);
        System.out.println("Observer eliminado: " + observer.getClass().getSimpleName());
    }

    /**
     * Notifica a todos los observadores sobre un evento
     * @param event Evento a notificar
     */
    public void publish(Event event) {
        System.out.println("Publicando evento: " + event.getEventType());
        for (EventObserver observer : observers) {
            if (observer.canHandle(event.getEventType())) {
                observer.update(event);
            }
        }
    }

    /**
     * Obtiene la cantidad de observadores registrados
     * @return Número de observadores
     */
    public int getObserverCount() {
        return observers.size();
    }
}
