package com.broker;

import java.util.concurrent.CompletableFuture;

/*
Represents a message listener/handler.

The handler returns a CompletableFuture to support both:
- synchronous handlers: return CompletableFuture.completedFuture(null)
- asynchronous handlers: return a future that completes when processing finishes

Returning a future allows the broker to dispatch to listeners without blocking; the dispatcher thread can hand off the work to a thread pool and continue; immediately, while completion (or failure) is handled asynchronously.
*/

@FunctionalInterface
public interface Listener {
    CompletableFuture<Void> onMessage(Message message);
}
