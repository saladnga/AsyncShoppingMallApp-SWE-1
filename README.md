# Shopping Mall Application

A Java-based e-commerce application built with an **asynchronous, event-driven architecture** using a custom message broker pattern. This console-based shopping mall system provides comprehensive functionality for customers, staff, and administrators with role-based access control.

## Architecture Overview

### Event-Driven Design

- **AsyncMessageBroker**: Central event dispatcher with thread pool execution
- **Modular Subsystems**: 7 loosely-coupled subsystems communicating via events
- **CompletableFuture**: Non-blocking asynchronous message processing
- **Bounded Queue**: Backpressure control with configurable message queuing

### Design Patterns

- **Repository Pattern**: Data access abstraction with SQLite persistence
- **Builder Pattern**: Clean entity construction (e.g., `User.Builder`)
- **Singleton Pattern**: Session management with `SessionManager`
- **Observer Pattern**: Event-driven communication between subsystems

## Features

### User Management & Authentication

- **Secure Registration**: PBKDF2 password hashing (310,000 iterations)
- **Role-based Access Control**: Customer, Staff, CEO roles with secret codes
- **Session Management**: UUID-based secure session tokens
- **Password Policy**: Complex validation (8+ chars, mixed case, numbers, special chars)

### Shopping & Inventory

- **Product Catalog**: Browse, search, and view item details
- **Inventory Management**: Staff can add, edit, and manage products
- **Like System**: Track item popularity with like counts
- **Stock Management**: Real-time inventory tracking
- **Item Ranking**: Display top items by popularity

### Order Management

- **Shopping Cart**: Add items and create orders
- **Order Processing**: Multi-step async order workflow
- **Order Tracking**: Monitor order status and shipping
- **Order History**: View past purchases and order details

### Wishlist System

- **Save for Later**: Add items to personal wishlist
- **Wishlist Management**: View, add, and remove wishlist items
- **Stock Notifications**: Track availability of wishlist items

### Messaging System

- **Customer-Staff Communication**: Internal messaging platform
- **Conversation Threading**: Organized message conversations
- **Real-time Notifications**: Instant message alerts
- **Message History**: Complete conversation tracking

### Payment Processing

- **Multiple Payment Methods**: Store and manage payment cards
- **Payment Authorization**: Integrated payment processing
- **Transaction History**: Complete payment audit trail
- **Refund Processing**: Handle payment reversals

### Reporting & Analytics

- **Sales Reports**: Generate business performance reports
- **User Analytics**: Track system usage patterns
- **Inventory Reports**: Monitor stock levels and performance

## Technical Stack

### Core Technologies

- **Java 25**: Modern Java features and syntax
- **SQLite**: Lightweight embedded database
- **JDBC**: Database connectivity and prepared statements
- **Spring Security**: PBKDF2 password hashing
- **Concurrent Collections**: Thread-safe data structures

### Security Features

- **Password Security**: Industry-standard PBKDF2 hashing
- **SQL Injection Prevention**: Prepared statements throughout
- **Session Management**: Secure token-based authentication
- **Role-based Authorization**: Method-level access control

## Project Structure

```
src/com/
├── Main.java                    # Application entry point & orchestration
├── broker/                      # Async message broker system
│   ├── AsyncMessageBroker.java  # Central event dispatcher
│   ├── EventType.java          # Event type definitions
│   ├── Listener.java           # Event listener interface
│   └── Message.java            # Message wrapper
├── common/                      # Shared utilities & DTOs
│   ├── Database.java           # SQLite connection manager
│   └── dto/                    # Data transfer objects
├── entities/                    # Domain models (14 entities)
│   ├── User.java              # User entity with Builder pattern
│   ├── Item.java              # Product/item entity
│   ├── Order.java             # Order management entity
│   └── ...                    # Other business entities
├── managers/                    # Business logic layer
│   ├── account/               # User account management
│   ├── item/                  # Item management
│   ├── order/                 # Order processing
│   └── ...                    # Other business managers
├── repository/                  # Data access layer
│   ├── UserRepository.java    # User data interface
│   ├── SQLiteUserRepository.java # SQLite user implementation
│   └── ...                    # Other repositories
├── services/                    # External services
│   ├── AuthenticationService.java # Password hashing/verification
│   ├── SessionManager.java    # Session management
│   └── ...                    # Other services
├── subsystems/                  # Modular subsystems
│   ├── AccountManagement.java # User account subsystem
│   ├── ItemManagement.java    # Inventory subsystem
│   └── ...                    # Other subsystems
└── ui/                         # User interface layer
    ├── CustomerUI.java        # Customer interface
    ├── StaffUI.java          # Staff interface
    └── CeoUI.java            # CEO interface
```

