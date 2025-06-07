package com.test.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.syos.config.ConfigLoader;

import static org.junit.jupiter.api.Assertions.*;

class ConfigLoaderTest {

    // Since ConfigLoader uses a static initializer block, properties are loaded
    // when the class is first accessed. For tests, we rely on the
    // application.properties file placed in src/test/resources.

    @Test
    @DisplayName("Should retrieve a string property successfully")
    void testGetStringProperty() {
        String value = ConfigLoader.get("test.property.string");
        assertNotNull(value, "String property should not be null");
        assertEquals("Hello World", value, "String property value mismatch");
    }

    @Test
    @DisplayName("Should retrieve a number property successfully")
    void testGetNumberProperty() {
        String value = ConfigLoader.get("test.property.number");
        assertNotNull(value, "Number property should not be null");
        assertEquals("12345", value, "Number property value mismatch");
    }

    @Test
    @DisplayName("Should retrieve a boolean property successfully")
    void testGetBooleanProperty() {
        String value = ConfigLoader.get("test.property.boolean");
        assertNotNull(value, "Boolean property should not be null");
        assertEquals("true", value, "Boolean property value mismatch");
    }

    @Test
    @DisplayName("Should return null for a non-existent property")
    void testGetNonExistentProperty() {
        String value = ConfigLoader.get("non.existent.property");
        assertNull(value, "Non-existent property should return null");
    }

    // Testing the RuntimeException for missing application.properties is tricky
    // because the static block executes only once. To properly test this,
    // you would typically need to run tests in separate JVMs or use a custom
    // classloader, which is usually overkill for a simple config loader.
    // For this scenario, we primarily focus on successful loading and retrieval.
    // The current implementation throws a RuntimeException, which is a design
    // choice that implies the application cannot proceed without the config file.
    // If it were a non-static class or had an exposed load method, it would be
    // easier to test failure scenarios.
}