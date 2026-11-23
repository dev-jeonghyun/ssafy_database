package com.ssafy.live.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.ServletContext;

public record ApiConfig(String kakaoMapKey) {
    public static ApiConfig load(ServletContext context) throws IOException {
        Map<String, String> values;
        try (InputStream input = context.getResourceAsStream("/WEB-INF/.env")) {
            values = parse(input);
        }
        return new ApiConfig(value(values, "KAKAO_MAP_KEY"));
    }

    public static Map<String, String> parse(InputStream input) throws IOException {
        Map<String, String> values = new HashMap<>();
        if (input == null) {
            return values;
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.replace("\uFEFF", "").trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("export ")) {
                line = line.substring(7).trim();
            }
            int equals = line.indexOf('=');
            if (equals < 1) {
                continue;
            }
            String name = line.substring(0, equals).trim();
            String value = line.substring(equals + 1).trim();
            if (value.length() >= 2 && ((value.startsWith("\"") && value.endsWith("\""))
                    || (value.startsWith("'") && value.endsWith("'")))) {
                value = value.substring(1, value.length() - 1);
            } else {
                int comment = value.indexOf(" #");
                if (comment >= 0) {
                    value = value.substring(0, comment).trim();
                }
            }
            values.put(name, value);
        }
        return values;
    }

    private static String value(Map<String, String> values, String name) {
        String environment = System.getenv(name);
        return environment != null && !environment.isBlank() ? environment.trim() : values.getOrDefault(name, "");
    }
}
