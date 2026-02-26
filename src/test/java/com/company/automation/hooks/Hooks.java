package com.company.automation.hooks;

import com.company.automation.driver.DriverManager;
import com.company.automation.retry.RetryAnalyzer;
import com.company.automation.utils.AllureEnvironmentWriter;
import com.company.automation.utils.ConfigReader;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.qameta.allure.Allure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.ByteArrayInputStream;
import java.util.concurrent.atomic.AtomicBoolean;

public class Hooks {

    private static final Logger logger = LogManager.getLogger(Hooks.class);
    private static final AtomicBoolean environmentWritten = new AtomicBoolean(false);

    @Before(order = 0)
    public void writeEnvironmentInfo() {
        if (environmentWritten.compareAndSet(false, true)) {
            AllureEnvironmentWriter.writeEnvironmentInfo();
        }
    }

    @Before(order = 1)
    public void setUp(Scenario scenario) {

        // ── NEW: record attempt and log which attempt this is ──
        RetryAnalyzer.recordAttempt(scenario.getName());
        int attempt = RetryAnalyzer.getAttemptNumber(scenario.getName());
        int total   = RetryAnalyzer.getTotalAllowedAttempts();

        logger.info("╔══════════════════════════════════════════╗");
        logger.info("║ Scenario : {}", scenario.getName());
        logger.info("║ Attempt  : {} of {}", attempt, total);
        logger.info("║ Thread   : {}", Thread.currentThread().getId());
        logger.info("║ Env      : {}", ConfigReader.getActiveEnvironment().toUpperCase());
        logger.info("╚══════════════════════════════════════════╝");

        DriverManager.initDriver();
    }

    @After
    public void tearDown(Scenario scenario) {

        WebDriver driver = DriverManager.getDriverOrNull();

        try {
            if (scenario.isFailed()) {
                handleFailure(scenario, driver);
            } else {
                logger.info("✅ PASSED : {}", scenario.getName());
                RetryAnalyzer.reset(scenario.getName()); // clear state on pass
            }

        } catch (Exception e) {
            logger.error("Error in tearDown: {}", scenario.getName(), e);

        } finally {
            DriverManager.quitDriver();
            logger.info("Finished  : {}", scenario.getName());
        }
    }

    private void handleFailure(Scenario scenario, WebDriver driver) {

        int attempt   = RetryAnalyzer.getAttemptNumber(scenario.getName());
        int total     = RetryAnalyzer.getTotalAllowedAttempts();
        boolean retry = RetryAnalyzer.shouldRetry(scenario.getName());

        if (retry) {
            logger.warn("❌ FAILED (attempt {} of {}) — queued for retry: {}",
                    attempt, total, scenario.getName());
        } else {
            logger.error("❌ FAILED (attempt {} of {}) — FINAL, no more retries: {}",
                    attempt, total, scenario.getName());
            RetryAnalyzer.reset(scenario.getName());
        }

        captureScreenshot(scenario, driver, attempt);
    }

    private void captureScreenshot(Scenario scenario, WebDriver driver, int attempt) {

        if (!(driver instanceof TakesScreenshot ss)) {
            logger.warn("Driver unavailable or does not support screenshots");
            return;
        }

        try {
            byte[] screenshot  = ss.getScreenshotAs(OutputType.BYTES);
            String safeName    = scenario.getName().replaceAll("\\W+", "_");
            String label       = safeName + "_attempt_" + attempt;

            // Attach to Cucumber HTML report
            scenario.attach(screenshot, "image/png", label);

            // Attach to Allure report — each attempt gets its own entry
            Allure.addAttachment(
                    "Failure Screenshot — Attempt " + attempt,
                    "image/png",
                    new ByteArrayInputStream(screenshot),
                    "png"
            );

            logger.info("📸 Screenshot captured: {}", label);

        } catch (Exception e) {
            logger.error("Screenshot capture failed", e);
        }
    }
}