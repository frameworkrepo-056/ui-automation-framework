package com.company.automation.factory;

import com.company.automation.exceptions.FrameworkException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public final class BrowserFactory {

    private BrowserFactory() {}

    public static WebDriver createDriver(String browser, boolean headless) {

        if (browser == null || browser.isBlank()) {
            throw new FrameworkException("Browser name cannot be null or empty");
        }

        switch (browser.toLowerCase()) {

            case "chrome":
                return createChromeDriver(headless);

            default:
                throw new FrameworkException(
                        "Unsupported browser: " + browser);
        }
    }

    private static WebDriver createChromeDriver(boolean headless) {

        ChromeOptions options = new ChromeOptions();

        if (headless) {
            options.addArguments("--headless=new");
        }

        options.addArguments("--start-maximized");

        return new ChromeDriver(options);
    }
}