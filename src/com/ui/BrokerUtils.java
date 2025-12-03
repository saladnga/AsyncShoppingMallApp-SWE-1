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
        final long requestTime = System.currentTimeMillis();
        final boolean[] responseReceived = new boolean[] { false };

        Listener listener = new Listener() {
            @Override
            public CompletableFuture<Void> onMessage(Message message) {
                // Ignore if we already got a response (prevents stale events from completing
                // the future)
                synchronized (responseReceived) {
                    if (responseReceived[0]) {
                        System.out.println("[BrokerUtils] Ignoring duplicate/stale " + responseType + " event");
                        return Listener.completed();
                    }
                    responseReceived[0] = true;
                }

                try {
                    Object p = message.getPayload();
                    // System.out.println("[BrokerUtils] Received " + responseType + " response: " +
                    // (p instanceof java.util.List<?> ? ((java.util.List<?>) p).size() + " items" :
                    // p));
                    fut.complete((T) p);
                } catch (Throwable ex) {
                    System.out.println("[BrokerUtils] Error processing response: " + ex.getMessage());
                    fut.complete(null);
                }
                return Listener.completed();
            }
        };

        try {
            // Register listener BEFORE publishing to avoid race condition
            broker.registerListener(responseType, listener);
            // System.out.println("[BrokerUtils] Registered listener and publishing " +
            // requestType);

            // Publish the request
            broker.publish(requestType, requestPayload);

            // Wait for response
            T result = fut.get(timeoutMs, TimeUnit.MILLISECONDS);
            // System.out.println("[BrokerUtils] Got result for " + responseType);
            return result;
        } catch (Exception ex) {
            System.out.println("[BrokerUtils] Timeout waiting for " + responseType + ": " + ex.getMessage());
            return null;
        } finally {
            broker.unregisterListener(responseType, listener);
            // System.out.println("[BrokerUtils] Unregistered listener for " +
            // responseType);
        }
    }
}