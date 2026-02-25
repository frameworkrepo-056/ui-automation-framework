package com.company.automation.stepdefinitions;

import com.company.automation.driver.DriverManager;
import com.company.automation.pages.GooglePage;
import com.company.automation.utils.ConfigReader;
import io.cucumber.java.en.Given;
import org.junit.jupiter.api.Assertions;

public class GoogleSteps {

    private final GooglePage googlePage = new GooglePage();

    @Given("I open the Google homepage")
    public void openGoogleHomepage() {
        System.out.println("Base URL: " + ConfigReader.get("base.url"));
        googlePage.open(ConfigReader.get("base.url"));

        String actualTitle = DriverManager.getDriver().getTitle();

        Assertions.assertEquals(
                "Some Wrong Title",
                actualTitle,
                "Intentional mismatch to verify Allure reporting"
        );
    }
}