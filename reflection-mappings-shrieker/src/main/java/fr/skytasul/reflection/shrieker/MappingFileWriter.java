package fr.skytasul.reflection.shrieker;

import fr.skytasul.reflection.Version;
import fr.skytasul.reflection.mappings.Mappings;
import fr.skytasul.reflection.mappings.files.MappingFileReader;
import fr.skytasul.reflection.mappings.files.MappingType;
import org.jetbrains.annotations.NotNull;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

public class MappingFileWriter {

	private final @NotNull MappingType type;
	private final @NotNull Path path;
	private final @NotNull Writer writer;
	private final @NotNull Map<Version, Mappings> mappings;

	public MappingFileWriter(@NotNull MappingType type, @NotNull Path path, @NotNull Map<Version, Mappings> mappings) {
		this.type = type;
		this.path = path;
		this.writer = null;
		this.mappings = mappings;
	}

	public MappingFileWriter(@NotNull MappingType type, @NotNull Writer writer, @NotNull Map<Version, Mappings> mappings) {
		this.type = type;
		this.path = null;
		this.writer = writer;
		this.mappings = mappings;
	}

	public void writeAll() throws IOException {
		try (BufferedWriter writer =
				path != null ? Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
						: new BufferedWriter(this.writer)) {

			writeHeader(writer);
			for (var mapping : mappings.values())
				type.write(writer, mapping);
		}
	}

	private void writeHeader(@NotNull BufferedWriter writer) throws IOException {
		writer.append(MappingFileReader.VERSIONS_NOTICE);
		writer.newLine();

		int headerLength = 1 + mappings.size() + 1;
		int lastLine = headerLength - 1;
		for (var mapping : mappings.entrySet()) {
			int mappingLength =
					mapping.getValue().getClasses().stream()
							.mapToInt(c -> c.getFields().size() + c.getMethods().size() + 1)
							.sum();
			writer.append("# reflection-remapper | %s %d-%d".formatted(mapping.getKey(), lastLine + 1,
					lastLine + mappingLength));
			writer.newLine();
			lastLine += mappingLength;
		}

		writer.append(MappingFileReader.VERSIONS_NOTICE);
		writer.newLine();
	}

}
