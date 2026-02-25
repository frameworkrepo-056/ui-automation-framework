package com.company.automation.hooks;

import com.company.automation.driver.DriverManager;
import com.company.automation.utils.AllureEnvironmentWriter;
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
    /**
     * AtomicBoolean ensures environment info is written exactly once
     * even when multiple threads hit @Before(order=0) simultaneously.
     * A plain boolean has a race condition — all threads can read false
     * before any writes true, causing concurrent file corruption.
     */
    private static final AtomicBoolean environmentWritten = new AtomicBoolean(false);


    @Before(order = 0)
    public void beforeAll() {
        // compareAndSet returns true only for the first thread that flips false → true
        if (environmentWritten.compareAndSet(false, true)) {
            AllureEnvironmentWriter.writeEnvironmentInfo();
        }
    }

    @Before(order = 1)
    public void setUp(Scenario scenario) {

        logger.info("==============================================");
        logger.info("Starting Scenario: {}", scenario.getName());
        logger.info("Thread ID: {}", Thread.currentThread().getId());

        DriverManager.initDriver();
    }

    @After
    public void tearDown(Scenario scenario) {

        WebDriver driver = DriverManager.getDriverOrNull();

        try {

            if (driver != null && scenario.isFailed()) {

                logger.error("Scenario FAILED: {}", scenario.getName());

                if (driver instanceof TakesScreenshot screenshotDriver) {

                    byte[] screenshot =
                            screenshotDriver.getScreenshotAs(OutputType.BYTES);

                    // Attach to Cucumber
                    scenario.attach(
                            screenshot,
                            "image/png",
                            scenario.getName().replaceAll(" ", "_")
                    );

                    // Attach to Allure
                    Allure.addAttachment(
                            "Failure Screenshot",
                            "image/png",
                            new ByteArrayInputStream(screenshot),
                            "png"
                    );
                }

            } else if (driver != null) {

                logger.info("Scenario PASSED: {}", scenario.getName());

            } else {

                logger.warn("Driver was not initialized. Skipping screenshot capture.");
            }

        } catch (Exception e) {
            logger.error("Error during teardown", e);
        } finally {

            DriverManager.quitDriver();

            logger.info("Finished Scenario: {}", scenario.getName());
            logger.info("==============================================");
        }
    }
}