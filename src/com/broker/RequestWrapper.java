package com.broker;

public class RequestWrapper {
    private final String correlationId;
    private final Object payload;
    
    public RequestWrapper(String correlationId, Object payload) {
        this.correlationId = correlationId;
        this.payload = payload;
    }
    public String getCorrelationId() { return correlationId; }
    public Object getPayload() { return payload; }
}