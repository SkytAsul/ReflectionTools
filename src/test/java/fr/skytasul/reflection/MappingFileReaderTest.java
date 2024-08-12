package fr.skytasul.reflection;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.List;
import java.util.stream.Stream;

class MappingFileReaderTest {

	@ParameterizedTest
	@CsvSource(value = {
			"1.18.2,",
			"1.19.1,1.19.1",
			"1.19.4,1.19.1",
			"1.19.6,1.19.5",
			"1.20,1.19.5",
			"1.20.2,1.20.2",
			"1.20.3,1.20.2",
			"1.20.4,1.20.4",
			"1.20.5,1.20.4",
			"1.20.7,1.20.6",
			"1.30,1.20.6",
	})
	void testBestMatchedVersion(String target, String expectedResult) {
		var targetVersion = Version.parse(target);
		var expectedVersion = expectedResult == null ? null : Version.parse(expectedResult);
		var match = MappingFileReader.getBestMatchedVersion(targetVersion,
				List.of(getVersionArray("1.19.1", "1.19.5", "1.20.2", "1.20.4", "1.20.6")));

		assertEquals(expectedVersion, match.orElse(null));
	}

	@Test
	void testCorrectHeader() {
		assertDoesNotThrow(() -> {
			var reader = new MappingFileReader(getLines("""
				# ignored line
				# reflection-remapper | AVAILABLE VERSIONS
				# reflection-remapper | 1.0.0 10-20
				# reflection-remapper | 1.1.0 20-30
				# reflection-remapper | 1.2.0 30-40
				# reflection-remapper | AVAILABLE VERSIONS
					"""));
			var versions = reader.readAvailableVersions();

			assertArrayEquals(getVersionArray("1.0.0", "1.1.0", "1.2.0"), versions.toArray());
		});
	}

	@Test
	void testIncorrectHeader() {
		assertThrows(IllegalArgumentException.class, () -> {
			new MappingFileReader(getLines("""
					# nothing here
					""")).readAvailableVersions();
		});
		assertThrows(IllegalArgumentException.class, () -> {
			new MappingFileReader(getLines("""
					# reflection-remapper | AVAILABLE VERSIONS
					# reflection-remapper | 1.0.0 10-20
					# no end to the versions
					""")).readAvailableVersions();
		});
	}

	@Test
	void testKeepVersion() {
		assertDoesNotThrow(() -> {
			var reader = new MappingFileReader(getLines("""
					# ignored line
					# reflection-remapper | AVAILABLE VERSIONS
					# reflection-remapper | 1.0.0 10-20
					# reflection-remapper | 1.1.0 20-30
					# reflection-remapper | 1.2.0 30-40
					# reflection-remapper | AVAILABLE VERSIONS
						"""));
			reader.readAvailableVersions();

			assertFalse(reader.keepOnlyVersion(new Version(1, 3, 0)));
			assertTrue(reader.keepOnlyVersion(new Version(1, 1, 0)));

			assertArrayEquals(getVersionArray("1.1.0"), reader.getAvailableVersions().toArray());
		});
	}

	@Test
	void testKeepBestVersion() {
		assertDoesNotThrow(() -> {
			var reader = new MappingFileReader(getLines("""
					# ignored line
					# reflection-remapper | AVAILABLE VERSIONS
					# reflection-remapper | 1.0.0 10-20
					# reflection-remapper | 1.1.0 20-30
					# reflection-remapper | 1.2.0 30-40
					# reflection-remapper | AVAILABLE VERSIONS
						"""));
			reader.readAvailableVersions();

			var versionMatch = reader.keepBestMatchedVersion(new Version(1, 1, 2));
			assertEquals(new Version(1, 1, 0), versionMatch.orElseThrow());

			assertArrayEquals(getVersionArray("1.1.0"), reader.getAvailableVersions().toArray());
		});
	}

	@Test
	void testParseMappings() {
		assertDoesNotThrow(() -> {
			var reader = new MappingFileReader(getLines("""
					# reflection-remapper | AVAILABLE VERSIONS
					# reflection-remapper | 1.0.0 4-5
					# reflection-remapper | 1.1.0 6-7
					# reflection-remapper | AVAILABLE VERSIONS
					net.minecraft.world.entity.Interaction -> abc:
					    java.lang.String stringField -> a
					net.minecraft.world.entity.Interaction -> abd:
					    java.lang.String stringField -> b
						"""));
			reader.readAvailableVersions();
			reader.parseMappings();

			assertNotNull(reader.getParsedMappings(new Version(1, 0, 0)));
			assertNotNull(reader.getParsedMappings(new Version(1, 1, 0)));
		});

	}

	static Version[] getVersionArray(String... versions) {
		return Stream.of(versions).map(Version::parse).toArray(Version[]::new);
	}

	static List<String> getLines(String string) {
		return List.of(string.split("\\n"));
	}

}
