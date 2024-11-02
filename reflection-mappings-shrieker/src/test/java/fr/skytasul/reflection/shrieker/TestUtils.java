package fr.skytasul.reflection.shrieker;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import fr.skytasul.reflection.Version;
import fr.skytasul.reflection.mappings.Mappings;
import fr.skytasul.reflection.mappings.files.MappingType;
import org.jetbrains.annotations.NotNull;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class TestUtils {

	public static List<String> getLines(String string) {
		return List.of(string.split("\\n"));
	}

	public static List<String> readLines(@NotNull URL resourceUrl) throws IOException, URISyntaxException {
		return Files.readAllLines(Path.of(resourceUrl.toURI()));
	}

	public static Mappings parseMappings(MappingType type, Version version, String lines) {
		return type.parse(getLines(lines));
	}

	public static String writeMappings(MappingType type, Mappings mappings) {
		return assertDoesNotThrow(() -> {
			var writer = new StringWriter();
			try (var bufferedWriter = new BufferedWriter(writer)) {
				type.write(bufferedWriter, mappings);
			}
			return writer.toString();
		});
	}

}
