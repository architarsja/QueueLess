package com.queueless.dao;

import com.queueless.model.QueueEntry;
import com.queueless.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QueueDAO {

    public QueueEntry register(String name, String phone, long serviceId, String type) throws SQLException {

        try (Connection c = DBConnection.getConnection()) {

            c.setAutoCommit(false);

            try {

                String prefix;

                try (PreparedStatement p = c.prepareStatement(
                        "SELECT prefix FROM services WHERE id=? AND status='ACTIVE' FOR UPDATE")) {

                    p.setLong(1, serviceId);

                    try (ResultSet r = p.executeQuery()) {

                        if (!r.next()) {
                            throw new SQLException("Service is unavailable.");
                        }

                        prefix = r.getString(1);
                    }
                }

                int seq = 0;

                try (PreparedStatement p = c.prepareStatement(
                        "SELECT last_token_number FROM queue_counters WHERE service_id=? FOR UPDATE")) {

                    p.setLong(1, serviceId);

                    try (ResultSet r = p.executeQuery()) {

                        if (r.next()) {
                            seq = r.getInt(1);
                        }
                    }
                }

                seq++;

                try (PreparedStatement p = c.prepareStatement(
                        "INSERT INTO queue_counters(service_id,last_token_number) " +
                        "VALUES(?,?) " +
                        "ON DUPLICATE KEY UPDATE last_token_number=VALUES(last_token_number)")) {

                    p.setLong(1, serviceId);
                    p.setInt(2, seq);
                    p.executeUpdate();
                }

                String token = prefix + String.format("%03d", seq);

                long id;

                try (PreparedStatement p = c.prepareStatement(
                        "INSERT INTO queue_entries(" +
                        "token_number," +
                        "token_sequence," +
                        "customer_name," +
                        "phone," +
                        "service_id," +
                        "registration_type" +
                        ") VALUES(?,?,?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {

                    p.setString(1, token);
                    p.setInt(2, seq);
                    p.setString(3, name);
                    p.setString(4, phone);
                    p.setLong(5, serviceId);
                    p.setString(6, type);

                    p.executeUpdate();

                    try (ResultSet r = p.getGeneratedKeys()) {

                        if (!r.next()) {
                            throw new SQLException("Unable to create queue entry.");
                        }

                        id = r.getLong(1);
                    }
                }

                c.commit();

                return findById(id);

            } catch (Exception e) {

                c.rollback();

                if (e instanceof SQLException se) {
                    throw se;
                }

                throw new SQLException(e);
            }
        }
    }

    public QueueEntry findById(long id) throws SQLException {

        String q =
                "SELECT q.*, s.service_name " +
                "FROM queue_entries q " +
                "JOIN services s ON s.id=q.service_id " +
                "WHERE q.id=?";

        try (
                Connection c = DBConnection.getConnection();
                PreparedStatement p = c.prepareStatement(q)
        ) {

            p.setLong(1, id);

            try (ResultSet r = p.executeQuery()) {

                if (r.next()) {
                    return map(r);
                }

                return null;
            }
        }
    }

    public QueueEntry findByToken(String token) throws SQLException {

        String q =
                "SELECT q.*, s.service_name " +
                "FROM queue_entries q " +
                "JOIN services s ON s.id=q.service_id " +
                "WHERE q.token_number=? " +
                "ORDER BY q.id DESC " +
                "LIMIT 1";

        try (
                Connection c = DBConnection.getConnection();
                PreparedStatement p = c.prepareStatement(q)
        ) {

            p.setString(1, token);

            try (ResultSet r = p.executeQuery()) {

                if (r.next()) {
                    return map(r);
                }

                return null;
            }
        }
    }

    public List<QueueEntry> list(long serviceId) throws SQLException {

        String q =
                "SELECT q.*, s.service_name " +
                "FROM queue_entries q " +
                "JOIN services s ON s.id=q.service_id " +
                "WHERE q.service_id=? " +
                "ORDER BY q.token_sequence";

        List<QueueEntry> entries = new ArrayList<>();

        try (
                Connection c = DBConnection.getConnection();
                PreparedStatement p = c.prepareStatement(q)
        ) {

            p.setLong(1, serviceId);

            try (ResultSet r = p.executeQuery()) {

                while (r.next()) {
                    entries.add(map(r));
                }
            }
        }

        return entries;
    }

    public QueueEntry current(long serviceId) throws SQLException {

        String q =
                "SELECT q.*, s.service_name " +
                "FROM queue_entries q " +
                "JOIN services s ON s.id=q.service_id " +
                "WHERE q.service_id=? " +
                "AND q.status='SERVING' " +
                "LIMIT 1";

        try (
                Connection c = DBConnection.getConnection();
                PreparedStatement p = c.prepareStatement(q)
        ) {

            p.setLong(1, serviceId);

            try (ResultSet r = p.executeQuery()) {

                if (r.next()) {
                    return map(r);
                }

                return null;
            }
        }
    }

    public QueueEntry callNext(long serviceId) throws SQLException {

        try (Connection c = DBConnection.getConnection()) {

            c.setAutoCommit(false);

            try {

                /*
                 * Check whether another customer is already being served.
                 */
                try (PreparedStatement p = c.prepareStatement(
                        "SELECT id " +
                        "FROM queue_entries " +
                        "WHERE service_id=? " +
                        "AND status='SERVING' " +
                        "FOR UPDATE")) {

                    p.setLong(1, serviceId);

                    try (ResultSet r = p.executeQuery()) {

                        if (r.next()) {
                            throw new SQLException(
                                    "A customer is already being served."
                            );
                        }
                    }
                }

                /*
                 * Find the earliest waiting customer.
                 */
                long id = -1;

                try (PreparedStatement p = c.prepareStatement(
                        "SELECT id " +
                        "FROM queue_entries " +
                        "WHERE service_id=? " +
                        "AND status='WAITING' " +
                        "ORDER BY token_sequence " +
                        "LIMIT 1 " +
                        "FOR UPDATE")) {

                    p.setLong(1, serviceId);

                    try (ResultSet r = p.executeQuery()) {

                        if (r.next()) {
                            id = r.getLong(1);
                        }
                    }
                }

                if (id < 0) {
                    throw new SQLException("Queue is empty.");
                }

                /*
                 * Move the selected customer to SERVING.
                 */
                try (PreparedStatement p = c.prepareStatement(
                        "UPDATE queue_entries " +
                        "SET status='SERVING', called_at=NOW() " +
                        "WHERE id=?")) {

                    p.setLong(1, id);
                    p.executeUpdate();
                }

                c.commit();

                return findById(id);

            } catch (Exception e) {

                c.rollback();

                if (e instanceof SQLException se) {
                    throw se;
                }

                throw new SQLException(e);
            }
        }
    }

    public void action(long id, String action) throws SQLException {

        String target = switch (action) {

            case "complete" -> "COMPLETED";

            case "skip" -> "SKIPPED";

            case "cancel" -> "CANCELLED";

            default -> throw new SQLException("Invalid queue action.");
        };

        try (Connection c = DBConnection.getConnection()) {

            c.setAutoCommit(false);

            try {

                String expected =
                        "cancel".equals(action)
                                ? "WAITING"
                                : "SERVING";

                long serviceId;

                /*
                 * Lock the queue entry and verify its current state.
                 */
                try (PreparedStatement p = c.prepareStatement(
                        "SELECT service_id " +
                        "FROM queue_entries " +
                        "WHERE id=? " +
                        "AND status=? " +
                        "FOR UPDATE")) {

                    p.setLong(1, id);
                    p.setString(2, expected);

                    try (ResultSet r = p.executeQuery()) {

                        if (!r.next()) {
                            throw new SQLException(
                                    "This queue entry cannot be changed from its current status."
                            );
                        }

                        serviceId = r.getLong(1);
                    }
                }

                /*
                 * Update the selected customer.
                 */
                try (PreparedStatement p = c.prepareStatement(
                        "UPDATE queue_entries " +
                        "SET status=?, completed_at=NOW() " +
                        "WHERE id=?")) {

                    p.setString(1, target);
                    p.setLong(2, id);

                    p.executeUpdate();
                }

                /*
                 * When a serving customer is completed or skipped,
                 * automatically move the next waiting customer to SERVING.
                 *
                 * Cancel does not automatically call the next customer
                 * because cancellation is intended only for WAITING entries.
                 */
                if (!"cancel".equals(action)) {

                    long next = -1;

                    try (PreparedStatement p = c.prepareStatement(
                            "SELECT id " +
                            "FROM queue_entries " +
                            "WHERE service_id=? " +
                            "AND status='WAITING' " +
                            "ORDER BY token_sequence " +
                            "LIMIT 1 " +
                            "FOR UPDATE")) {

                        p.setLong(1, serviceId);

                        try (ResultSet r = p.executeQuery()) {

                            if (r.next()) {
                                next = r.getLong(1);
                            }
                        }
                    }

                    if (next > 0) {

                        try (PreparedStatement p = c.prepareStatement(
                                "UPDATE queue_entries " +
                                "SET status='SERVING', called_at=NOW() " +
                                "WHERE id=?")) {

                            p.setLong(1, next);
                            p.executeUpdate();
                        }
                    }
                }

                c.commit();

            } catch (Exception e) {

                c.rollback();

                if (e instanceof SQLException se) {
                    throw se;
                }

                throw new SQLException(e);
            }
        }
    }

    public int peopleAhead(QueueEntry q) throws SQLException {

        String x =
                "SELECT COUNT(*) " +
                "FROM queue_entries " +
                "WHERE service_id=? " +
                "AND status='WAITING' " +
                "AND token_sequence<?";

        try (
                Connection c = DBConnection.getConnection();
                PreparedStatement p = c.prepareStatement(x)
        ) {

            p.setLong(1, q.serviceId);
            p.setInt(2, q.sequence);

            try (ResultSet r = p.executeQuery()) {

                r.next();

                return r.getInt(1);
            }
        }
    }

    public int completedToday(long serviceId) throws SQLException {
        return count(serviceId, "COMPLETED");
    }

    public int waiting(long serviceId) throws SQLException {
        return count(serviceId, "WAITING");
    }

    public long totalCustomers() throws SQLException {

        try (
                Connection c = DBConnection.getConnection();
                PreparedStatement p = c.prepareStatement(
                        "SELECT COUNT(*) FROM queue_entries");
                ResultSet r = p.executeQuery()
        ) {

            r.next();

            return r.getLong(1);
        }
    }

    private int count(long serviceId, String status) throws SQLException {

        try (
                Connection c = DBConnection.getConnection();
                PreparedStatement p = c.prepareStatement(
                        "SELECT COUNT(*) " +
                        "FROM queue_entries " +
                        "WHERE service_id=? " +
                        "AND status=? " +
                        "AND registered_at>=CURDATE()")
        ) {

            p.setLong(1, serviceId);
            p.setString(2, status);

            try (ResultSet r = p.executeQuery()) {

                r.next();

                return r.getInt(1);
            }
        }
    }

    public double avgWait(long serviceId) throws SQLException {

        String q =
                "SELECT AVG(" +
                "TIMESTAMPDIFF(MINUTE, registered_at, called_at)" +
                ") " +
                "FROM queue_entries " +
                "WHERE service_id=? " +
                "AND called_at IS NOT NULL " +
                "AND registered_at>=CURDATE()";

        try (
                Connection c = DBConnection.getConnection();
                PreparedStatement p = c.prepareStatement(q)
        ) {

            p.setLong(1, serviceId);

            try (ResultSet r = p.executeQuery()) {

                r.next();

                return r.getDouble(1);
            }
        }
    }

    private QueueEntry map(ResultSet r) throws SQLException {

        QueueEntry q = new QueueEntry();

        q.id = r.getLong("id");
        q.serviceId = r.getLong("service_id");
        q.token = r.getString("token_number");
        q.sequence = r.getInt("token_sequence");
        q.name = r.getString("customer_name");
        q.phone = r.getString("phone");
        q.serviceName = r.getString("service_name");
        q.registrationType = r.getString("registration_type");
        q.status = r.getString("status");

        Timestamp registeredAt = r.getTimestamp("registered_at");

        if (registeredAt != null) {
            q.registeredAt = registeredAt
                    .toLocalDateTime()
                    .toString()
                    .replace('T', ' ');
        }

        return q;
    }
}