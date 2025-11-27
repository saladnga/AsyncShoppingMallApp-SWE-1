package com.broker;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable event message used inside the broker.
 *
 * A Message contains:
 * - a unique ID (for message identity and tracking)
 * - the event type identifying what kind of event this is
 * - an optional payload containing domain data
 * - a creation timestamp (epoch milliseconds)
 * - an optional correlationId for tracing related events across services
 *
 * Messages are immutable and therefore thread-safe.
 */

public class Message {
    private final String id;
    private final EventType eventType;
    private final Object payload;
    private final long timestamp;
    private final String correlationId;

    // Constructor used for publishing message normally, generates a new UUID and
    // timestamp automatically
    public Message(EventType eventType, Object payload) {
        this(
                UUID.randomUUID().toString(),
                eventType,
                payload,
                Instant.now().toEpochMilli(),
                null);
    }

    public Message(String id, EventType eventType, Object payload, long timestamp, String correlationId) {
        this.id = id;
        this.eventType = eventType;
        this.payload = payload;
        this.timestamp = timestamp;
        this.correlationId = correlationId;
    }

    public String getId() {
        return id;
    }

    public EventType getEventType() {
        return eventType;
    }

    public Object getPayload() {
        return payload;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    @Override
    public String toString() {
        return "Message{" + "id=" + id + ", evenType=" + eventType + ", timestamp=" + timestamp + "}";
    }

}
