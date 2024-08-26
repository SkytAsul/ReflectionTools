package fr.skytasul.reflection.shrieker;

import static fr.skytasul.reflection.TestUtils.getLines;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import fr.skytasul.reflection.*;
import org.junit.jupiter.api.Test;
import java.io.StringWriter;
import java.util.List;

class MappingsShriekerTest {

	@Test
	void testKeepEverything() {
		var realMappings = parseMappings(Version.ZERO, """
				some.package.SomeClass -> abc:
				    java.lang.String stringField -> a
				    67:85:void voidMethod(int) -> a
				""");

		var shrieker = new MappingsShrieker(mappingsToFill -> {
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

		var reducedMappings = shrieker.getReducedMappings().get(0);
		assertEquals("""
				# reflection-remapper | AVAILABLE VERSIONS
				# reflection-remapper | 0.0.0 3-5
				# reflection-remapper | AVAILABLE VERSIONS
				some.package.SomeClass -> abc:
				    stringField -> a
				    voidMethod(int) -> a
				""", writeMappings(reducedMappings));
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

		var shrieker = new MappingsShrieker(mappingsToFill -> {
			mappingsToFill.getClass("some.other.package.SomeOtherClass").getMethod("intMethod");
		});

		assertDoesNotThrow(() -> {
			shrieker.registerVersionMappings(Version.ZERO, realMappings);
		});

		assertEquals(1, shrieker.getReducedMappings().size());

		var reducedMappings = shrieker.getReducedMappings().get(0);
		assertEquals("""
				# reflection-remapper | AVAILABLE VERSIONS
				# reflection-remapper | 0.0.0 3-4
				# reflection-remapper | AVAILABLE VERSIONS
				some.other.package.SomeOtherClass -> abd:
				    intMethod() -> b
				""", writeMappings(reducedMappings));
	}

	static VersionedMappings parseMappings(Version version, String lines) {
		var mappings = new VersionedMappingsObfuscated(version);
		new MappingParser(mappings).parseAndFill(getLines(lines));
		return mappings;
	}

	static String writeMappings(VersionedMappings... mappings) {
		var writer = new StringWriter();
		assertDoesNotThrow(new MappingFileWriter(writer, List.of(mappings))::writeAll);
		return writer.toString();
	}

}
