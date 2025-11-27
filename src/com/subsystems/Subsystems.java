package com.subsystems;
import com.broker.AsyncMessageBroker;

// init: register listener
// start:: for background tasks
// shutdown: unregister listener and close resources

public interface Subsystems {
    void init(AsyncMessageBroker broker);
    void start();
    void shutdown();
}
