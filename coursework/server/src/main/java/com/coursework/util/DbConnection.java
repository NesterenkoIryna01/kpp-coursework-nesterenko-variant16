package com.coursework.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Фабрика JDBC-з’єднань до MySQL.
 * <p>
 * Параметри читаються зі змінних середовища / system properties:
 * {@code DB_URL}, {@code DB_USER}, {@code DB_PASSWORD}.
 * Якщо змінні не задані — використовуються значення за замовчуванням
 * для локальної розробки. <b>Абсолютних шляхів у коді немає</b>
 * (вимога переносимості курсової роботи).
 */
public final class DbConnection {

    private static final Logger log = LoggerFactory.getLogger(DbConnection.class);

    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/building_defects?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8&allowPublicKeyRetrieval=true";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "root";

    private DbConnection() {}

    public static Connection getConnection() throws SQLException {
        String url = envOrDefault("DB_URL", DEFAULT_URL);
        String user = envOrDefault("DB_USER", DEFAULT_USER);
        String password = envOrDefault("DB_PASSWORD", DEFAULT_PASSWORD);

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found", e);
        }

        log.debug("Connecting to {}", url);
        return DriverManager.getConnection(url, user, password);
    }

    private static String envOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            value = System.getProperty(key);
        }
        return (value == null || value.isBlank()) ? defaultValue : value;
    }
}
