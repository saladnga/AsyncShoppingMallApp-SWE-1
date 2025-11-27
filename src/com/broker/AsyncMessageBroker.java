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
        this.listenerExecutor = Executors.newFixedThreadPool(Math.max(1, listenerThreads));

        // this.dispatcherThread = new Thread(() -> dispatchLoop(),
        // "AsyncMessageBroker-dispatcher");
    }

    // Starts the broker and launches the dispatcher thread
    public synchronized void start() {
        // Start only when it is not running
        if (!running) {
            running = true;
            dispatcherThread = new Thread(this::dispatchLoop);
            dispatcherThread.start();
            System.out.println("[Broker] started");
        }
    }

    // Stops the dispatcher and thread pool gracefully
    public synchronized void stop() {
        running = false;
        dispatcherThread.interrupt();
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

    // Publish a message to the queue. If the queue is full or broker not running.
    // returns a failed future
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

    // Main loop that pulls messages from the queue and distributes them to
    // listeners
    public void dispatchLoop() {
        System.out.println("[Broker] Dispatcher loop started");

        // Run while broker is active or there are still unprocessed messages in queue
        while (running) {
            try {
                Message message = queue.take();
                List<Listener> listeners = subscribers.get(message.getEventType());

                if (listeners != null) {
                    for (Listener listener : listeners) {
                        listenerExecutor.submit(() -> listener.onMessage(message));
                    }
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
