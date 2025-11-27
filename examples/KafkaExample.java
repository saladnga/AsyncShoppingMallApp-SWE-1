// Example: Using Apache Kafka for distributed messaging
// This scales to handle millions of messages across multiple services

package examples;

public class KafkaExample {
    /*
    // With Kafka, your events become distributed across multiple servers
    
    @KafkaListener(topics = "user.login.requests")
    public void handleLoginRequest(LoginRequest request) {
        // Process login
        User user = authenticateUser(request);
        
        // Publish to different topic
        kafkaTemplate.send("user.login.success", user);
    }
    
    @KafkaListener(topics = "order.creation.requests") 
    public void handleOrderCreation(OrderRequest request) {
        // Process order
        Order order = createOrder(request);
        
        // Trigger payment processing on another service
        kafkaTemplate.send("payment.authorization.requests", order);
    }
    
    Benefits:
    - Handles millions of messages per second
    - Distributed across multiple servers
    - Built-in persistence and replay
    - Industry standard for microservices
    */
}