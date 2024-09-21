package com.example.demo.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesLoader {

    private static final EventLogger eventLogger = new EventLogger(PropertiesLoader.class);
    private static final Properties properties = new Properties();

    // Static block to load properties once when the class is first loaded
    static {
        // Load properties from the classpath
        try (InputStream input = PropertiesLoader.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                throw new IOException("Unable to find application.properties in the classpath");
            }
            properties.load(input);
        } catch (IOException ex) {
            eventLogger.logException(ex);  // Log the exception
        }
    }

    // Static method to get a property by key
    public static String getProperty(String key) {
        return properties.getProperty(key);
    }
}
