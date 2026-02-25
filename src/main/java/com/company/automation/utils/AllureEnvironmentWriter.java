package com.company.automation.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

public final class AllureEnvironmentWriter {

    private AllureEnvironmentWriter() {}

    public static void writeEnvironmentInfo() {

        try {
            File resultsDir = new File("target/allure-results");
            if (!resultsDir.exists()) {
                resultsDir.mkdirs();
            }

            Properties props = new Properties();
            props.setProperty("Browser", ConfigReader.get("browser"));
            props.setProperty("Headless", ConfigReader.get("headless"));
            props.setProperty("Base URL", ConfigReader.get("base.url"));
            props.setProperty("OS", System.getProperty("os.name"));
            props.setProperty("Java Version", System.getProperty("java.version"));
            props.setProperty("User", System.getProperty("user.name"));

            File file = new File(resultsDir, "environment.properties");

            try (FileWriter writer = new FileWriter(file)) {
                props.store(writer, "Allure Environment");
            }
            writeExecutorInfo(resultsDir);

        } catch (IOException e) {
            throw new RuntimeException("Failed to write Allure environment info", e);
        }
    }
    private static void writeExecutorInfo(File resultsDir) throws IOException {

        File executorFile = new File(resultsDir, "executor.json");

        String json = """
            {
              "name": "Local Maven Execution",
              "type": "maven",
              "buildName": "UI Automation Framework",
              "buildUrl": "http://localhost",
              "reportUrl": "http://localhost",
              "buildOrder": 1
            }
            """;

        try (FileWriter writer = new FileWriter(executorFile)) {
            writer.write(json);
        }
    }
}