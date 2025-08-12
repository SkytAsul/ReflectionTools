package fr.skytasul.reflection.shrieker;

import fr.skytasul.reflection.Version;
import fr.skytasul.reflection.mappings.Mappings;
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

			var versions = mappings.keySet().stream().sorted().toList();
			for (var version : versions) {
				writer.append("# reflection-remapper | %s".formatted(version.toString()));
				writer.newLine();
				type.write(writer, mappings.get(version));
			}
		}
	}

}
