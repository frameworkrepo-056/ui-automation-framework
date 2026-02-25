package com.company.automation.utils;

import com.company.automation.exceptions.FrameworkException;

import java.io.InputStream;
import java.util.Properties;

public final class ConfigReader {

    private static final Properties properties = new Properties();

    static {
        try (InputStream input =
                     ConfigReader.class
                             .getClassLoader()
                             .getResourceAsStream("config/config.properties")) {

            if (input == null) {
                throw new FrameworkException(
                        "config.properties not found in classpath under config/");
            }

            properties.load(input);

        } catch (Exception e) {
            throw new FrameworkException(
                    "Failed to load config.properties", e);
        }
    }

    private ConfigReader() {}

    public static String get(String key) {

        String systemValue = System.getProperty(key);
        if (systemValue != null && !systemValue.isBlank()) {
            return systemValue;
        }

        String value = properties.getProperty(key);

        if (value == null || value.isBlank()) {
            throw new FrameworkException(
                    "Property '" + key + "' is not defined in config.properties");
        }

        return value.trim();
    }
}