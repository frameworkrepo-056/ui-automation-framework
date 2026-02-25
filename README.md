# UI Automation Framework

> A production-grade, parallel-enabled UI test automation framework built with **Cucumber 7**, **Selenium 4**, **JUnit 5 Platform**, and **Allure Reporting** — fully integrated with a **Jenkins CI/CD pipeline** and hosted on GitHub.

---

## Table of Contents

1. [Overview](#1-overview)
2. [Tech Stack](#2-tech-stack)
3. [Prerequisites](#3-prerequisites)
4. [Project Structure](#4-project-structure)
5. [Framework Architecture](#5-framework-architecture)
   - [Core Layer (src/main)](#51-core-layer-srcmain)
   - [Test Layer (src/test)](#52-test-layer-srctest)
   - [How the layers connect](#53-how-the-layers-connect)
6. [Configuration](#6-configuration)
7. [Writing Tests — Step by Step](#7-writing-tests--step-by-step)
8. [Tagging Strategy](#8-tagging-strategy)
9. [Running Tests Locally](#9-running-tests-locally)
10. [Parallel Execution Deep Dive](#10-parallel-execution-deep-dive)
11. [Allure Reporting](#11-allure-reporting)
12. [Logging](#12-logging)
13. [Jenkins CI/CD Pipeline](#13-jenkins-cicd-pipeline)
14. [Git Branch Strategy](#14-git-branch-strategy)
15. [Extending the Framework](#15-extending-the-framework)
16. [Troubleshooting](#16-troubleshooting)

---

## 1. Overview

This framework provides a structured, scalable approach to browser-based UI test automation following the **Behaviour-Driven Development (BDD)** methodology. Tests are written in plain English using Gherkin syntax, making them readable by both technical and non-technical stakeholders.

**What makes this framework production-ready:**

- **Parallel execution** — multiple browser instances run simultaneously, drastically cutting total test suite time
- **Thread-safe isolation** — each parallel thread gets its own dedicated `WebDriver` instance via `ThreadLocal`; no shared state, no race conditions
- **Headless CI/CD execution** — runs with zero display requirement in any CI environment
- **Automatic reporting** — `mvn test` generates a full interactive Allure report with screenshots, timeline, and trend graphs
- **Config-driven** — zero hardcoded values; every setting can be overridden from the command line without touching code
- **Auto screenshot on failure** — failed scenarios automatically capture and attach browser screenshots to the Allure report
- **Jenkins pipeline** — parameterised builds with browser, tag, thread, and headless options

---

## 2. Tech Stack

| Technology | Version | Role |
|---|---|---|
| **Java** | 21 LTS | Core language |
| **Maven** | 3.9.x | Build tool and dependency management |
| **Selenium WebDriver** | 4.18.1 | Browser automation engine |
| **WebDriverManager** | 5.7.0 | Automatic ChromeDriver binary management |
| **Cucumber** | 7.15.0 | BDD framework — Gherkin parsing and step wiring |
| **JUnit 5 Platform** | 5.10.2 | Test runner, parallel engine, lifecycle hooks |
| **Allure** | 2.25.0 | Interactive test reporting |
| **Log4j2** | 2.22.1 | Thread-safe parallel logging |
| **Jenkins** | 2.x LTS | CI/CD pipeline automation |
| **Git / GitHub** | — | Source control and remote repository |

---

## 3. Prerequisites

The following must be installed before running this framework.

| Tool | Minimum Version | How to verify | Download |
|---|---|---|---|
| JDK | 21 (LTS) | `java -version` | https://adoptium.net |
| Maven | 3.9+ | `mvn -version` | https://maven.apache.org |
| Google Chrome | Latest stable | — | https://www.google.com/chrome |
| Git | 2.x | `git --version` | https://git-scm.com |

> **Note:** ChromeDriver is downloaded **automatically** by WebDriverManager at runtime. You do not need to manually download or configure ChromeDriver.

---

## 4. Project Structure

```
ui-automation-framework/
│
├── src/
│   │
│   ├── main/                                        ← CORE FRAMEWORK (reusable infrastructure)
│   │   └── java/com/company/automation/
│   │       │
│   │       ├── constants/
│   │       │   └── FrameworkConstants.java          # Shared string constants (browser names, paths)
│   │       │
│   │       ├── driver/
│   │       │   └── DriverManager.java               # Thread-safe WebDriver lifecycle (ThreadLocal)
│   │       │
│   │       ├── exceptions/
│   │       │   └── FrameworkException.java          # Custom unchecked exception for framework errors
│   │       │
│   │       ├── factory/
│   │       │   └── BrowserFactory.java              # Factory pattern — creates Chrome instances
│   │       │
│   │       ├── pages/
│   │       │   ├── BasePage.java                    # Abstract base class all page objects extend
│   │       │   └── GooglePage.java                  # Page Object for Google homepage
│   │       │
│   │       └── utils/
│   │           ├── AllureEnvironmentWriter.java     # Writes runtime env info to Allure results
│   │           ├── ConfigReader.java                # Reads config.properties; system props override
│   │           └── WaitUtils.java                   # Centralised explicit wait utilities
│   │
│   └── test/                                        ← TEST LAYER (scenario-specific code)
│       ├── java/com/company/automation/
│       │   │
│       │   ├── hooks/
│       │   │   └── Hooks.java                       # @Before / @After — driver init, screenshots
│       │   │
│       │   ├── runner/
│       │   │   └── RunCucumberTest.java             # JUnit 5 Suite entry point for Surefire
│       │   │
│       │   └── stepdefinitions/
│       │       ├── GoogleSteps.java                 # Step definitions for google.feature
│       │       └── ParallelIsolationSteps.java      # Step definitions for parallel verification
│       │
│       └── resources/
│           ├── features/
│           │   ├── google.feature                   # Google Search scenarios
│           │   └── parallel_isolation.feature       # Parallel thread isolation verification
│           │
│           ├── config/
│           │   └── config.properties                # Runtime configuration (browser, url, waits)
│           │
│           ├── junit-platform.properties            # JUnit 5 parallel execution settings
│           └── log4j2.xml                           # Logging configuration
│
├── Jenkinsfile                                      # Declarative Jenkins pipeline definition
├── pom.xml                                          # Maven build and dependency configuration
├── .gitignore                                       # Excludes target/, logs/, IDE files
└── README.md                                        # This file
```

---

## 5. Framework Architecture

The framework is split into two distinct layers. The **Core Layer** knows nothing about specific tests. The **Test Layer** knows nothing about browser setup. This separation means you can add hundreds of test scenarios without ever touching the framework infrastructure.

### 5.1 Core Layer (`src/main`)

#### `DriverManager` — Thread-safe WebDriver Lifecycle

The most important class in the framework. Uses `ThreadLocal<WebDriver>` to give each parallel thread its own completely isolated browser instance.

```
Parallel run with 4 threads:

Thread 31 → ChromeDriver instance A  (only Thread 31 can access this)
Thread 33 → ChromeDriver instance B  (only Thread 33 can access this)
Thread 35 → ChromeDriver instance C  (only Thread 35 can access this)
Thread 37 → ChromeDriver instance D  (only Thread 37 can access this)
```

**Key methods:**

| Method | When to call | Behaviour |
|---|---|---|
| `initDriver()` | `@Before` hook only | Creates driver for current thread. No-op if already initialised. |
| `getDriver()` | Inside step definitions via page objects | Returns driver for current thread. Throws `FrameworkException` if not initialised — **fail fast**. |
| `getDriverOrNull()` | `@After` teardown only | Returns driver or `null`. Never throws. Safe for cleanup code. |
| `quitDriver()` | `@After` hook only | Quits driver and removes from `ThreadLocal`. Prevents memory leaks. |

> **Rule:** Never call `DriverManager` directly from step definitions. Always go through a Page Object.

---

#### `BrowserFactory` — Browser Creation

Implements the **Factory Pattern** to decouple browser creation from the rest of the framework. Currently supports Chrome with the `--headless=new` flag (Chrome's modern headless mode).

```java
// Adding a new browser is simply adding a case:
switch (browser.toLowerCase()) {
    case "chrome":  return createChromeDriver(headless);
    case "firefox": return createFirefoxDriver(headless);  // extend here
    case "edge":    return createEdgeDriver(headless);     // extend here
}
```

Chrome options configured:
- `--headless=new` when headless is enabled (modern headless, not deprecated legacy mode)
- `--start-maximized` for consistent viewport size

---

#### `BasePage` — Abstract Page Object Base

All Page Objects extend `BasePage`. It wraps raw Selenium calls with built-in explicit waits so that:
- Step definitions never import `WebElement`, `By`, or `ExpectedConditions`
- All waits go through `WaitUtils` — consistent timeout behaviour everywhere

```java
// What BasePage provides to all page objects:
protected void click(By locator)              // waits for clickable → clicks
protected void type(By locator, String text)  // waits for visible → clears → types
protected String getText(By locator)          // waits for visible → returns text
protected void waitForTitle(String title)     // waits until page title contains text
```

The constructor automatically fetches the current thread's driver from `DriverManager.getDriver()` — so page objects are always bound to the correct thread's browser.

---

#### `ConfigReader` — Externalised Configuration

Loads `config.properties` once at class initialisation (static block). Every value can be overridden by a JVM system property, enabling CI/CD to control behaviour without file edits.

```
Priority order (highest wins):
  1. CLI system property:  -Dbrowser=firefox
  2. config.properties:    browser=chrome
```

Throws `FrameworkException` with a clear message if a key is missing — no silent `null` returns anywhere in the framework.

---

#### `WaitUtils` — Explicit Wait Utilities

Centralised wait logic. The timeout is read from `config.properties` (`explicit.wait` key) once at startup — consistent across all waits.

| Method | Waits until |
|---|---|
| `waitForVisibility(driver, locator)` | Element is present in DOM and visible |
| `waitForClickability(driver, locator)` | Element is visible and not disabled |
| `waitForTitleContains(driver, title)` | Page title contains the given string |

> **Never use `Thread.sleep()` in tests.** Always use `WaitUtils`.

---

#### `AllureEnvironmentWriter` — Report Environment Info

Writes two files to `target/allure-results/` before any test runs:

- `environment.properties` — Browser, Headless mode, Base URL, OS, Java version, username
- `executor.json` — Build metadata shown in Allure's Executor widget

Uses `AtomicBoolean.compareAndSet(false, true)` in `Hooks` so this runs exactly once even when 4+ threads hit `@Before(order = 0)` simultaneously.

---

#### `FrameworkConstants` — String Constants

Holds shared constant strings to avoid magic strings scattered across the codebase:

```java
public static final String CHROME  = "chrome";
public static final String FIREFOX = "firefox";
public static final String EDGE    = "edge";
```

---

#### `FrameworkException` — Custom Unchecked Exception

Extends `RuntimeException`. Used throughout the framework to wrap checked exceptions and provide meaningful error messages. Makes stack traces immediately informative rather than showing generic `NullPointerException`.

---

### 5.2 Test Layer (`src/test`)

#### `RunCucumberTest` — Suite Entry Point

The single class that Surefire discovers and runs. It is annotated with `@Suite` and `@IncludeEngines("cucumber")`, making it the **sole gateway** through which all test discovery flows.

```java
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")          // scan all .feature files here
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.company.automation")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty, summary, io.qameta.allure...")
public class RunCucumberTest {}
```

The `<includes>` directive in `pom.xml`'s Surefire configuration locks discovery to only this class, preventing the double-execution bug that occurs when the raw Cucumber engine fires independently alongside the Suite runner.

---

#### `Hooks` — Lifecycle Management

Runs before and after every scenario. Manages driver setup, environment writing, screenshot capture, and teardown.

**Execution order per scenario:**

```
@Before(order = 0) → Write Allure environment info (once, thread-safe AtomicBoolean)
@Before(order = 1) → Log scenario name + thread ID → DriverManager.initDriver()
      ↓
   [Scenario steps execute]
      ↓
@After             → If FAILED: capture screenshot → attach to Cucumber + Allure
                   → finally: DriverManager.quitDriver() (always runs, even on error)
```

Screenshot attachment uses dual attachment — attached to both the Cucumber HTML report and the Allure report, ensuring screenshots appear regardless of which report you view.

---

#### Step Definitions

Step definitions wire Gherkin steps to page object method calls. They should be **thin glue code only** — no Selenium, no `WebDriver`, no `WebElement`.

```java
// Correct pattern — delegates everything to page object
@Given("I open the Google homepage")
public void openGoogleHomepage() {
    googlePage.open(ConfigReader.get("base.url"));
}
```

Step definitions are auto-discovered. Any class in the `com.company.automation` package containing `@Given`, `@When`, `@Then` annotations is picked up automatically — no registration required.

---

### 5.3 How the Layers Connect

```
RunCucumberTest (entry point)
        │
        ▼
Hooks.@Before ──► DriverManager.initDriver() ──► BrowserFactory.createDriver()
                                                          │
                                                          ▼
                                                   ChromeDriver (Thread N)
                                                          │
                                          stored in ThreadLocal<WebDriver>
                                                          │
Cucumber Step ──► StepDefinition ──► PageObject extends BasePage
                                              │
                                    BasePage constructor calls
                                    DriverManager.getDriver()
                                    (gets THIS thread's driver)
                                              │
                                              ▼
                                       WaitUtils ──► Selenium action
                                              │
Hooks.@After ◄── scenario result ◄── assertion result
        │
        ▼
Screenshot (on fail) ──► Allure attachment
DriverManager.quitDriver() ──► ThreadLocal.remove()
```

---

## 6. Configuration

All runtime settings are in `src/test/resources/config/config.properties`:

```properties
# Browser to launch: chrome (firefox and edge require BrowserFactory extension)
browser=chrome

# Run without a visible browser window: true = headless, false = headed (local dev)
headless=false

# Maximum seconds to wait for elements/conditions before timing out
explicit.wait=10

# Base URL used by page objects as the starting point
base.url=https://www.google.com
```

**Every property can be overridden at runtime from the CLI without editing the file:**

```cmd
mvn test -Dbrowser=chrome -Dheadless=true -Dexplicit.wait=15 -Dbase.url=https://staging.myapp.com
```

**Parallel thread count** is controlled separately via Maven property:

```cmd
mvn test -Dparallel.threads=8
```

Or permanently in `pom.xml`:
```xml
<parallel.threads>4</parallel.threads>
```

---

## 7. Writing Tests — Step by Step

Follow these three steps to add new tests. You never need to modify the framework infrastructure.

### Step 1 — Create a Feature File

Create a `.feature` file in `src/test/resources/features/`. Group related scenarios in the same file. Tag at both Feature and Scenario level.

```gherkin
@regression
Feature: User Login

  @smoke @login
  Scenario: Successful login with valid credentials
    Given I navigate to the login page
    When I enter username "testuser" and password "SecurePass123"
    Then I should be on the dashboard page
    And the welcome message should contain "testuser"

  @login @negative
  Scenario: Login fails with invalid password
    Given I navigate to the login page
    When I enter username "testuser" and password "wrongpass"
    Then I should see the error message "Invalid credentials"
```

### Step 2 — Create a Page Object

Create a class in `src/main/java/com/company/automation/pages/` that extends `BasePage`. Define all locators as private fields. Expose behaviour through public methods — never expose raw locators or `WebElement` objects.

```java
package com.company.automation.pages;

import org.openqa.selenium.By;

public class LoginPage extends BasePage {

    // Locators — private, never exposed outside this class
    private final By usernameField  = By.id("username");
    private final By passwordField  = By.id("password");
    private final By loginButton    = By.cssSelector("button[type='submit']");
    private final By errorMessage   = By.className("error-message");
    private final By welcomeBanner  = By.id("welcome-banner");

    public void navigateTo(String url) {
        driver.get(url);
    }

    public void enterCredentials(String username, String password) {
        type(usernameField, username);
        type(passwordField, password);
    }

    public void clickLogin() {
        click(loginButton);
    }

    public String getErrorMessage() {
        return getText(errorMessage);
    }

    public String getWelcomeMessage() {
        return getText(welcomeBanner);
    }
}
```

### Step 3 — Create Step Definitions

Create a step definitions class in `src/test/java/com/company/automation/stepdefinitions/`. Instantiate page objects as fields. Keep each method to 1–3 lines — only page object calls and assertions.

```java
package com.company.automation.stepdefinitions;

import com.company.automation.pages.LoginPage;
import com.company.automation.utils.ConfigReader;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.jupiter.api.Assertions;

public class LoginSteps {

    // Instantiated per scenario — safe for parallel use (no static fields)
    private final LoginPage loginPage = new LoginPage();

    @Given("I navigate to the login page")
    public void navigateToLoginPage() {
        loginPage.navigateTo(ConfigReader.get("base.url") + "/login");
    }

    @When("I enter username {string} and password {string}")
    public void enterCredentials(String username, String password) {
        loginPage.enterCredentials(username, password);
        loginPage.clickLogin();
    }

    @Then("I should be on the dashboard page")
    public void verifyDashboard() {
        loginPage.waitForTitle("Dashboard");
    }

    @And("the welcome message should contain {string}")
    public void verifyWelcomeMessage(String expectedUser) {
        Assertions.assertTrue(
            loginPage.getWelcomeMessage().contains(expectedUser),
            "Welcome message did not contain: " + expectedUser
        );
    }

    @Then("I should see the error message {string}")
    public void verifyErrorMessage(String expectedError) {
        Assertions.assertEquals(expectedError, loginPage.getErrorMessage());
    }
}
```

> **No further registration needed.** Cucumber auto-discovers all step definition classes in the `com.company.automation` package via the `@ConfigurationParameter` on `RunCucumberTest`.

---

## 8. Tagging Strategy

Tags categorise scenarios and control which tests run in any given execution. Apply tags at both the Feature level (affects all scenarios) and individual Scenario level.

```gherkin
@regression                          ← Feature-level tag: all scenarios inherit this
Feature: Shopping Cart

  @smoke @cart                       ← Scenario has: @regression, @smoke, @cart
  Scenario: Add item to cart
    ...

  @cart @negative                    ← Scenario has: @regression, @cart, @negative
  Scenario: Cannot add out-of-stock item
    ...

  @wip                               ← Work in progress — excluded from CI
  Scenario: Apply discount coupon
    ...
```

**Recommended tag taxonomy:**

| Tag | Scope | Purpose |
|---|---|---|
| `@regression` | Feature | Full regression suite — every scenario |
| `@smoke` | Scenario | Critical path — 5–10 key scenarios, runs on every commit |
| `@login` | Scenario | Login feature area |
| `@cart` | Scenario | Shopping cart feature area |
| `@negative` | Scenario | Negative / error path tests |
| `@wip` | Scenario | In development — excluded from all CI runs |
| `@ignore` | Scenario | Permanently disabled |

**CLI tag filter examples:**

```cmd
# Run only smoke tests (fast CI gate)
mvn test "-Dcucumber.filter.tags=@smoke"

# Run full regression suite
mvn test "-Dcucumber.filter.tags=@regression"

# Run regression but skip work-in-progress
mvn test "-Dcucumber.filter.tags=@regression and not @wip"

# Run login OR cart tests
mvn test "-Dcucumber.filter.tags=@login or @cart"

# Run smoke tests on staging
mvn test "-Dcucumber.filter.tags=@smoke" -Dbase.url=https://staging.myapp.com -Dheadless=true
```

> ⚠️ **Important:** Always use `-Dcucumber.filter.tags` for filtering — never use `-Dcucumber.features`. The `-Dcucumber.features` property bypasses the Suite runner and causes every scenario to execute twice.

---

## 9. Running Tests Locally

### Clone and set up

```cmd
git clone https://github.com/frameworkrepo-056/ui-automation-framework.git
cd ui-automation-framework
git checkout development
```

### Basic execution commands

```cmd
rem Run all scenarios with defaults (headed Chrome, 4 threads)
mvn test

rem Run headless (no browser window — faster)
mvn test -Dheadless=true

rem Run with 8 parallel threads
mvn test -Dparallel.threads=8

rem Run specific tag
mvn test "-Dcucumber.filter.tags=@smoke"

rem Run specific tag, headless, 6 threads
mvn test "-Dcucumber.filter.tags=@regression" -Dheadless=true -Dparallel.threads=6

rem Full clean build
mvn clean test

rem Generate report without re-running tests
mvn allure:report

rem Open report in browser immediately after generation
mvn allure:serve
```

### After the run, find your outputs here:

| Output | Location |
|---|---|
| Allure raw results | `target/allure-results/` |
| Allure HTML report | `target/site/allure-maven-plugin/index.html` |
| Log file | `target/logs/automation.log` |
| Surefire XML results | `target/surefire-reports/` |

---

## 10. Parallel Execution Deep Dive

### Configuration

Parallel settings are split between two files that work together:

**`src/test/resources/junit-platform.properties`** — primary config:
```properties
# Enable parallel execution at the JUnit Platform level
junit.jupiter.execution.parallel.enabled=true

# Use a fixed thread count (not dynamic CPU-based)
junit.jupiter.execution.parallel.config.strategy=fixed
junit.jupiter.execution.parallel.config.fixed.parallelism=4

# Run scenarios concurrently (not sequentially)
junit.jupiter.execution.parallel.mode.default=CONCURRENT
junit.jupiter.execution.parallel.mode.classes.default=CONCURRENT

# Enable Cucumber's parallel scenario distribution
cucumber.execution.parallel.enabled=true
```

**`pom.xml`** — Surefire passes these as configurationParameters (fallback/override):
```xml
<configurationParameters>
    junit.jupiter.execution.parallel.enabled=true
    junit.jupiter.execution.parallel.config.strategy=fixed
    junit.jupiter.execution.parallel.config.fixed.parallelism=${parallel.threads}
    cucumber.execution.parallel.enabled=true
</configurationParameters>
```

### Thread safety mechanisms

| Component | Problem solved | Solution |
|---|---|---|
| `DriverManager` | Multiple threads sharing one driver | `ThreadLocal<WebDriver>` — one driver per thread |
| `Hooks.environmentWritten` | Multiple threads all writing env info | `AtomicBoolean.compareAndSet(false, true)` — write-once |
| Log4j2 file appender | Log corruption from concurrent writes | `RollingRandomAccessFile` — lock-free, async-safe |
| `ConfigReader` | Thread-safe read access | Static `Properties` loaded once — safe for concurrent reads |
| Page objects | Instance state corruption | New instance created per step definition class (not static) |

### Forcing a scenario to be sequential

If a scenario writes to a shared resource (e.g., a test database), annotate its step definition class to pin it to the main thread:

```java
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.SAME_THREAD)
public class DatabaseSteps {
    // This class will never run in parallel with other scenarios
}
```

### Memory guidelines

Each Chrome instance uses approximately 200–400MB of RAM. Set JVM heap accordingly in `pom.xml`:

| Threads | Recommended `-Xmx` |
|---|---|
| 4 | `-Xmx2048m` |
| 8 | `-Xmx4096m` |
| 12 | `-Xmx6144m` |

```xml
<argLine>-Xms512m -Xmx2048m --add-opens java.base/java.lang=ALL-UNNAMED</argLine>
```

---

## 11. Allure Reporting

### Generating reports

```cmd
rem Reports generate automatically as part of mvn test (via verify phase)
mvn test

rem Generate static HTML report manually
mvn allure:report

rem Launch as live web server (auto-opens browser)
mvn allure:serve
```

### Report sections explained

| Section | What it shows |
|---|---|
| **Overview** | Total scenarios, pass/fail/skip breakdown, trend graph across builds |
| **Suites** | Each scenario with all step results, duration, and attached screenshots |
| **Behaviors** | Results grouped by Feature and Scenario name |
| **Timeline** | Visual timeline proving parallel execution — bars overlap when running simultaneously |
| **Graphs** | Duration distribution, status trends, retry analysis |
| **Environment** | Browser, URL, OS, Java version from `AllureEnvironmentWriter` |

### Screenshots on failure

When any scenario fails, `Hooks.tearDown()` automatically:
1. Captures the current browser state as a PNG screenshot
2. Attaches it to the **Cucumber** HTML report (named after the scenario)
3. Attaches it to the **Allure** report as "Failure Screenshot"

No configuration is needed — this works for all scenarios automatically.

### Environment tab contents

The Environment widget in every Allure report shows:

```
Browser       chrome
Headless      true
Base URL      https://www.google.com
OS            Windows 11
Java Version  21.0.x
User          jenkins
```

---

## 12. Logging

Log4j2 is configured in `src/test/resources/log4j2.xml` with two appenders:

**Console appender** — for terminal output during test runs:
```
15:22:17 [ForkJoinPool-3-worker-2] INFO  Hooks - Starting Scenario: Open Google homepage
15:22:17 [ForkJoinPool-3-worker-2] INFO  Hooks - Thread ID: 35
15:22:17 [ForkJoinPool-3-worker-2] INFO  DriverManager - Initializing browser: chrome | Headless: true | Thread: 35
```

**File appender** — written to `target/logs/automation.log`:
```
2026-02-25 15:22:23 [Thread-35] INFO  DriverManager - Browser launched successfully
2026-02-25 15:22:25 [Thread-35] INFO  Hooks - Scenario PASSED: Open Google homepage
```

The file appender uses `RollingRandomAccessFile` — a lock-free implementation safe for concurrent writes from multiple threads. Rolls over at 20MB, keeps 5 archives compressed as `.gz`.

The `[Thread-N]` prefix in every log line lets you trace the complete story of any single scenario through the log even when 6 scenarios are interleaved.

---

## 13. Jenkins CI/CD Pipeline

### Setup summary

| Setting | Value |
|---|---|
| Jenkins URL | `http://localhost:8080` |
| Job name | `ui-automation-framework` |
| Source | `Jenkinsfile` in repository root |
| Watched branch | `development` |
| Trigger | Manual or GitHub push webhook |

### Pipeline stages

```
┌─────────────────────────────────────────────────────────────────────┐
│  Checkout SCM → Tool Install → Checkout → Build → Test →            │
│  Generate Allure Report → Archive Reports                            │
└─────────────────────────────────────────────────────────────────────┘
```

| Stage | What happens | Failure behaviour |
|---|---|---|
| **Checkout SCM** | Jenkins fetches `Jenkinsfile` from `development` | Pipeline aborts |
| **Tool Install** | Downloads Maven 3.9.x and confirms JDK 21 | Pipeline aborts |
| **Checkout** | Explicit `git` step to pin to `development` branch | Pipeline aborts |
| **Build** | `mvn clean compile test-compile` — compile verification | Pipeline aborts |
| **Test** | `mvn test` with selected parameters via `catchError` | Build marked `UNSTABLE`, pipeline **continues** |
| **Generate Allure Report** | `mvn allure:report` — builds HTML from raw results | Pipeline continues |
| **Archive Reports** | Publishes Allure plugin report + zips HTML + archives logs | Pipeline ends |

> The Test stage uses `catchError(buildResult: 'UNSTABLE', stageResult: 'UNSTABLE')` so that test failures mark the build yellow (unstable) but do not abort the pipeline — the Allure report is **always generated** regardless of test outcomes.

### Build parameters

Every build exposes these parameters via **Build with Parameters**:

| Parameter | Type | Default | Description |
|---|---|---|---|
| `BROWSER` | Choice | `chrome` | Browser: `chrome`, `firefox`, `edge` |
| `TAGS` | String | *(empty = all)* | Cucumber tag expression e.g. `@smoke`, `@regression and not @wip` |
| `THREADS` | String | `4` | Number of parallel browser threads |
| `HEADLESS` | Boolean | `true` | Headless mode — always true in CI |

### Triggering builds

**Manual:**
1. Open `http://localhost:8080/job/ui-automation-framework`
2. Click **Build with Parameters**
3. Set browser, tags, threads, headless
4. Click **Build**

**Automatic on push:**
Jenkins is configured with **GitHub hook trigger for GITScm polling**. Any push to the `development` branch automatically triggers a full pipeline run.

### Viewing reports in Jenkins

After a build completes:

- **Interactive Allure report:** Click the build number → click **Allure Report** in the left sidebar
- **Downloadable zip:** Click the build number → click **Build Artifacts** → download `allure-report-N.zip`
- **Raw logs:** Click the build number → click **Console Output**
- **Stage breakdown:** Click the build number → click **Pipeline Overview**

### Build status meanings

| Status | Colour | Meaning |
|---|---|---|
| `SUCCESS` | 🟢 Green | All pipeline stages passed, all tests passed |
| `UNSTABLE` | 🟡 Yellow | Pipeline ran fine, but some test scenarios failed |
| `FAILURE` | 🔴 Red | A pipeline stage itself failed (compilation error, network issue, etc.) |
| `ABORTED` | ⚫ Grey | Build was manually cancelled |

---

## 14. Git Branch Strategy

```
main          ← stable, production-ready only — no direct commits
development   ← active integration branch — all PRs merge here
feature/xxx   ← individual feature branches — branched from development
```

### Standard workflow

```cmd
rem 1. Always start from an up-to-date development branch
git checkout development
git pull origin development

rem 2. Create your feature branch with a descriptive name
git checkout -b feature/add-login-tests

rem 3. Write your tests (feature file + page object + step definitions)
rem    Run locally to confirm they pass:
mvn test "-Dcucumber.filter.tags=@login" -Dheadless=true

rem 4. Commit with a conventional commit message
git add .
git commit -m "feat: add login page tests with @smoke and @login tags"

rem 5. Push and open a Pull Request on GitHub
git push -u origin feature/add-login-tests
```

Then raise a **Pull Request** on GitHub: `feature/add-login-tests` → `development`. Jenkins auto-runs the pipeline when the PR is merged.

### Commit message conventions

| Prefix | Use for |
|---|---|
| `feat:` | New feature file, page object, or step definitions |
| `fix:` | Bug fix in test logic or framework code |
| `ci:` | Jenkinsfile or Maven build configuration |
| `refactor:` | Code restructuring with no behaviour change |
| `test:` | Adding or modifying test utilities |
| `docs:` | README or documentation updates |

---

## 15. Extending the Framework

### Adding a new browser (e.g. Firefox)

**Step 1** — Add Firefox dependency to `pom.xml`:
```xml
<dependency>
    <groupId>org.seleniumhq.selenium</groupId>
    <artifactId>selenium-firefox-driver</artifactId>
    <version>${selenium.version}</version>
</dependency>
```

**Step 2** — Add a case to `BrowserFactory.java`:
```java
case "firefox":
    FirefoxOptions options = new FirefoxOptions();
    if (headless) options.addArguments("-headless");
    return new FirefoxDriver(options);
```

**Step 3** — Run with Firefox:
```cmd
mvn test -Dbrowser=firefox -Dheadless=true
```

### Adding a new page

1. Create `MyAppPage.java` extending `BasePage` in `src/main/java/.../pages/`
2. Define `By` locators as private fields
3. Expose behaviour as public methods using `click()`, `type()`, `getText()`, `waitForTitle()`

### Adding a new feature

1. Create `my-feature.feature` in `src/test/resources/features/`
2. Create step definitions class in `src/test/java/.../stepdefinitions/`
3. No runner or configuration changes needed

### Adding custom Allure annotations

Enrich Allure reports with metadata by annotating step definitions:

```java
import io.qameta.allure.*;

@Feature("Login")
@Story("Valid credentials login")
@Severity(SeverityLevel.CRITICAL)
@Owner("QA Team")
@Step("User logs in with username {username}")
public void login(String username) { ... }
```

---

## 16. Troubleshooting

### Scenarios run twice

**Symptom:** You see `6 Scenarios (6 passed)` but you only have 3 scenarios.

**Cause:** You used `-Dcucumber.features` which bypasses the Suite runner and fires the raw Cucumber engine independently alongside `RunCucumberTest`.

**Fix:** Never use `-Dcucumber.features`. Use tags:
```cmd
# Wrong
mvn test "-Dcucumber.features=src/test/resources/features/google.feature"

# Correct
mvn test "-Dcucumber.filter.tags=@google"
```

---

### `Driver is not initialized` exception

**Cause:** A step definition (or its page object constructor) is calling `DriverManager.getDriver()` before `@Before(order = 1)` has run, or after `@After` has already called `quitDriver()`.

**Fix:** Only initialise `DriverManager` from `Hooks.setUp()`. Never call it from constructors of page objects referenced at field level in step definitions. The `BasePage` constructor safely calls `getDriver()` — this works because `BasePage` is only instantiated inside step methods which run after `@Before`.

---

### CDP version warning in logs

```
WARNING: Unable to find CDP implementation matching 145
```

**Cause:** Harmless version mismatch between Selenium 4.18.1 and Chrome 145. Selenium ships CDP bindings for specific Chrome versions.

**Impact:** None — all browser automation works normally.

**Fix:** Update Selenium version in `pom.xml` when a version matching your Chrome release is available.

---

### `Cannot run program "sh"` in Jenkins

**Cause:** The Jenkinsfile contains a `sh` step but Jenkins is running on Windows which has no `sh` binary.

**Fix:** All shell commands in `Jenkinsfile` must use `bat` not `sh`. The current `Jenkinsfile` already uses `bat` throughout.

---

### Allure report missing from Jenkins build page

**Cause:** Jenkins Content Security Policy (CSP) blocks Allure's JavaScript from rendering.

**Fix:** Go to **Manage Jenkins → Script Console** and run:
```groovy
System.setProperty("hudson.model.DirectoryBrowserSupport.CSP", "")
```
Then re-run the build.

---

### Out of memory with many parallel threads

**Symptom:** `java.lang.OutOfMemoryError: Java heap space` during test run.

**Fix:** Increase heap in `pom.xml`:
```xml
<argLine>-Xms512m -Xmx4096m --add-opens java.base/java.lang=ALL-UNNAMED</argLine>
```

Rule of thumb: allocate at least 400MB per parallel thread.

---

### `Build Artifacts` zip fails with path not found

**Symptom:** `Compress-Archive: The path 'target\allure-report\*' does not exist`

**Cause:** The Allure Jenkins plugin writes its report to `allure-report\` at the **workspace root**, not inside `target\`.

**Fix:** The zip path in `Jenkinsfile` must reference `allure-report\*` not `target\allure-report\*`:
```groovy
bat """powershell -Command "Compress-Archive -Path allure-report\\* -DestinationPath allure-report-${BUILD_NUMBER}.zip -Force" """
archiveArtifacts artifacts: "allure-report-${BUILD_NUMBER}.zip"
```

---

### Chrome not launching on Jenkins

**Cause:** Chrome binary not found on the Jenkins agent machine.

**Fix:** Install Chrome on the machine running Jenkins:
```cmd
rem Download and install Chrome from:
rem https://www.google.com/chrome
```

WebDriverManager handles ChromeDriver automatically — Chrome itself must be installed manually.

---

*For any issues not covered here, check the Jenkins Console Output for the full Maven log, or the `target/logs/automation.log` file which contains thread-level detail for every step of every scenario.*
