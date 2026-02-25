package com.company.automation.stepdefinitions;

import com.company.automation.driver.DriverManager;
import com.company.automation.utils.WaitUtils;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.junit.jupiter.api.Assertions;

public class ParallelIsolationSteps {

    @Given("I open {string}")
    public void iOpen(String url) {
        DriverManager.getDriver().get(url);
    }

    @Then("the page title should contain {string}")
    public void pageTitleShouldContain(String expected) {
        // Uses WaitUtils — consistent with rest of the framework
        WaitUtils.waitForTitleContains(DriverManager.getDriver(), expected);
        
        String actualTitle = DriverManager.getDriver().getTitle();
        System.out.printf(
                "[TITLE CHECK] Thread: %s | Expected: %s | Actual: %s%n",
                Thread.currentThread().getId(), expected, actualTitle
        );
        Assertions.assertTrue(
                actualTitle.contains(expected),
                "Title mismatch on thread " + Thread.currentThread().getId()
                        + " — possible driver leak! Got: " + actualTitle
        );
    }
}