package fr.skytasul.reflection;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

class MappingFileWriterTest {

	@Test
	void testWriteSingle() {
		var writer = new StringWriter();
		var mapping = new VersionedMappingsObfuscated(new Version(1, 2, 3));

		var stringHandle = new VersionedMappingsObfuscated.ClassHandle("java.lang.String", "a");
		stringHandle.fields = List.of();
		stringHandle.methods = List.of(stringHandle.new MethodHandle("split", "b", new Type[] {stringHandle}));
		mapping.classes = List.of(stringHandle);

		assertDoesNotThrow(new MappingFileWriter(writer, List.of(mapping))::writeAll);

		assertEquals("""
				# reflection-remapper | AVAILABLE VERSIONS
				# reflection-remapper | 1.2.3 3-4
				# reflection-remapper | AVAILABLE VERSIONS
				java.lang.String -> a
				    split(java.lang.String) -> b
				""", writer.toString());
	}

	@Test
	void testWriteMultiple() {
		var writer = new StringWriter();

		var mappings = new ArrayList<VersionedMappings>();
		for (int i = 0; i < 2; i++) {
			var mapping = new VersionedMappingsObfuscated(new Version(1, 2, i));

			var stringHandle =
					new VersionedMappingsObfuscated.ClassHandle("java.lang.String", Character.toString('a' + i * 2));
			stringHandle.fields = List.of();
			stringHandle.methods = List
					.of(stringHandle.new MethodHandle("split", Character.toString('b' + i * 2), new Type[] {stringHandle}));
			mapping.classes = List.of(stringHandle);

			mappings.add(mapping);
		}

		assertDoesNotThrow(new MappingFileWriter(writer, mappings)::writeAll);

		assertEquals("""
				# reflection-remapper | AVAILABLE VERSIONS
				# reflection-remapper | 1.2.0 4-5
				# reflection-remapper | 1.2.1 6-7
				# reflection-remapper | AVAILABLE VERSIONS
				java.lang.String -> a
				    split(java.lang.String) -> b
				java.lang.String -> c
				    split(java.lang.String) -> d
				""", writer.toString());
	}

}
