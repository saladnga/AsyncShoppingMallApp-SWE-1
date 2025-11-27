package com.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Minimal database helper that opens a JDBC connection and runs the
 * `src/com/resources/schema.sql` file to initialize the schema.
 *
 * This keeps initialization outside of the business logic and lets
 * subsystems run DB calls from their async handlers.
 */
public class Database {
    private Connection connection;

    /**
     * Connects to the provided JDBC URL and executes the project's SQL schema
     * file to create tables if they do not exist.
     *
     * Example: connect("jdbc:h2:mem:shopping_mall")
     */
    public void connect(String url) {
        try {
            connection = DriverManager.getConnection(url);

            // Try to read schema file relative to project root
            Path schemaPath = Path.of("src/com/resources/schema.sql");
            if (Files.exists(schemaPath)) {
                String sql = Files.readString(schemaPath);
                // Split statements on semicolon followed by newline to be conservative
                String[] statements = sql.split(";\n");
                try (Statement stmt = connection.createStatement()) {
                    for (String s : statements) {
                        String trim = s.trim();
                        if (trim.isEmpty()) continue;
                        try {
                            stmt.execute(trim);
                        } catch (SQLException ex) {
                            // Log and continue - some statements like PRAGMA may not be supported
                            System.err.println("[Database] Failed to execute statement: " + ex.getMessage());
                        }
                    }
                }
                System.out.println("[Database] Schema executed from " + schemaPath.toString());
            } else {
                System.out.println("[Database] Schema file not found at: " + schemaPath.toString());
            }

            System.out.println("[Database] Connected to " + url);
        } catch (SQLException | IOException e) {
            System.err.println("[Database] Connect failed: " + e.getMessage());
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("[Database] Connection closed");
            } catch (SQLException e) {
                System.err.println("[Database] Close failed: " + e.getMessage());
            }
        }
    }

    // Lightweight placeholders - you can implement these as needed or use
    // the provided SimpleDatabase for richer functionality.
    public <T> T queryOne(String sql, Object param) {
        return null;
    }

    public int executeUpdate(String sql, Object param) {
        return 0;
    }

    public void beginTransaction() {
    }

    public void commit() {
    }

    public void rollback() {
    }
}
