package fr.skytasul.reflection.mappings.files;

import static fr.skytasul.reflection.TestUtils.getLines;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import fr.skytasul.reflection.mappings.RealMappings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

class ProguardMappingTest {

	private ProguardMapping reader;

	@BeforeEach
	void setUp() {
		reader = new ProguardMapping(true);
	}

	@Test
	void testParseEmptyClass() {
		var mappings = reader.parse(getLines("""
				# comment that should be ignored
				net.minecraft.world.entity.Interaction -> abc:
				"""));
		assertEquals(1, mappings.getClasses().size());

		var parsedClass = mappings.getClasses().iterator().next();
		assertTrue(parsedClass.getMethods().isEmpty());
		assertTrue(parsedClass.getFields().isEmpty());
		assertEquals("net.minecraft.world.entity.Interaction", parsedClass.getOriginalName());
		assertEquals("abc", parsedClass.getMappedName());
	}

	@Test
	void testParseFullClass() {
		var mappings = reader.parse(getLines("""
				net.minecraft.world.entity.Interaction -> abc:
				    java.lang.String stringField -> a
				    67:85:void voidMethod(int) -> a
				"""));
		assertEquals(1, mappings.getClasses().size());

		var parsedClass = mappings.getClasses().iterator().next();
		assertEquals(1, parsedClass.getMethods().size());
		assertEquals(1, parsedClass.getFields().size());
		assertDoesNotThrow(() -> {
			var parsedField = parsedClass.getFields().iterator().next();
			assertEquals("stringField", parsedField.getOriginalName());
			assertEquals("a", parsedField.getMappedName());
			var parsedMethod = parsedClass.getMethods().iterator().next();
			assertEquals("voidMethod", parsedMethod.getOriginalName());
			assertEquals("a", parsedMethod.getMappedName());
			assertArrayEquals(new Type[] {int.class}, parsedMethod.getParameterTypes());
		});
		assertEquals("net.minecraft.world.entity.Interaction", parsedClass.getOriginalName());
		assertEquals("abc", parsedClass.getMappedName());
	}

	@Test
	void testParseTwoClasses() {
		var mappings = reader.parse(getLines("""
				net.minecraft.world.entity.Interaction -> abc:
				    java.lang.String stringField -> a
				net.minecraft.world.entity.Marker -> xyz:
				    10:11:boolean boolMethod() -> b
				"""));
		assertEquals(2, mappings.getClasses().size());

		for (var parsedClass : mappings.getClasses()) {
			boolean first = parsedClass.getOriginalName().equals("net.minecraft.world.entity.Interaction");
			assertEquals(first ? 0 : 1, parsedClass.getMethods().size());
			assertEquals(first ? 1 : 0, parsedClass.getFields().size());
		}
	}

	@Test
	void testParseMinecraftMappings() {
		assertDoesNotThrow(() -> {
			try (var inputReader =
					new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/mc_server_1.21.txt")))) {
				var lines = inputReader.lines().toList();
				reader.parse(lines);
			}
		});
	}

	@Test
	void testParseEmptyParameters() {
		assertArrayEquals(new Type[] {}, reader.parseParameters("", Map.of(), Map.of()));
	}

	@Test
	void testParsePrimitiveParameters() {
		assertArrayEquals(new Type[] {int.class}, reader.parseParameters("int", Map.of(), Map.of()));
		assertArrayEquals(new Type[] {int.class, boolean.class}, reader.parseParameters("int,boolean", Map.of(), Map.of()));
	}

	@Test
	void testParseJavaParameters() {
		assertArrayEquals(new Type[] {String.class}, reader.parseParameters("java.lang.String", Map.of(), Map.of()));
		assertArrayEquals(new Type[] {String.class, java.nio.file.Files.class},
				reader.parseParameters("java.lang.String,java.nio.file.Files", Map.of(), Map.of()));
	}

	@Test
	void testParseJavaArrayParameters() {
		assertArrayEquals(new Type[] {String[].class, int[].class},
				reader.parseParameters("java.lang.String[],int[]", Map.of(), Map.of()));
	}

	@Test
	void testParseCustomParameters() {
		var dummyClassHandle =
				new RealMappings.RealClassMapping("net.minecraft.ChatFormatting", "abc", List.of(), List.of());

		assertArrayEquals(new Type[] {dummyClassHandle}, reader.parseParameters("net.minecraft.ChatFormatting", Map.of(),
						Map.of(dummyClassHandle.getOriginalName(), dummyClassHandle)));
	}

	@Test
	void testParseCustomArrayParameters() {
		var dummyClassHandle =
				new RealMappings.RealClassMapping("net.minecraft.ChatFormatting", "abc", List.of(), List.of());

		assertArrayEquals(new Type[] {dummyClassHandle.getArrayType()},
				reader.parseParameters("net.minecraft.ChatFormatting[]", Map.of(),
						Map.of(dummyClassHandle.getOriginalName(), dummyClassHandle)));
	}

}
