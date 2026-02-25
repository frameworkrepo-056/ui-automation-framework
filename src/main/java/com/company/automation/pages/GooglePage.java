package com.company.automation.pages;

import org.openqa.selenium.By;

public class GooglePage extends BasePage {

    private final By searchBox = By.name("q");

    public void open(String url) {
        driver.get(url);
    }

    public void search(String query) {
        type(searchBox, query);
    }

    public void verifyTitleContains(String text) {
        waitForTitle(text);
    }
}