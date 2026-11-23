package com.ssafy.live.util;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/** JDBC 연결. DB_PASSWORD 환경 변수 또는 로컬 db.properties를 사용한다. */
public final class DBUtil {
    private static final Properties properties = new Properties();
    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (InputStream in = DBUtil.class.getResourceAsStream("/db.properties")) {
                if (in != null)
                    properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
            String config = System.getProperty("db.config", System.getenv().getOrDefault("DB_CONFIG", "db.properties"));
            Path file = Path.of(config);
            if (Files.isRegularFile(file))
                try (InputStream in = Files.newInputStream(file)) {
                    properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
                }
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }
    private DBUtil() {
    }
    public static synchronized void configure(Map<String, String> env) {
        for (String name : List.of("DB_URL", "DB_USERNAME", "DB_PASSWORD")) {
            if (env.containsKey(name))
                properties.setProperty(switch (name) {
                    case "DB_URL" -> "db.url";
                    case "DB_USERNAME" -> "db.username";
                    default -> "db.password";
                }, env.get(name));
        }
    }
    private static String setting(String key, String env, String fallback) {
        String value = System.getProperty(key);
        if (value == null)
            value = System.getenv(env);
        return value != null ? value : properties.getProperty(key, fallback);
    }
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(setting("db.url", "DB_URL",
                "jdbc:mysql://localhost:3306/ssafy_trip?characterEncoding=UTF-8&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true&useSSL=false"),
                setting("db.username", "DB_USERNAME", "root"), setting("db.password", "DB_PASSWORD", ""));
    }
}
