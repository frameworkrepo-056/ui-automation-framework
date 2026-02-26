package com.company.automation.utils;

import com.company.automation.exceptions.FrameworkException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.util.Properties;

/**
 * Two-layer configuration loader.
 *
 * Load order (each layer overrides the previous):
 *   Layer 1 — config/config.properties                 (base defaults)
 *   Layer 2 — config/environments/{env}.properties     (env-specific overrides)
 *   Layer 3 — JVM system properties  -Dkey=value       (CLI always wins)
 *
 * Environment resolution order:
 *   1. CLI:  mvn test -Denv=staging
 *   2. Base: env=dev in config.properties
 *   3. Hardcoded default: dev
 */
public final class ConfigReader {

    private static final Logger logger = LogManager.getLogger(ConfigReader.class);
    private static final Properties merged = new Properties();
    private static final String ACTIVE_ENV;

    static {
        // Step 1: Check CLI for -Denv BEFORE reading any file
        //         (we need env to know which file to load next)
        String cliEnv = System.getProperty("env");

        // Step 2: Load base config.properties into a temporary holder
        Properties base = loadFile("config/config.properties");

        // Step 3: Resolve env — CLI wins, then base file, then hardcoded "dev"
        ACTIVE_ENV = (cliEnv != null && !cliEnv.isBlank())
                ? cliEnv.trim().toLowerCase()
                : base.getProperty("env", "dev").trim().toLowerCase();

        // Step 4: Load env-specific file on top
        Properties envOverride = loadFile("config/environments/" + ACTIVE_ENV + ".properties");

        // Step 5: Merge — base first, env values override
        merged.putAll(base);
        merged.putAll(envOverride);

        logBanner();
    }

    private ConfigReader() {}

    /**
     * Returns the resolved value for a property key.
     * Priority: -Dkey=value (CLI) > environments/{env}.properties > config.properties
     * Throws FrameworkException with a clear message if key is missing everywhere.
     */
    public static String get(String key) {
        // CLI system property always wins — checked at read-time, not load-time
        String cli = System.getProperty(key);
        if (cli != null && !cli.isBlank()) {
            return cli.trim();
        }

        String value = merged.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new FrameworkException(
                    "Property '" + key + "' not found in config.properties"
                            + " or environments/" + ACTIVE_ENV + ".properties."
                            + " Add it to the file or pass -D" + key + "=value on the CLI.");
        }
        return value.trim();
    }

    /**
     * Returns the value, or a default if not defined anywhere.
     * Never throws — safe for optional / feature-flag properties.
     */
    public static String getOrDefault(String key, String defaultValue) {
        try {
            return get(key);
        } catch (FrameworkException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the active environment name in lowercase.
     * e.g. "dev", "staging", "uat", "prod"
     */
    public static String getActiveEnvironment() {
        return ACTIVE_ENV;
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private static Properties loadFile(String classpathPath) {
        try (InputStream in = ConfigReader.class
                .getClassLoader().getResourceAsStream(classpathPath)) {

            if (in == null) {
                throw new FrameworkException(
                        "Config file not found on classpath: " + classpathPath
                                + "\nValid environments: dev | staging | uat | prod");
            }

            Properties p = new Properties();
            p.load(in);
            return p;

        } catch (FrameworkException e) {
            throw e;
        } catch (Exception e) {
            throw new FrameworkException("Failed to read: " + classpathPath, e);
        }
    }

    private static void logBanner() {
        logger.info("╔══════════════════════════════════╗");
        logger.info("║  Environment  : {}", pad(ACTIVE_ENV.toUpperCase()));
        logger.info("║  Base URL     : {}", pad(merged.getProperty("base.url", "NOT SET")));
        logger.info("║  Browser      : {}", pad(merged.getProperty("browser", "chrome")));
        logger.info("║  Headless     : {}", pad(merged.getProperty("headless", "false")));
        logger.info("╚══════════════════════════════════╝");
    }

    private static String pad(String v) {
        return String.format("%-20s║", v);
    }
}