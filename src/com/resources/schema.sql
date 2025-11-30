PRAGMA foreign_keys = ON;

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    email TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL,
    role TEXT NOT NULL CHECK(role IN ('Customer', 'Staff', 'CEO')),
    phone_number TEXT,
    address TEXT,
    created_at INTEGER NOT NULL DEFAULT (strftime('%s','now'))
);

-- Items
CREATE TABLE IF NOT EXISTS items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    description TEXT,
    category TEXT,
    price REAL NOT NULL CHECK(price >= 0),
    stock_quantity INTEGER NOT NULL DEFAULT 0,
    like_count INTEGER NOT NULL DEFAULT 0
);

-- Payment methods
CREATE TABLE IF NOT EXISTS payment_methods (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    card_holder_name TEXT,
    card_number TEXT,
    expiry_date TEXT,
    card_type TEXT,
    created_at INTEGER NOT NULL DEFAULT (strftime('%s','now')),
    FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Wishlist
CREATE TABLE IF NOT EXISTS wishlist (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    customer_id INTEGER NOT NULL,
    item_id INTEGER NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    added_at INTEGER NOT NULL,
    UNIQUE(customer_id, item_id),
    FOREIGN KEY(customer_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY(item_id) REFERENCES items(id) ON DELETE CASCADE
);

-- Orders
CREATE TABLE IF NOT EXISTS orders (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    customer_id INTEGER NOT NULL,
    order_date INTEGER NOT NULL,
    status TEXT NOT NULL CHECK(status IN ('PLACED', 'SHIPPED', 'DELIVERED', 'CANCELED')),
    total_amount REAL NOT NULL,
    billing_address TEXT,
    payment_method_id INTEGER DEFAULT NULL,
    FOREIGN KEY(customer_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY(payment_method_id) REFERENCES payment_methods(id)
);

-- Order items
CREATE TABLE IF NOT EXISTS order_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id INTEGER NOT NULL,
    item_id INTEGER NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price REAL NOT NULL,
    sub_total REAL NOT NULL,
    FOREIGN KEY(order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY(item_id) REFERENCES items(id)
);

-- Messages / Conversations
CREATE TABLE IF NOT EXISTS conversations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    customer_id INTEGER NOT NULL,
    subject TEXT,
    created_at INTEGER NOT NULL DEFAULT (strftime('%s','now')),
    FOREIGN KEY(customer_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS messages (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    conversation_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    role TEXT NOT NULL,
    content TEXT NOT NULL,
    is_read INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL DEFAULT (strftime('%s','now')),
    FOREIGN KEY(conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
    FOREIGN KEY(user_id) REFERENCES users(id)
);

-- Reports and report contents
CREATE TABLE IF NOT EXISTS reports (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    type TEXT NOT NULL,
    start_date INTEGER NOT NULL,
    end_date INTEGER NOT NULL,
    created_date INTEGER NOT NULL DEFAULT (strftime('%s','now')),
    sold_quantity INTEGER NOT NULL DEFAULT 0,
    total_revenue REAL NOT NULL DEFAULT 0.0
);

CREATE TABLE IF NOT EXISTS report_content (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    report_id INTEGER NOT NULL,
    item_id INTEGER NOT NULL,
    item_sold INTEGER NOT NULL,
    unit_price REAL NOT NULL,
    sub_total REAL NOT NULL,
    FOREIGN KEY(report_id) REFERENCES reports(id) ON DELETE CASCADE,
    FOREIGN KEY(item_id) REFERENCES items(id)
);

-- Liked items (alias for light wishlist/likes)
CREATE TABLE IF NOT EXISTS liked_item (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    customer_id INTEGER NOT NULL,
    item_id INTEGER NOT NULL,
    UNIQUE(customer_id, item_id),
    FOREIGN KEY(customer_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY(item_id) REFERENCES items(id)
);