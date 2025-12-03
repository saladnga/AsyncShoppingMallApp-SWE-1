package com.broker;

public class RequestWrapper {
    private final String correlationId;
    private final Object payload;

    public RequestWrapper(String correlationId, Object payload) {
        this.correlationId = correlationId;
        this.payload = payload;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public Object getPayload() {
        return payload;
    }

    // Creates a wrapper with explicit correlationId and payload.
    public static RequestWrapper wrap(String correlationId, Object payload) {
        return new RequestWrapper(correlationId, payload);
    }

    /**
     * Creates a wrapper by extracting correlationId from an incoming Message.
     * Useful when re-publishing an event based on another.
     */
    public static RequestWrapper fromMessage(Message msg, Object payload) {
        String cid = msg.getCorrelationId() != null
                ? msg.getCorrelationId()
                : msg.getId();

        return new RequestWrapper(cid, payload);
    }

    /**
     * Returns a new wrapper with updated payload but same correlationId.
     */
    public RequestWrapper withPayload(Object newPayload) {
        return new RequestWrapper(this.correlationId, newPayload);
    }

    @Override
    public String toString() {
        return "RequestWrapper{" +
                "correlationId='" + correlationId + '\'' +
                ", payload=" + payload +
                '}';
    }
}