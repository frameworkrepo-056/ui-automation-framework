package com.company.automation.retry;

import com.company.automation.utils.ConfigReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe attempt counter for Cucumber scenarios.
 *
 * retry.count=2 means:
 *   Attempt 1 → original run  (Phase 1)
 *   Attempt 2 → first retry   (Phase 2, run 1)
 *   Attempt 3 → second retry  (Phase 2, run 2) ← not possible in two-phase model
 *
 * In our Surefire two-phase model Phase 2 runs the failures exactly once,
 * so effectively retry.count acts as a toggle (1 = retry enabled).
 * The count is used for logging and Allure screenshot labelling.
 */
public final class RetryAnalyzer {

    private static final Logger logger = LogManager.getLogger(RetryAnalyzer.class);

    public static final int     MAX_RETRIES;
    public static final boolean RETRY_ENABLED;

    static {
        int retries;
        boolean enabled;
        try {
            retries = Integer.parseInt(ConfigReader.getOrDefault("retry.count", "2"));
            enabled = Boolean.parseBoolean(ConfigReader.getOrDefault("retry.enabled", "true"));
        } catch (Exception e) {
            retries = 2;
            enabled = true;
        }
        MAX_RETRIES   = retries;
        RETRY_ENABLED = enabled;
        logger.info("Retry config — enabled: {} | max retries: {}", RETRY_ENABLED, MAX_RETRIES);
    }

    // scenarioName → how many times @Before has fired for this scenario
    private static final ConcurrentHashMap<String, AtomicInteger> attemptCounters =
            new ConcurrentHashMap<>();

    private RetryAnalyzer() {}

    /**
     * Call in @Before(order=1) — increments attempt counter for this scenario.
     */
    public static void recordAttempt(String scenarioName) {
        counter(scenarioName).incrementAndGet();
    }

    /**
     * Returns 1-based attempt number.
     * 1 = first run, 2 = first retry, 3 = second retry
     */
    public static int getAttemptNumber(String scenarioName) {
        return counter(scenarioName).get();
    }

    /**
     * Returns true if another attempt is still available after a failure.
     * attempt 1 fails → shouldRetry=true  (retries remaining: MAX_RETRIES)
     * attempt MAX_RETRIES+1 fails → shouldRetry=false (exhausted)
     */
    public static boolean shouldRetry(String scenarioName) {
        if (!RETRY_ENABLED) return false;
        return getAttemptNumber(scenarioName) <= MAX_RETRIES;
    }

    /**
     * Total attempts allowed = 1 original + MAX_RETRIES
     * e.g. retry.count=2 → 3 total attempts
     */
    public static int getTotalAllowedAttempts() {
        return MAX_RETRIES + 1;
    }

    /**
     * Clears state after final pass or final failure.
     * Prevents state leaking between Surefire Phase 1 and Phase 2.
     */
    public static void reset(String scenarioName) {
        attemptCounters.remove(scenarioName);
    }

    public static int     getMaxRetries()      { return MAX_RETRIES; }
    public static boolean isRetryEnabled()     { return RETRY_ENABLED; }

    private static AtomicInteger counter(String name) {
        return attemptCounters.computeIfAbsent(name, k -> new AtomicInteger(0));
    }
}