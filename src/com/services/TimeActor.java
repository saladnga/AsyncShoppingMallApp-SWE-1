package com.services;

import com.broker.AsyncMessageBroker;
import com.broker.EventType;

import java.time.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * TimeActor (Correct version based on real requirements)
 * -------------------------------------------------------
 * - Daily Report at 21:00 every business day (Mon–Fri)
 * - Monthly Report at 23:59 on last day of month
 * - Publishes events into AsyncMessageBroker
 * - Computes next execution dynamically (no fixed period)
 */

public class TimeActor {

    private static final Logger LOGGER = Logger.getLogger(TimeActor.class.getName());
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final AsyncMessageBroker broker;

    public TimeActor(AsyncMessageBroker broker) {
        this.broker = broker;
    }

    /** Start daily + monthly scheduling */
    public void start() {
        scheduleNextDailyReport();
        scheduleNextMonthlyReport();
    }

    /**
     * Schedules the next execution time for daily report (Mon–Fri at 21:00)
     */
    private void scheduleNextDailyReport() {
        LocalDateTime now = LocalDateTime.now();

        // Target time = today 21:00
        LocalDateTime target = now.withHour(21).withMinute(0).withSecond(0).withNano(0);

        // If now is past 21:00 → move to next day
        if (now.isAfter(target)) {
            target = target.plusDays(1);
        }

        // Skip weekend
        while (isWeekend(target.toLocalDate())) {
            target = target.plusDays(1);
        }

        long delay = Duration.between(now, target).toMillis();

        LOGGER.info("[TimeActor] Next DAILY report scheduled at " + target);

        scheduler.schedule(() -> {
            broker.publish(EventType.TIMER_TRIGGER_DAILY_REPORT, null);
            LOGGER.info("[TimeActor] DAILY REPORT triggered!");

            // Schedule the next one
            scheduleNextDailyReport();

        }, delay, TimeUnit.MILLISECONDS);
    }

    /**
     * Schedules monthly report at 23:59 on last day of month
     */
    private void scheduleNextMonthlyReport() {

        LocalDateTime now = LocalDateTime.now();

        // Compute last day of month
        LocalDate lastDay = now.toLocalDate().withDayOfMonth(now.toLocalDate().lengthOfMonth());

        LocalDateTime target = LocalDateTime.of(lastDay, LocalTime.of(23, 59, 0));

        // If today is last day and time has passed → move to next month
        if (now.isAfter(target)) {
            LocalDate firstNextMonth = now.toLocalDate().plusMonths(1).withDayOfMonth(1);
            LocalDate newLastDay = firstNextMonth.withDayOfMonth(firstNextMonth.lengthOfMonth());
            target = LocalDateTime.of(newLastDay, LocalTime.of(23, 59, 0));
        }

        long delay = Duration.between(now, target).toMillis();

        LOGGER.info("[TimeActor] Next MONTHLY report scheduled at " + target);

        scheduler.schedule(() -> {
            broker.publish(EventType.TIMER_TRIGGER_MONTHLY_REPORT, null);
            LOGGER.info("[TimeActor] MONTHLY REPORT triggered!");

            // Schedule next month
            scheduleNextMonthlyReport();

        }, delay, TimeUnit.MILLISECONDS);
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    public void stop() {
        LOGGER.info("[TimeActor] Stopping scheduler...");
        scheduler.shutdownNow();
    }
}