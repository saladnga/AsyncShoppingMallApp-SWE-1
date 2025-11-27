package com.services;

import com.broker.AsyncMessageBroker;
import com.broker.EventType;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Schedules periodic events and publishes them into the broker.
 *
 * Usage:
 * AsyncMessageBroker broker = new AsyncMessageBroker(...);
 * TimeActor timer = new TimeActor(broker);
 * timer.start(); // starts scheduling daily/monthly triggers
 * ...
 * timer.stop(); // stops the scheduler
 */

public class TimeActor {
    // Single-threaded scheduler to run tasks at fixed intervals
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final AsyncMessageBroker broker;

    public TimeActor(AsyncMessageBroker broker) {
        this.broker = broker;
    }

    /**
     * Starts scheduling periodic events.
     * - Daily report trigger runs every 24 hours
     * - Monthly report trigger runs every ~30 days
     */

    public void start() {
        long initialDelaySeconds = 5; // delay before first trigger
        long dailyPeriodSeconds = 24 * 60 * 60; // 24 hours
        long monthlyPeriodSeconds = 30L * 24 * 60 * 60; // 30 days (1 month)

        // Daily report trigger
        scheduler.scheduleAtFixedRate(() -> {
            // Publish daily report event with 1-second offer timeout
            broker.publish(EventType.TIMER_TRIGGER_DAILY_REPORT, null);
        }, initialDelaySeconds, dailyPeriodSeconds, TimeUnit.SECONDS);

        // Monthly report trigger
        scheduler.scheduleAtFixedRate(() -> {
            // Publish monthly report event with 1-second offer timeout
            broker.publish(EventType.TIMER_TRIGGER_MONTHLY_REPORT, null);
        }, initialDelaySeconds + 10, monthlyPeriodSeconds, TimeUnit.SECONDS);
    }

    /**
     * Stops the scheduler immediately (no waiting for tasks to finish)
     */

    public void stop() {
        scheduler.shutdown();
    }

    public boolean awaitTermination(Duration timeout) {
        try {
            return scheduler.awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }  catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
