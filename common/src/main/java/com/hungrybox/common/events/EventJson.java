package com.hungrybox.common.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Events travel over Kafka as plain JSON strings. Keeping serialization explicit
 * (instead of type headers) means producers and consumers only share the JSON contract.
 */
public class EventJson {

    private final ObjectMapper objectMapper;

    public EventJson(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String write(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not serialize event " + event.getClass().getSimpleName(), e);
        }
    }

    public <T> T read(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not parse " + type.getSimpleName() + " from payload", e);
        }
    }
}
