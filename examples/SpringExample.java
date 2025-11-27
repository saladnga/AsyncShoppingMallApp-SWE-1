// Example: Using Spring's @EventListener annotation
// This would replace your AsyncMessageBroker with Spring's ApplicationEventPublisher

package com.subsystems.spring;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.entities.User;

@Service
public class SpringAccountManagement {
    
    @EventListener
    @Async  // Makes it asynchronous automatically
    public void handleLoginRequest(LoginRequestEvent event) {
        // Your login logic here
        System.out.println("Handling login: " + event.getUsername());
        
        // Spring publishes result events automatically
        publisher.publishEvent(new LoginSuccessEvent(user));
    }
}

// Events become simple POJOs
public class LoginRequestEvent {
    private final String username;
    private final String password;
    
    public LoginRequestEvent(String username, String password) {
        this.username = username;
        this.password = password;
    }
    // getters...
}