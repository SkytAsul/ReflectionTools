package fr.skytasul.reflection.mappings.files;

import fr.skytasul.reflection.Version;
import fr.skytasul.reflection.mappings.Mappings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MappingFileReader {

	protected static final String VERSIONS_NOTICE = "# reflection-remapper | AVAILABLE VERSIONS";
	private static final Pattern VERSION_PATTERN = Pattern.compile(
			"# reflection-remapper \\| (?<major>\\d+)\\.(?<minor>\\d+)\\.(?<patch>\\d+) (?<firstline>\\d+)-(?<lastline>\\d+)");

	private final @NotNull MappingType mappingType;
	private final @NotNull List<String> lines;

	private @Nullable List<VersionPart> mappings;

	/**
	 * Creates a reader for a composite mappings file.
	 *
	 * @param mappingType type of mappings in this file
	 * @param lines of the mappings file
	 * @throws IOException
	 */
	public MappingFileReader(@NotNull MappingType mappingType, @NotNull List<String> lines) throws IOException {
		this.mappingType = mappingType;
		this.lines = lines;
	}

	/**
	 * Creates a reader for a plain mappings file, associated with its version.
	 *
	 * @param mappingType type of mappings in this file
	 * @param lines of the mappings file
	 * @param version version of the mappings
	 * @throws IOException
	 */
	public MappingFileReader(@NotNull MappingType mappingType, @NotNull List<String> lines,
			@NotNull Version version) throws IOException {
		this(mappingType, lines);

		mappings = List.of(new VersionPart(version, -1, -1));
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
					return getAvailableVersions();
			} else if (mappings != null && (versionMatcher = VERSION_PATTERN.matcher(line)).matches()) {
				mappings.add(new VersionPart(
						new Version(
								Integer.parseInt(versionMatcher.group("major")),
								Integer.parseInt(versionMatcher.group("minor")),
								Integer.parseInt(versionMatcher.group("patch"))),
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

		return mappings.stream().map(x -> x.version).sorted().toList();
	}

	public boolean keepOnlyVersion(@NotNull Version version) {
		for (var mapping : mappings) {
			if (version.equals(mapping.version)) {
				mappings = List.of(mapping);
				return true;
			}
		}
		return false;
	}

	public @NotNull Optional<Version> keepBestMatchedVersion(@NotNull Version targetVersion) {
		var foundVersion = getBestMatchedVersion(targetVersion, getAvailableVersions());
		if (foundVersion.isPresent())
			keepOnlyVersion(foundVersion.get());
		return foundVersion;
	}

	public void parseMappings() {
		for (var version : mappings) {
			List<String> linesToParse;
			if (version.firstLine == -1 && version.lastLine == -1)
				// plain mappings file containing only one version
				linesToParse = lines;
			else
				// composite file
				linesToParse = lines.subList(version.firstLine, version.lastLine + 1);
			version.mappings = mappingType.parse(linesToParse);
		}
	}

	public @NotNull Mappings getParsedMappings(@NotNull Version version) {
		return mappings.stream().filter(x -> x.version.equals(version)).findAny().orElseThrow().mappings;
	}

	private class VersionPart {
		private final Version version;
		private final int firstLine;
		private final int lastLine;
		private Mappings mappings;

		private VersionPart(Version version, int firstLine, int lastLine) {
			this.version = version;
			this.firstLine = firstLine;
			this.lastLine = lastLine;
		}
	}

	/**
	 * Returns the version present in the available versions list that matches the best the target
	 * version.
	 * <p>
	 * If there is a perfect match (i.e. the target version is present in the list) then the target
	 * version is returned.
	 * <p>
	 * If there is no perfect match but a version lower than the target is present, it will be returned.
	 * <p>
	 * If only greater versions are available, then an empty Optional is returned.
	 *
	 * @param targetVersion target version
	 * @param availableVersions available versions (<strong>must be sorted !</strong>)
	 * @return an optional containing a matching version if present, otherwise an empty optional
	 */
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
