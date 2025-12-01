package com.ui;

import com.broker.AsyncMessageBroker;
import com.broker.EventType;
import com.broker.Listener;
import com.broker.Message;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class BrokerUtils {

    /**
     * Publish a request and wait for a single response event. Returns null on
     * timeout/error.
     */
    @SuppressWarnings("unchecked")
    public static <T> T requestOnce(AsyncMessageBroker broker, EventType requestType, Object requestPayload,
            EventType responseType, long timeoutMs) {

        CompletableFuture<T> fut = new CompletableFuture<>();

        Listener listener = new Listener() {
            @Override
            public CompletableFuture<Void> onMessage(Message message) {
                try {
                    Object p = message.getPayload();
                    fut.complete((T) p);
                } catch (Throwable ex) {
                    fut.complete(null);
                }
                return Listener.completed();
            }
        };

        broker.registerListener(responseType, listener);

        try {
            broker.publish(requestType, requestPayload);
            return fut.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (Exception ex) {
            return null;
        } finally {
            broker.unregisterListener(responseType, listener);
        }
    }
}