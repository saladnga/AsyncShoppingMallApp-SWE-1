package com.broker;

import java.util.*;
import java.util.concurrent.*;

public class AsyncMessageBroker {
    // Each EventType maps to a thread-safe list of listeners
    // CopyOnWriteArrayList = safe for iteration while adding/removing listeners

    private Map<EventType, List<Listener>> subscribers = new ConcurrentHashMap<>();

    // Bounded message queue
    private BlockingQueue<Message> queue;

    // Thread pool that executes listeners concurrently
    private ExecutorService listenerExecutor;

    // Background thread that takes messages from queue and dispatches them
    private Thread dispatcherThread;

    // Broker lifecycle flag
    private volatile boolean running = false;

    public AsyncMessageBroker(int queueSize, int listenerThreads) {
        // Initialize queue
        this.queue = new LinkedBlockingQueue<>(queueSize);

        // Thread pool used to execute listeners processing messages
        this.listenerExecutor = Executors.newFixedThreadPool(Math.max(1, listenerThreads), r -> {
            Thread thread = new Thread(r);
            thread.setName("Listener Worker - " + thread.threadId());
            return thread;
        });
    }

    // Starts the broker and launches the dispatcher thread
    public synchronized void start() {
        // Start only when it is not running
        if (!running) {
            running = true;

            dispatcherThread = new Thread(this::dispatchLoop, "Broker Dispatcher");
            dispatcherThread.start();

            System.out.println("[Broker] started");
        }
    }

    // Stops the dispatcher and thread pool gracefully
    public synchronized void stop() {
        running = false;

        if (dispatcherThread != null) {
            dispatcherThread.interrupt();
        }

        listenerExecutor.shutdownNow();

        System.out.println("[Broker] stopped");
    }

    // Register a listener to receive messages of a specific event type
    public void registerListener(EventType eventType, Listener listener) {
        List<Listener> list = subscribers.get(eventType);

        if (list == null) {
            list = new CopyOnWriteArrayList<>();
            subscribers.put(eventType, list);
        }
        list.add(listener);
    }

    // Unregister a listener to receive messages from a specific event type
    public void unregisterListener(EventType eventType, Listener listener) {
        List<Listener> list = subscribers.get(eventType);

        if (list != null) {
            list.remove(listener);
        }
    }

    // Publish a message to the queue. If not return a failed future
    public void publish(EventType eventType, Object payload) {
        if (!running) {
            System.out.println("[Broker] System is not running");
            return;
        }

        Message message = new Message(eventType, payload);
        boolean success = queue.offer(message);

        if (!success) {
            System.out.println("[Broker] Queue full, message got rejected");
        }
    }

    // Main loop of the dispatcher
    public void dispatchLoop() {
        System.out.println("[Broker] Dispatcher loop started");

        // Run while broker is active or there are still unprocessed messages in queue
        while (running) {
            try {
                Message message = queue.take();

                List<Listener> listeners = subscribers.get(message.getEventType());

                // TC29: Async Broker Unknown Event - handle unmapped event types
                if (listeners != null && !listeners.isEmpty()) {
                    for (Listener listener : listeners) {
                        listenerExecutor.submit(() -> {
                            try {
                                listener.onMessage(message);
                            } catch (Exception e) {
                                System.err.println("[Broker] Listener error for " + message.getEventType() + ": " + e.getMessage());
                            }
                        });
                    }
                } else {
                    // TC29: Log warning for unknown events but continue running
                    System.out.println("[Broker] Warning: No handler found for event type: " + message.getEventType() + ". Message ignored.");
                }

            } catch (InterruptedException e) {
                break;
            }
        }

        System.out.println("[Broker] Dispatcher loop stopped");
    }

    public int getQueueSize() {
        return queue.size();
    }

}
