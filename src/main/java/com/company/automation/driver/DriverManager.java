package com.company.automation.driver;

import com.company.automation.exceptions.FrameworkException;
import com.company.automation.factory.BrowserFactory;
import com.company.automation.utils.ConfigReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;

import java.time.Duration;

public final class DriverManager {

    private static final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();
    private static final Logger logger = LogManager.getLogger(DriverManager.class);

    private DriverManager() {}

    public static void initDriver() {

        if (driverThreadLocal.get() != null) {
            return;
        }

        try {
            String browser = ConfigReader.get("browser");
            boolean headless = Boolean.parseBoolean(ConfigReader.get("headless"));

            logger.info("Initializing browser: {} | Headless: {} | Thread: {}",
                    browser, headless, Thread.currentThread().getId());

            WebDriver driver = BrowserFactory.createDriver(browser, headless);

            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            driver.manage().window().maximize();
            driver.manage().deleteAllCookies();

            driverThreadLocal.set(driver);

            logger.info("Browser launched successfully");

        } catch (Exception e) {
            logger.error("Failed to initialize WebDriver", e);
            throw new FrameworkException("Driver initialization failed", e);
        }
    }

    /**
     * Strict accessor – used inside test logic.
     * Fails fast if driver not initialized.
     */
    public static WebDriver getDriver() {

        WebDriver driver = driverThreadLocal.get();

        if (driver == null) {
            throw new FrameworkException(
                    "Driver is not initialized. Ensure initDriver() is called before usage.");
        }

        return driver;
    }

    /**
     * Safe accessor – used in teardown / defensive cleanup.
     * Returns null instead of throwing exception.
     */
    public static WebDriver getDriverOrNull() {
        return driverThreadLocal.get();
    }

    public static void quitDriver() {

        WebDriver driver = driverThreadLocal.get();

        if (driver != null) {
            driver.quit();
            driverThreadLocal.remove();
            logger.info("Driver closed successfully");
        } else {
            logger.warn("Attempted to quit driver but no driver was found for Thread: {}",
                    Thread.currentThread().getId());
        }
    }
}