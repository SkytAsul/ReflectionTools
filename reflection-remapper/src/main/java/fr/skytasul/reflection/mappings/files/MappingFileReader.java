package fr.skytasul.reflection.mappings.files;

import fr.skytasul.reflection.Version;
import fr.skytasul.reflection.mappings.Mappings;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MappingFileReader {

	private static final Pattern VERSION_PATTERN = Pattern.compile(
			"# reflection-remapper \\| (?<major>\\d+)\\.(?<minor>\\d+)\\.(?<patch>\\d+)");

	private final @NotNull MappingType mappingType;
	private final @NotNull List<VersionPart> mappings;

	/**
	 * Creates a reader for a composite mappings file.
	 *
	 * @param mappingType type of mappings in this file
	 * @param lines of the mappings file
	 * @throws IOException
	 */
	public MappingFileReader(@NotNull MappingType mappingType, @NotNull List<String> lines) throws IOException {
		this.mappingType = mappingType;

		this.mappings = readParts(lines);
	}

	/**
	 * Creates a reader for a plain mappings file, associated with its version.
	 *
	 * @param mappingType type of mappings in this file
	 * @param lines of the mappings file
	 * @param version version of the mappings
	 * @throws IOException
	 */
	public MappingFileReader(@NotNull MappingType mappingType, @NotNull List<String> lines, @NotNull Version version)
			throws IOException {
		this.mappingType = mappingType;

		this.mappings = List.of(new VersionPart(version, lines));
	}

	public @NotNull List<Version> getAvailableVersions() {
		return mappings.stream().map(x -> x.version).sorted().toList();
	}

	public boolean keepOnlyVersion(@NotNull Version version) {
		if (!mappings.stream().anyMatch(part -> part.version.equals(version)))
			return false;
		mappings.removeIf(part -> !part.version.equals(version));
		return true;
	}

	public @NotNull Optional<Version> keepBestMatchedVersion(@NotNull Version targetVersion) {
		var foundVersion = getBestMatchedVersion(targetVersion, getAvailableVersions());
		if (foundVersion.isPresent())
			keepOnlyVersion(foundVersion.get());
		return foundVersion;
	}

	public void parseMappings() {
		for (var version : mappings)
			version.mappings = mappingType.parse(version.lines);
	}

	public @NotNull Mappings getParsedMappings(@NotNull Version version) {
		return mappings.stream().filter(x -> x.version.equals(version)).findAny().orElseThrow().mappings;
	}

	private static class VersionPart {
		private final Version version;
		private final List<String> lines;
		private Mappings mappings;

		private VersionPart(Version version, List<String> lines) {
			this.version = version;
			this.lines = lines;
		}
	}

	private static @NotNull List<VersionPart> readParts(@NotNull List<String> lines) {
		List<VersionPart> parts = new ArrayList<>();

		VersionPart currentPart = null;
		for (String line : lines) {
			Matcher versionMatcher = VERSION_PATTERN.matcher(line);
			if (versionMatcher.matches()) {
				if (currentPart != null)
					parts.add(currentPart);
				currentPart = new VersionPart(new Version(
						Integer.parseInt(versionMatcher.group("major")),
						Integer.parseInt(versionMatcher.group("minor")),
						Integer.parseInt(versionMatcher.group("patch"))), new ArrayList<>());
			} else if (line.startsWith("#") || line.isBlank()) {
				continue; // ignore comments
			} else {
				if (currentPart == null)
					throw new IllegalArgumentException("File should start with a version information");

				currentPart.lines.add(line);
			}
		}

		if (currentPart != null)
			parts.add(currentPart);

		return parts;
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
