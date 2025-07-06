package com.test.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.syos.config.ConfigLoader;

import static org.junit.jupiter.api.Assertions.*;

class ConfigLoaderTest {

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

}