package com.company.automation.constants;

public final class FrameworkConstants {

    private FrameworkConstants() {}

    public static final String CONFIG_FILE_PATH =
            System.getProperty("user.dir") + "/src/test/resources/config/config.properties";

    public static final String CHROME = "chrome";
    public static final String FIREFOX = "firefox";
    public static final String EDGE = "edge";

}