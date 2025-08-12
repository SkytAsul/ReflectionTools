package fr.skytasul.reflection.shrieker;

import static fr.skytasul.reflection.shrieker.TestUtils.getLines;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import fr.skytasul.reflection.Version;
import fr.skytasul.reflection.mappings.Mappings;
import fr.skytasul.reflection.mappings.files.ProguardMapping;
import org.junit.jupiter.api.Test;
import java.io.StringWriter;
import java.util.Map;

class MappingsShriekerTest {

	private static final ProguardMapping MAPPING_TYPE = new ProguardMapping(false);

	@Test
	void testKeepEverything() {
		var realMappings = parseMappings(Version.ZERO, """
				some.package.SomeClass -> abc:
				    java.lang.String stringField -> a
				    67:85:void voidMethod(int) -> a
				""");

		var shrieker = new MappingsShrieker(MAPPING_TYPE, (mappingsToFill, version) -> {
			assertDoesNotThrow(() -> {
				var clazz = mappingsToFill.getClass("some.package.SomeClass");
				clazz.getField("stringField").get(null);
				clazz.getMethod("voidMethod", int.class).invoke(null, 1);
			});
		});

		assertDoesNotThrow(() -> {
			shrieker.registerVersionMappings(Version.ZERO, realMappings);
		});

		assertEquals(1, shrieker.getReducedMappings().size());

		assertEquals("""
				# reflection-remapper | 0.0.0
				some.package.SomeClass -> abc:
				    stringField -> a
				    voidMethod(int) -> a
				""", writeMappings(shrieker.getReducedMappings()));
	}

	@Test
	void testPartial() {
		var realMappings = parseMappings(Version.ZERO, """
				some.package.SomeClass -> abc:
				    java.lang.String stringField -> b
				    67:85:void voidMethod(int) -> c
				some.other.package.SomeOtherClass -> abd:
				    java.lang.Comparable aField -> a
				    67:85:int intMethod() -> b
				""");

		var shrieker = new MappingsShrieker(MAPPING_TYPE, (mappingsToFill, version) -> {
			mappingsToFill.getClass("some.other.package.SomeOtherClass").getMethod("intMethod");
		});

		assertDoesNotThrow(() -> {
			shrieker.registerVersionMappings(Version.ZERO, realMappings);
		});

		assertEquals(1, shrieker.getReducedMappings().size());

		assertEquals("""
				# reflection-remapper | 0.0.0
				some.other.package.SomeOtherClass -> abd:
				    intMethod() -> b
				""", writeMappings(shrieker.getReducedMappings()));
	}

	static Mappings parseMappings(Version version, String lines) {
		return MAPPING_TYPE.parse(getLines(lines));
	}

	static String writeMappings(Map<Version, Mappings> mappings) {
		var writer = new StringWriter();
		assertDoesNotThrow(new MappingFileWriter(MAPPING_TYPE, writer, mappings)::writeAll);
		return writer.toString();
	}

}
