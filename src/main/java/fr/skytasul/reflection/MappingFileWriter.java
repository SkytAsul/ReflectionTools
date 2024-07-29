package fr.skytasul.reflection;

import org.jetbrains.annotations.NotNull;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MappingFileWriter {

	private final @NotNull Path path;
	private final @NotNull List<VersionedMappings> mappings;

	public MappingFileWriter(@NotNull Path path, @NotNull List<VersionedMappings> mappings) {
		this.path = path;
		this.mappings = mappings;
	}

	public void writeAll() throws IOException {
		try (BufferedWriter writer =
				Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

			writeHeader(writer);
			for (var mapping : mappings)
				writeSingleMapping(writer, mapping);
		}
	}

	private void writeHeader(@NotNull BufferedWriter writer) throws IOException {
		writer.append(MappingFileReader.VERSIONS_NOTICE);
		writer.newLine();

		int headerLength = 1 + mappings.size() + 1;
		int lastLine = headerLength - 1;
		for (var mapping : mappings) {
			int mappingLength =
					mapping.getClasses().stream().mapToInt(c -> c.getFields().size() + c.getMethods().size() + 1).sum();
			writer.append("# reflection-remapper | %d.%d.%d %d-%d".formatted(mapping.getMajor(), mapping.getMinor(),
					mapping.getPatch(), lastLine + 1, lastLine + mappingLength));
			writer.newLine();
			lastLine += mappingLength;
		}

		writer.append(MappingFileReader.VERSIONS_NOTICE);
		writer.newLine();
	}

	private void writeSingleMapping(@NotNull BufferedWriter writer, @NotNull VersionedMappings mappings)
			throws IOException {
		for (var mappedClass : mappings.getClasses()) {
			writer.append("%s -> %s".formatted(mappedClass.getOriginalName(), mappedClass.getObfuscatedName()));
			writer.newLine();

			for (var mappedField : mappedClass.getFields()) {
				writer.append("    %s -> %s".formatted(mappedField.getOriginalName(), mappedField.getObfuscatedName()));
				writer.newLine();
			}

			for (var mappedMethod : mappedClass.getMethods()) {
				String parameters = Stream.of(mappedMethod.getParameterTypes())
						.map(parameter -> parameter.getTypeName())
						.collect(Collectors.joining(","));
				writer.append("    %s(%s) -> %s".formatted(mappedMethod.getOriginalName(), parameters,
						mappedMethod.getObfuscatedName()));
				writer.newLine();
			}
		}
	}

}
