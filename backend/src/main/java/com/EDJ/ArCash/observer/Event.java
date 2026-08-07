package com.EDJ.ArCash.observer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Clase que representa un evento del sistema
 * Contiene información sobre el tipo de evento y datos asociados
 */
@Getter
@Setter
@AllArgsConstructor
@Builder
public class Event {
    private EventType eventType;
    private LocalDateTime timestamp;
    private Map<String, Object> data;

    public Event(EventType eventType) {
        this.eventType = eventType;
        this.timestamp = LocalDateTime.now();
        this.data = new HashMap<>();
    }

    /**
     * Agrega un dato al evento
     * @param key Clave del dato
     * @param value Valor del dato
     */
    public void addData(String key, Object value) {
        this.data.put(key, value);
    }

    /**
     * Obtiene un dato del evento
     * @param key Clave del dato
     * @return Valor del dato
     */
    public Object getData(String key) {
        return this.data.get(key);
    }
}
