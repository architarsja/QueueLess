package com.queueless.util;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class DBConnection {

    private static final Properties PROPS = load();

    private DBConnection() {
    }

    private static Properties load() {
        Properties p = new Properties();

        try (InputStream in =
                     DBConnection.class.getClassLoader()
                             .getResourceAsStream("db.properties")) {

            if (in != null) {
                p.load(in);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return p;
    }

    private static String value(String env, String key, String fallback) {
        String v = System.getenv(env);

        if (v == null || v.isBlank()) {
            v = System.getProperty(env);
        }

        if (v == null || v.isBlank()) {
            v = PROPS.getProperty(key);
        }

        return (v == null || v.isBlank()) ? fallback : v;
    }

    public static Connection getConnection() throws SQLException {

        String url = value("DB_URL", "db.url", "");
        String user = value("DB_USERNAME", "db.username", "");
        String pass = value("DB_PASSWORD", "db.password", "");

        if (url.isBlank() || user.isBlank()) {
            throw new SQLException(
                "Database configuration is missing. Set DB_URL, DB_USERNAME and DB_PASSWORD."
            );
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException(
                "MySQL JDBC driver was not found. Check mysql-connector-j dependency.",
                e
            );
        }

        return DriverManager.getConnection(url, user, pass);
    }
}