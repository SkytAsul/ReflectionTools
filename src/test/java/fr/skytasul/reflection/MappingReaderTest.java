package fr.skytasul.reflection;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;

class MappingReaderTest {

	@Test
	void testParseNativeParameters() {
		var mappings = new VersionedMappingsImplementation(0, 0, 0);
		mappings.classes = Collections.emptyList();
		var reader = new MappingParser(mappings);

		assertArrayEquals(new Type[] {int.class}, reader.parseParameters("int"));
		assertArrayEquals(new Type[] {int.class, boolean.class}, reader.parseParameters("int,boolean"));
	}

	@Test
	void testParseJavaParameters() {
		var mappings = new VersionedMappingsImplementation(0, 0, 0);
		mappings.classes = Collections.emptyList();
		var reader = new MappingParser(mappings);

		assertArrayEquals(new Type[] {String.class}, reader.parseParameters("java.lang.String"));
		assertArrayEquals(new Type[] {String.class, java.nio.file.Files.class},
				reader.parseParameters("java.lang.String,java.nio.file.Files"));
	}

	@Test
	void testParseJavaArrayParameters() {
		var mappings = new VersionedMappingsImplementation(0, 0, 0);
		mappings.classes = Collections.emptyList();
		var reader = new MappingParser(mappings);

		assertArrayEquals(new Type[] {String[].class, int[].class},
				reader.parseParameters("java.lang.String[],int[]"));
	}

	@Test
	void testParseCustomParameters() {
		var mappings = new VersionedMappingsImplementation(0, 0, 0);
		var dummyClassHandle = mappings.new ClassHandle("net.minecraft.ChatFormatting", "abc");
		mappings.classes = Arrays.asList(dummyClassHandle);
		var reader = new MappingParser(mappings);

		assertArrayEquals(new Type[] {dummyClassHandle},
				reader.parseParameters("net.minecraft.ChatFormatting"));
	}

	@Test
	void testParseCustomArrayParameters() {
		var mappings = new VersionedMappingsImplementation(0, 0, 0);
		var dummyClassHandle = mappings.new ClassHandle("net.minecraft.ChatFormatting", "abc");
		mappings.classes = Arrays.asList(dummyClassHandle);
		var reader = new MappingParser(mappings);

		assertArrayEquals(new Type[] {dummyClassHandle.getArrayType()},
				reader.parseParameters("net.minecraft.ChatFormatting[]"));
	}

}
