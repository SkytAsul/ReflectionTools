package fr.skytasul.reflection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class VersionTest {

	@Test
	void testFormat() {
		assertEquals("1.2.3", new Version(1, 2, 3).toString());
		assertEquals("1.2.3", new Version(1, 2, 3).toString(true));
		assertEquals("1.2.0", new Version(1, 2, 0).toString());
		assertEquals("1.2.0", new Version(1, 2, 0).toString(false));
		assertEquals("1.2", new Version(1, 2, 0).toString(true));
	}

	@ParameterizedTest
	@ValueSource(strings = {"1.0.0", "1.2.3", "0.0.1", "0.1.0", "4.3.2", "4.3.0"})
	void testParseNoOmit(String version) {
		var parsedVersion = Version.parse(version);
		assertEquals(version, parsedVersion.toString(false));
	}

	@ParameterizedTest
	@ValueSource(strings = {"1.0", "1.2.3", "0.0.1", "0.1", "4.3.2", "4.3"})
	void testParseWithOmit(String version) {
		var parsedVersion = Version.parse(version);
		assertEquals(version, parsedVersion.toString(true));
	}

	@ParameterizedTest
	@CsvSource(value = {
			"2.5.1,2.5.3", // simple patch change
			"2.5.2,2.6.1", // minor change, lower patch
			"2.5.2,2.6.3", // minor change, higher patch
			"2.5.2,2.6.2", // minor change, same patch
			"2.5.2,3.0.0", // major change
	})
	void testComparisons(String low, String high) {
		var lowest = Version.parse(low);
		var highest = Version.parse(high);

		assertTrue(lowest.isBefore(highest));
		assertFalse(highest.isBefore(lowest));
		assertFalse(lowest.isAfter(highest));
		assertTrue(highest.isAfter(lowest));
		assertFalse(highest.is(lowest));
		assertFalse(lowest.is(highest));
	}

	@ParameterizedTest
	@ValueSource(strings = {"1.0.0", "1.2.3", "0.0.1", "0.1", "4.3.2", "4.3"})
	void testEquality(String version) {
		var parsedVersion1 = Version.parse(version);
		var parsedVersion2 = Version.parse(version);

		assertTrue(parsedVersion1.is(parsedVersion2));
		assertTrue(parsedVersion2.is(parsedVersion1));
		assertTrue(parsedVersion1.isAfter(parsedVersion2)); // highest or equal
		assertTrue(parsedVersion2.isAfter(parsedVersion1));
		assertTrue(parsedVersion1.equals(parsedVersion2));
		assertTrue(parsedVersion2.equals(parsedVersion1));
	}

}
