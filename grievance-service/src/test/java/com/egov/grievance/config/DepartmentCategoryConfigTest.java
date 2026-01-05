package com.egov.grievance.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DepartmentCategoryConfigTest {

	@Test
	void loadConfig_success() {
		DepartmentCategoryConfig config = new DepartmentCategoryConfig();
		config.load();
		assertNotNull(config.getDepartments());
		assertTrue(config.isValidDepartment("D001"));
		assertTrue(config.isValid("D001", "C101"));
	}

	@Test
	void invalidDepartment() {
		DepartmentCategoryConfig config = new DepartmentCategoryConfig();
		config.load();
		assertFalse(config.isValidDepartment("INVALID"));
		assertFalse(config.isValid("INVALID", "C101"));
	}

	@Test
	void getSlaHours_success() {
		DepartmentCategoryConfig config = new DepartmentCategoryConfig();
		config.load();
		assertEquals(48, config.getSlaHours("D001", "C101"));
	}

	@Test
	void getSlaHours_invalidCategory() {
		DepartmentCategoryConfig config = new DepartmentCategoryConfig();
		config.load();
		assertThrows(IllegalArgumentException.class, () -> config.getSlaHours("D001", "BAD"));
	}

	@Test
	void getCategories_success() {
		DepartmentCategoryConfig config = new DepartmentCategoryConfig();
		config.load();
		var categories = config.getCategories("D001");
		assertNotNull(categories);
		assertTrue(categories.containsKey("C101"));
	}

	@Test
	void getCategories_invalidDepartment_returnsEmpty() {
		DepartmentCategoryConfig config = new DepartmentCategoryConfig();
		config.load();
		var categories = config.getCategories("INVALID");
		assertNotNull(categories);
		assertTrue(categories.isEmpty());
	}

	@Test
	void loadConfig_failure_throwsRuntimeException() {
		DepartmentCategoryConfig config = new DepartmentCategoryConfig() {
			@Override
			public void load() {
				throw new RuntimeException("Failed to load department-category config");
			}
		};
		assertThrows(RuntimeException.class, config::load);
	}

	@Test
	void getSlaHours_invalidDepartment() {
		DepartmentCategoryConfig config = new DepartmentCategoryConfig();
		config.load();
		assertThrows(IllegalArgumentException.class, () -> config.getSlaHours("INVALID", "C101"));
	}

	@Test
	void isValid_whenDepartmentsNull_returnsFalse() {
		DepartmentCategoryConfig config = new DepartmentCategoryConfig();
		assertFalse(config.isValid("D001", "C101"));
	}

	@Test
	void isValidDepartment_whenDepartmentsNull_returnsFalse() {
		DepartmentCategoryConfig config = new DepartmentCategoryConfig();
		assertFalse(config.isValidDepartment("D001"));
	}

}
