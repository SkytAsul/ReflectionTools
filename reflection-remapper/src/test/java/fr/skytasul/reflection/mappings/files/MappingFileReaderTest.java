package fr.skytasul.reflection.mappings.files;

import static fr.skytasul.reflection.TestUtils.getLines;
import static fr.skytasul.reflection.Version.parseArray;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import fr.skytasul.reflection.Version;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.List;

class MappingFileReaderTest {

	private final ProguardMapping mappingType = new ProguardMapping(false);

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
				List.of(parseArray("1.19.1", "1.19.5", "1.20.2", "1.20.4", "1.20.6")));

		assertEquals(expectedVersion, match.orElse(null));
	}

	@Test
	void testNoVersions() {
		assertDoesNotThrow(() -> {
			var reader = new MappingFileReader(mappingType, getLines(""));
			assertArrayEquals(new Version[0], reader.getAvailableVersions().toArray());

			reader = new MappingFileReader(mappingType, getLines("""
					# ignored line
					"""));
			assertArrayEquals(new Version[0], reader.getAvailableVersions().toArray());
		});
	}

	@Test
	void testVersionWithoutMappings() {
		assertDoesNotThrow(() -> {
			var reader = new MappingFileReader(mappingType, getLines("""
					# reflection-remapper | 1.0.0
					# reflection-remapper | 1.1.0
					net.minecraft.world.entity.Interaction -> abc:
					"""));
			assertArrayEquals(parseArray("1.0.0", "1.1.0"), reader.getAvailableVersions().toArray());
		});
	}

	@Test
	void testIncorrectVersionHeaders() {
		assertThrows(IllegalArgumentException.class, () -> {
			new MappingFileReader(mappingType, getLines("""
					# no version information
					net.minecraft.world.entity.Interaction -> abc:
					""")).getAvailableVersions();
		});
		assertThrows(IllegalArgumentException.class, () -> {
			new MappingFileReader(mappingType, getLines("""
					# reflection-remapper | a.b.c
					net.minecraft.world.entity.Interaction -> abc:
					""")).getAvailableVersions();
		});
	}

	@Test
	void testKeepVersion() {
		assertDoesNotThrow(() -> {
			var reader = new MappingFileReader(mappingType, getLines("""
					# ignored line
					# reflection-remapper | 1.0.0
					net.minecraft.world.entity.Interaction -> abc:
					# reflection-remapper | 1.1.0
					net.minecraft.world.entity.Interaction -> abc:
					# reflection-remapper | 1.2.0
					net.minecraft.world.entity.Interaction -> abc:
						"""));
			assertFalse(reader.keepOnlyVersion(new Version(1, 3, 0)));
			assertTrue(reader.keepOnlyVersion(new Version(1, 1, 0)));

			assertArrayEquals(parseArray("1.1.0"), reader.getAvailableVersions().toArray());
		});
	}

	@Test
	void testKeepBestVersion() {
		assertDoesNotThrow(() -> {
			var reader = new MappingFileReader(mappingType, getLines("""
					# ignored line
					# reflection-remapper | 1.0.0
					# reflection-remapper | 1.1.0
					# reflection-remapper | 1.2.0
						"""));
			var versionMatch = reader.keepBestMatchedVersion(new Version(1, 1, 2));
			assertEquals(new Version(1, 1, 0), versionMatch.orElseThrow());

			assertArrayEquals(parseArray("1.1.0"), reader.getAvailableVersions().toArray());
		});
	}

	@Test
	void testParseMappings() {
		assertDoesNotThrow(() -> {
			var reader = new MappingFileReader(mappingType, getLines("""
					# reflection-remapper | 1.0.0
					net.minecraft.world.entity.Interaction -> abc:
					    java.lang.String stringField -> a
					# reflection-remapper | 1.1.0
					net.minecraft.world.entity.Interaction -> abd:
					    java.lang.String stringField -> b
						"""));
			reader.parseMappings();

			assertNotNull(reader.getParsedMappings(new Version(1, 0, 0)));
			assertNotNull(reader.getParsedMappings(new Version(1, 1, 0)));
		});

	}

}
