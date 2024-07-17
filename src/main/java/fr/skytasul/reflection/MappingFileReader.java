package fr.skytasul.reflection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MappingFileReader {

	protected static final String VERSIONS_NOTICE = "# reflection-remapper | AVAILABLE VERSIONS";
	private static final Pattern VERSION_PATTERN = Pattern.compile(
			"# reflection-remapper \\| (?<major>\\d+)\\.(?<minor>\\d+)\\.(?<patch>\\d+) (?<firstline>\\d+)-(?<lastline>\\d+)");

	private final @NotNull Path path;
	private final @NotNull List<String> lines;

	private @Nullable List<VersionPart> mappings;

	/**
	 * Creates a reader for a composite mappings file.
	 *
	 * @param path path to the mappings file
	 * @throws IOException
	 */
	public MappingFileReader(@NotNull Path path) throws IOException {
		this.path = path;

		lines = Files.readAllLines(path);
	}

	/**
	 * Creates a reader for a plain mappings file, associated with its version.
	 *
	 * @param path path to the mappings file
	 * @param major
	 * @param minor
	 * @param patch
	 * @throws IOException
	 */
	public MappingFileReader(@NotNull Path path, int major, int minor, int patch) throws IOException {
		this(path);

		mappings = List.of(new VersionPart(new VersionedMappingsImplementation(major, minor, patch), -1, -1));
	}

	public @NotNull List<VersionedMappings> getAvailableVersions() {
		if (mappings == null)
			throw new IllegalStateException("No versions are available for now");

		return mappings.stream().map(x -> (VersionedMappings) x.mappings).toList();
	}

	public void readAvailableVersions() {
		if (mappings != null)
			throw new IllegalStateException("Versions are already known");

		Matcher versionMatcher;
		for (String line : lines) {
			if (line.equals(VERSIONS_NOTICE)) {
				if (mappings == null)
					mappings = new ArrayList<>();
				else
					return;
			} else if (mappings != null && (versionMatcher = VERSION_PATTERN.matcher(line)).matches()) {
				mappings.add(new VersionPart(
						new VersionedMappingsImplementation(
								Integer.parseInt(versionMatcher.group("major")),
								Integer.parseInt(versionMatcher.group("minor")),
								Integer.parseInt(versionMatcher.group("patch"))),
						Integer.parseInt(versionMatcher.group("firstline")),
						Integer.parseInt(versionMatcher.group("lastline"))));
			}
		}

		if (mappings == null)
			throw new IllegalArgumentException("File does not contain version information");
		if (mappings != null)
			throw new IllegalArgumentException("Invalid syntax: no end to the version informations");
	}

	public void keepOnlyVersion(int major, int minor, int patch) {
		for (var iterator = mappings.iterator(); iterator.hasNext();) {
			var version = iterator.next();
			if (!version.mappings.isVersion(major, minor, patch))
				iterator.remove();
		}
	}

	public void parseMappings() {
		for (var version : mappings) {
			MappingParser parser = new MappingParser(version.mappings);
			List<String> linesToParse;
			if (version.firstLine == -1 && version.lastLine == -1)
				// plain mappings file containing only one version
				linesToParse = lines;
			else
				// composite file
				linesToParse = lines.subList(version.firstLine, version.lastLine + 1);
			parser.parseAndFill(linesToParse);
		}
	}

	private record VersionPart(VersionedMappingsImplementation mappings, int firstLine, int lastLine) {
	}

}
