package fr.skytasul.reflection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
	 * @param version version of the mappings
	 * @throws IOException
	 */
	public MappingFileReader(@NotNull Path path, @NotNull Version version) throws IOException {
		this(path);

		mappings = List.of(new VersionPart(new VersionedMappingsImplementation(version), -1, -1));
	}

	public @NotNull List<Version> readAvailableVersions() {
		if (mappings != null)
			throw new IllegalStateException("Versions are already known");

		Matcher versionMatcher;
		for (String line : lines) {
			if (line.equals(VERSIONS_NOTICE)) {
				if (mappings == null)
					mappings = new ArrayList<>();
				else
					return mappings.stream().map(x -> x.mappings.getVersion()).toList();
			} else if (mappings != null && (versionMatcher = VERSION_PATTERN.matcher(line)).matches()) {
				mappings.add(new VersionPart(
						new VersionedMappingsImplementation(new Version(
								Integer.parseInt(versionMatcher.group("major")),
								Integer.parseInt(versionMatcher.group("minor")),
								Integer.parseInt(versionMatcher.group("patch")))),
						Integer.parseInt(versionMatcher.group("firstline")),
						Integer.parseInt(versionMatcher.group("lastline"))));
			}
		}

		if (mappings == null)
			throw new IllegalArgumentException("File does not contain version information");

		// if we are here, we didn't return at the end of the version informations block: error
		throw new IllegalArgumentException("Invalid syntax: no end to the version informations");
	}

	public @NotNull List<Version> getAvailableVersions() {
		if (mappings == null)
			throw new IllegalStateException("No versions are available for now");

		return mappings.stream().map(x -> x.mappings.getVersion()).sorted().toList();
	}

	public boolean keepOnlyVersion(@NotNull Version version) {
		boolean found = false;
		for (var iterator = mappings.iterator(); iterator.hasNext();) {
			if (version.equals(iterator.next().mappings.getVersion()))
				found = true;
			else
				iterator.remove();
		}
		return found;
	}

	public @NotNull Optional<Version> keepBestMatchedVersion(@NotNull Version targetVersion) {
		var foundVersion = getBestMatchedVersion(targetVersion, getAvailableVersions());
		if (foundVersion.isPresent())
			keepOnlyVersion(foundVersion.get());
		return foundVersion;
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

	public @NotNull VersionedMappings getParsedMappings(@NotNull Version version) {
		return mappings.stream().filter(x -> x.mappings.getVersion().equals(version)).findAny().orElseThrow().mappings;
	}

	private record VersionPart(VersionedMappingsImplementation mappings, int firstLine, int lastLine) {
	}

	public static @NotNull Optional<Version> getBestMatchedVersion(@NotNull Version targetVersion,
			@NotNull List<@NotNull Version> availableVersions) {
		Version lastVersion = null;
		for (var version : availableVersions) {
			if (version.is(targetVersion))
				return Optional.of(version);

			if (version.isBefore(targetVersion))
				lastVersion = version;

			if (version.isAfter(targetVersion))
				break;
		}
		return Optional.ofNullable(lastVersion);
	}

}
