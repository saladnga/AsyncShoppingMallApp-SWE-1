package com.common;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Database {

    private Connection connection;

    /** Connect SQLite + load schema */
    public synchronized void connect(String url) {
        // Database Connection Resilience - handle connection failures gracefully
        try {
            connection = DriverManager.getConnection(url);

            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA journal_mode=WAL;");
                st.execute("PRAGMA foreign_keys=ON;");
            }

            runSchema();

            System.out.println("[Database] Connected to " + url);

        } catch (SQLException e) {
            // Catch SQLException specifically
            connection = null;
            System.err.println("[Database] Connection failed: " + e.getMessage());
            System.err.println("[Database] Database unavailable. Please check if the database file exists.");
            throw new RuntimeException("Database connection failed", e);
        } catch (Exception e) {
            // Catch other exceptions
            connection = null;
            System.err.println("[Database] Connection failed: " + e.getMessage());
            throw new RuntimeException("Database connection failed", e);
        }
    }

    /** Load schema.sql from resources */
    private void runSchema() {
        try {
            InputStream in = getClass().getResourceAsStream("/com/resources/schema.sql");
            if (in == null) {
                System.err.println("[Database] schema.sql not found.");
                return;
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }

            String[] statements = sb.toString().split(";");

            try (Statement st = connection.createStatement()) {
                for (String raw : statements) {
                    String sql = raw.trim();
                    if (sql.isEmpty())
                        continue;

                    try {
                        st.execute(sql);
                    } catch (SQLException ex) {
                        System.err.println("[Database] Schema error: " + ex.getMessage());
                    }
                }
            }

            System.out.println("[Database] Schema executed.");

        } catch (Exception e) {
            System.err.println("[Database] Failed loading schema: " + e.getMessage());
        }
    }

    public Connection getConnection() {
        return connection;
    }

    // =====================================================================
    // QUERY METHODS
    // =====================================================================

    /** Query 1 row */
    public synchronized <T> T queryOne(String sql, ResultMapper<T> mapper, Object... params) {
        // Database Connection Resilience - check connection before query
        if (connection == null) {
            System.err.println("[Database] queryOne failed: Database connection unavailable");
            return null;
        }

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            fillParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return mapper.map(rs);
            }

        } catch (SQLException e) {
            // Handle SQLException gracefully
            System.err.println("[Database] queryOne failed: " + e.getMessage());
            // Don't crash - return null to indicate failure
        } catch (Exception e) {
            System.err.println("[Database] queryOne error: " + e.getMessage());
        }
        return null;
    }

    /** Query list */
    public synchronized <T> List<T> queryList(String sql, ResultMapper<T> mapper, Object... params) {

        List<T> list = new ArrayList<>();
        // Database Connection Resilience - check connection before query
        if (connection == null) {
            System.err.println("[Database] queryList failed: Database connection unavailable");
            return list;
        }

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            fillParams(ps, params);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    list.add(mapper.map(rs));
            }

        } catch (SQLException e) {
            // Handle SQLException gracefully
            System.err.println("[Database] queryList failed: " + e.getMessage());
            // Return empty list instead of crashing
        } catch (Exception e) {
            System.err.println("[Database] queryList error: " + e.getMessage());
        }

        return list;
    }

    /** Update / delete */
    public synchronized int executeUpdate(String sql, Object... params) {
        if (connection == null)
            return 0;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            fillParams(ps, params);
            return ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[Database] update failed: " + e.getMessage());
            return 0;
        }
    }

    // =====================================================================
    // INSERT RETURN ID
    // =====================================================================

    public synchronized int executeInsertReturnId(String sql, Object... params) {
        if (connection == null)
            return -1;

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            fillParams(ps, params);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next())
                    return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("[Database] insert failed: " + e.getMessage());
        }

        return -1;
    }

    // =====================================================================
    // Transaction Control
    // =====================================================================

    public synchronized void beginTransaction() {
        try {
            if (connection != null)
                connection.setAutoCommit(false);
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    public synchronized void commit() {
        try {
            if (connection != null) {
                connection.commit();
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    public synchronized void rollback() {
        try {
            if (connection != null) {
                connection.rollback();
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    // =====================================================================
    // UTILITIES
    // =====================================================================

    private void fillParams(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }

    public synchronized void close() {
        try {
            if (connection != null)
                connection.close();
            System.out.println("[Database] Closed.");
        } catch (SQLException e) {
            System.err.println("[Database] Close failed: " + e.getMessage());
        }
    }

    /** Result Mapper Interface */
    public interface ResultMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }
}