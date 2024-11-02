package fr.skytasul.reflection.shrieker;

import static fr.skytasul.reflection.shrieker.TestUtils.parseMappings;
import static fr.skytasul.reflection.shrieker.TestUtils.writeMappings;
import static org.junit.jupiter.api.Assertions.assertEquals;
import fr.skytasul.reflection.Version;
import fr.skytasul.reflection.mappings.files.ProguardMapping;
import org.junit.jupiter.api.Test;

class PipeMappingsTest {

	private static final ProguardMapping MAPPING_TYPE = new ProguardMapping(true);

	@Test
	void testClass() {
		var mappingsA = parseMappings(MAPPING_TYPE, Version.ZERO, """
				some.class.A -> aaa:
				""");
		var mappingsB = parseMappings(MAPPING_TYPE, Version.ZERO, """
				aaa -> another.X:
				""");

		var pipe = new PipeMappings(mappingsA, mappingsB);
		assertEquals("""
				some.class.A -> another.X:
				""", writeMappings(MAPPING_TYPE, pipe));
	}

	@Test
	void testClassWithMembers() {
		var mappingsA = parseMappings(MAPPING_TYPE, Version.ZERO, """
				some.class.A -> aaa:
				    java.lang.String stringField -> a
				    67:85:void voidMethod(some.class.A) -> a
				""");
		var mappingsB = parseMappings(MAPPING_TYPE, Version.ZERO, """
				aaa -> another.X:
				    a -> mmhTasty
				    void a(aaa) -> alrightThen
				""");

		var pipe = new PipeMappings(mappingsA, mappingsB);
		assertEquals("""
				some.class.A -> another.X:
				    stringField -> mmhTasty
				    voidMethod(some.class.A) -> alrightThen
				""", writeMappings(MAPPING_TYPE, pipe));
	}

	@Test
	void testUnmappedThings() {
		var mappingsA = parseMappings(MAPPING_TYPE, Version.ZERO, """
				some.class.A -> aaa:
				    java.lang.String stringField -> a
				    67:85:void voidMethod(some.class.A) -> a
				a.class.OwO -> owo:
				    boolean hmmm -> okay
				""");
		var mappingsB = parseMappings(MAPPING_TYPE, Version.ZERO, """
				aaa -> another.X:
				    a -> mmhTasty
				    int unmappedMethod(aaa) -> k
				""");

		var pipe = new PipeMappings(mappingsA, mappingsB);
		assertEquals("""
				some.class.A -> another.X:
				    stringField -> mmhTasty
				    voidMethod(some.class.A) -> a
				    unmappedMethod(some.class.A) -> k
				a.class.OwO -> owo:
				    hmmm -> okay
				""", writeMappings(MAPPING_TYPE, pipe));
	}

}
