package com.example.sams.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class EnvLoader {

    private EnvLoader() {
    }

    public static void load(String fileName) {
        Path envFile = Path.of(fileName);
        if (!Files.exists(envFile)) {
            return;
        }

        try {
            List<String> lines = Files.readAllLines(envFile, StandardCharsets.UTF_8);
            for (String rawLine : lines) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
                    continue;
                }

                int separatorIndex = line.indexOf('=');
                String key = line.substring(0, separatorIndex).trim();
                String value = stripQuotes(line.substring(separatorIndex + 1).trim());

                if (!key.isEmpty() && System.getenv(key) == null && System.getProperty(key) == null) {
                    System.setProperty(key, value);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load environment file: " + envFile.toAbsolutePath(), exception);
        }
    }

    private static String stripQuotes(String value) {
        if (value.length() >= 2) {
            boolean wrappedInDoubleQuotes = value.startsWith("\"") && value.endsWith("\"");
            boolean wrappedInSingleQuotes = value.startsWith("'") && value.endsWith("'");
            if (wrappedInDoubleQuotes || wrappedInSingleQuotes) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