## Database Schema

The application uses SQLite with a comprehensive schema:

- **users**: User accounts with role-based access
- **items**: Product catalog with inventory
- **orders**: Order management and tracking
- **order_items**: Order line items
- **payment_methods**: Stored payment cards
- **wishlist**: User wishlist items
- **conversations**: Message conversations
- **reports**: Generated business reports
- **shipping_tracking**: Order fulfillment status

## Getting Started

### Prerequisites

- Java 25 or higher
- SQLite JDBC driver (included in dependencies)
- Spring Security (for password hashing)

### Installation

1. **Clone the repository**

   ```bash
   git clone https://github.com/saladnga/shopping-mall-app-java-async.git
   cd shopping-mall-app-java-async
   ```

2. **Compile the application**

   ```bash
   javac -cp "lib/*" src/com/*.java src/com/**/*.java
   ```

3. **Run the application**
   ```bash
   java -cp "lib/*:src" com.Main
   ```

### Default Login Credentials

The application includes role-based registration with secret codes:

- **Customer**: No secret code required
- **Staff**: Secret code `STAFF2025`
- **CEO**: Secret code `CEO2025`

## Usage

### Customer Features

1. **Registration & Login**: Create account and authenticate
2. **Browse Items**: View product catalog with details
3. **Shopping**: Add items to cart and create orders
4. **Wishlist**: Save items for later purchase
5. **Messaging**: Contact staff for support
6. **Account Management**: View and edit profile

### Staff Features

1. **Inventory Management**: Add, edit, and manage products
2. **Customer Support**: Handle customer messages
3. **Order Management**: Process and track orders
4. **Stock Control**: Monitor and update inventory levels

### CEO Features

1. **Business Reports**: Generate sales and analytics reports
2. **User Management**: View system users and activity
3. **System Overview**: Monitor application performance

## Configuration

### Message Broker Settings

```java
// In Main.java
AsyncMessageBroker broker = new AsyncMessageBroker(1000, 8);
// Queue size: 1000 messages
// Thread pool: 8 worker threads
```

### Database Configuration

```java
// SQLite database file
database.connect("jdbc:sqlite:shopping_mall.db");
```

### Password Security

- **Algorithm**: PBKDF2WithHmacSHA256
- **Iterations**: 310,000 (Spring Security recommended)
- **Salt Length**: 16 bytes

## Async Flow Examples

### User Login Flow

```
User Input → USER_LOGIN_REQUEST → AccountManagement
                ↓ (async auth)
         AuthenticationService
                ↓
USER_LOGIN_SUCCESS/FAILED ← AccountManagement
```

### Order Processing Flow

```
Customer Order → ORDER_CREATE_REQUEST → OrderManagement
                      ↓ (validate items)
              Item & Stock Validation
                      ↓ (process payment)
                PaymentService
                      ↓ (create shipping)
                ShippingService
                      ↓
            ORDER_CONFIRMED → Customer
```

## Event Types

The system uses 20+ event types for communication:

- `USER_LOGIN_REQUEST/SUCCESS/FAILED`
- `USER_REGISTER_REQUESTED/SUCCESS`
- `ITEM_LIST_REQUEST/RETURNED`
- `ORDER_CREATE_REQUEST/CONFIRMED`
- `WISHLIST_ADD/REMOVE_REQUEST`
- `MESSAGE_SENT_CONFIRMATION`
- `REPORT_GENERATION_COMPLETE`

## Testing

Currently, the application lacks automated testing. Recommended test coverage:

### Unit Tests

- **AuthenticationService**: Password hashing and verification
- **RegisterManager**: Input validation and user creation
- **LoginManager**: Authentication logic
- **SessionManager**: Session lifecycle management

### Integration Tests

- **Repository Layer**: Database operations with in-memory SQLite
- **Message Broker**: Event publishing and listener execution
- **End-to-End Flows**: Complete user scenarios

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## Authors

- **CS3332 Team**: [Sam Cao](https://github.com/Samcave2206), [Quan Nguyen](https://github.com/NguyenQuan297), [Vu Hoang](https://github.com/saladnga)

## Acknowledgments

- Spring Security for robust password hashing
- SQLite for lightweight database solution
- Java Concurrency utilities for async processing
