package fr.skytasul.reflection.shrieker;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import fr.skytasul.reflection.Version;
import fr.skytasul.reflection.mappings.Mappings;
import fr.skytasul.reflection.mappings.RealMappings;
import fr.skytasul.reflection.mappings.files.ProguardMapping;
import fr.skytasul.reflection.shrieker.MappingFileWriter;
import org.junit.jupiter.api.Test;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class MappingFileWriterTest {

	private final ProguardMapping mappingType = new ProguardMapping(false);

	@Test
	void testWriteSingle() {
		var stringHandle = new RealMappings.RealClassMapping("java.lang.String", "a", List.of(),
				List.of(new RealMappings.RealClassMapping.RealMethodMapping("charAt", "b", new Type[] {int.class})));
		var mappings = new RealMappings(List.of(stringHandle));

		var writer = new StringWriter();
		assertDoesNotThrow(new MappingFileWriter(mappingType, writer, Map.of(Version.parse("1.2.3"), mappings))::writeAll);

		assertEquals("""
				# reflection-remapper | AVAILABLE VERSIONS
				# reflection-remapper | 1.2.3 3-4
				# reflection-remapper | AVAILABLE VERSIONS
				java.lang.String -> a:
				    charAt(int) -> b
				""", writer.toString());
	}

	@Test
	void testWriteMultiple() {
		var writer = new StringWriter();

		var allMappings = new HashMap<Version, Mappings>();
		for (int i = 0; i < 2; i++) {
			var stringHandle = new RealMappings.RealClassMapping("java.lang.String", Character.toString('a' + i * 2),
					List.of(), List.of(new RealMappings.RealClassMapping.RealMethodMapping("charAt",
							Character.toString('b' + i * 2), new Type[] {int.class})));
			var mappings = new RealMappings(List.of(stringHandle));

			allMappings.put(new Version(1, 2, i), mappings);
		}

		assertDoesNotThrow(new MappingFileWriter(mappingType, writer, allMappings)::writeAll);

		assertEquals("""
				# reflection-remapper | AVAILABLE VERSIONS
				# reflection-remapper | 1.2.1 4-5
				# reflection-remapper | 1.2.0 6-7
				# reflection-remapper | AVAILABLE VERSIONS
				java.lang.String -> c:
				    charAt(int) -> d
				java.lang.String -> a:
				    charAt(int) -> b
				""", writer.toString());
	}

}
