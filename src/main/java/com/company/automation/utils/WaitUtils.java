package com.company.automation.utils;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public final class WaitUtils {

    private static final int DEFAULT_TIMEOUT =
            Integer.parseInt(ConfigReader.get("explicit.wait"));

    private WaitUtils() {}

    private static WebDriverWait getWait(WebDriver driver) {
        return new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT));
    }

    public static WebElement waitForVisibility(WebDriver driver, By locator) {
        return getWait(driver).until(
                ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public static WebElement waitForClickability(WebDriver driver, By locator) {
        return getWait(driver).until(
                ExpectedConditions.elementToBeClickable(locator));
    }

    public static void waitForTitleContains(WebDriver driver, String title) {
        getWait(driver).until(
                ExpectedConditions.titleContains(title));
    }
}