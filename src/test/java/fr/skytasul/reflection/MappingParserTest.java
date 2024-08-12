package fr.skytasul.reflection;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;

class MappingParserTest {

	private VersionedMappingsObfuscated mappings;
	private MappingParser reader;

	@BeforeEach
	void setUp() {
		mappings = new VersionedMappingsObfuscated(Version.ZERO);
		mappings.classes = new ArrayList<>();
		reader = new MappingParser(mappings);
	}

	@Test
	void testParseEmptyClass() {
		var lines = Arrays.asList("""
				# comment that should be ignored
				net.minecraft.world.entity.Interaction -> abc:
				""".split("\\n"));
		reader.parseAndFill(lines);
		assertEquals(1, mappings.classes.size());

		var parsedClass = mappings.classes.get(0);
		assertTrue(parsedClass.getMethods().isEmpty());
		assertTrue(parsedClass.getFields().isEmpty());
		assertEquals("net.minecraft.world.entity.Interaction", parsedClass.getOriginalName());
		assertEquals("abc", parsedClass.getObfuscatedName());
	}

	@Test
	void testParseFullClass() {
		var lines = Arrays.asList("""
				net.minecraft.world.entity.Interaction -> abc:
				    java.lang.String stringField -> a
				    67:85:void voidMethod(int) -> a
				""".split("\\n"));
		reader.parseAndFill(lines);
		assertEquals(1, mappings.classes.size());

		var parsedClass = mappings.classes.get(0);
		assertEquals(1, parsedClass.getMethods().size());
		assertEquals(1, parsedClass.getFields().size());
		assertDoesNotThrow(() -> {
			var parsedField = parsedClass.getField("stringField");
			assertEquals("stringField", parsedField.getOriginalName());
			assertEquals("a", parsedField.getObfuscatedName());
			var parsedMethod = parsedClass.getMethod("voidMethod", int.class);
			assertEquals("voidMethod", parsedMethod.getOriginalName());
			assertEquals("a", parsedMethod.getObfuscatedName());
			assertArrayEquals(new Type[] {int.class}, parsedMethod.getParameterTypes());
		});
		assertEquals("net.minecraft.world.entity.Interaction", parsedClass.getOriginalName());
		assertEquals("abc", parsedClass.getObfuscatedName());
	}

	@Test
	void testParseTwoClasses() {
		var lines = Arrays.asList("""
				net.minecraft.world.entity.Interaction -> abc:
				    java.lang.String stringField -> a
				net.minecraft.world.entity.Marker -> xyz:
				    10:11:boolean boolMethod() -> b
				""".split("\\n"));
		reader.parseAndFill(lines);
		assertEquals(2, mappings.classes.size());

		var parsedClass = mappings.classes.get(0);
		assertEquals(0, parsedClass.getMethods().size());
		assertEquals(1, parsedClass.getFields().size());
		parsedClass = mappings.classes.get(1);
		assertEquals(1, parsedClass.getMethods().size());
		assertEquals(0, parsedClass.getFields().size());
	}

	@Test
	void testParseEmptyParameters() {
		assertArrayEquals(new Type[] {}, reader.parseParameters(""));
	}

	@Test
	void testParsePrimitiveParameters() {
		assertArrayEquals(new Type[] {int.class}, reader.parseParameters("int"));
		assertArrayEquals(new Type[] {int.class, boolean.class}, reader.parseParameters("int,boolean"));
	}

	@Test
	void testParseJavaParameters() {
		assertArrayEquals(new Type[] {String.class}, reader.parseParameters("java.lang.String"));
		assertArrayEquals(new Type[] {String.class, java.nio.file.Files.class},
				reader.parseParameters("java.lang.String,java.nio.file.Files"));
	}

	@Test
	void testParseJavaArrayParameters() {
		assertArrayEquals(new Type[] {String[].class, int[].class},
				reader.parseParameters("java.lang.String[],int[]"));
	}

	@Test
	void testParseCustomParameters() {
		var dummyClassHandle = new VersionedMappingsObfuscated.ClassHandle("net.minecraft.ChatFormatting", "abc");
		mappings.classes.add(dummyClassHandle);

		assertArrayEquals(new Type[] {dummyClassHandle},
				reader.parseParameters("net.minecraft.ChatFormatting"));
	}

	@Test
	void testParseCustomArrayParameters() {
		var dummyClassHandle = new VersionedMappingsObfuscated.ClassHandle("net.minecraft.ChatFormatting", "abc");
		mappings.classes.add(dummyClassHandle);

		assertArrayEquals(new Type[] {dummyClassHandle.getArrayType()},
				reader.parseParameters("net.minecraft.ChatFormatting[]"));
	}

}
