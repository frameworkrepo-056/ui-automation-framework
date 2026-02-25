package com.company.automation.pages;

import com.company.automation.driver.DriverManager;
import com.company.automation.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public abstract class BasePage {

    protected WebDriver driver;

    protected BasePage() {
        this.driver = DriverManager.getDriver();
    }

    protected void click(By locator) {
        WebElement element = WaitUtils.waitForClickability(driver, locator);
        element.click();
    }

    protected void type(By locator, String text) {
        WebElement element = WaitUtils.waitForVisibility(driver, locator);
        element.clear();
        element.sendKeys(text);
    }

    protected String getText(By locator) {
        return WaitUtils.waitForVisibility(driver, locator).getText();
    }

    protected void waitForTitle(String title) {
        WaitUtils.waitForTitleContains(driver, title);
    }
}