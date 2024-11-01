package fr.skytasul.reflection.mappings.files;

import fr.skytasul.reflection.mappings.Mappings;
import fr.skytasul.reflection.mappings.RealMappings;
import fr.skytasul.reflection.mappings.RealMappings.RealClassMapping;
import org.jetbrains.annotations.NotNull;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SpigotClassMapping implements MappingType {

	private static final Logger LOGGER = Logger.getLogger("SpigotClassMapping");

	private final boolean failOnLineParse;

	public SpigotClassMapping(boolean failOnLineParse) {
		this.failOnLineParse = failOnLineParse;
	}

	@Override
	public @NotNull Mappings parse(@NotNull List<String> lines) {
		var classes = new ArrayList<RealClassMapping>();
		for (String line : lines) {
			if (line.startsWith("#"))
				continue;

			String[] columns = line.split(" ");
			if (columns.length == 2) {
				String original = columns[0].replace('/', '.');
				String mapped = columns[1].replace('/', '.');
				classes.add(new RealClassMapping(original, mapped, List.of(), List.of()));
			} else {
				if (failOnLineParse)
					throw new IllegalArgumentException("Failed to parse line " + line);
				else
					LOGGER.log(Level.WARNING, "Failed to parse line {0}", line);
			}
		}
		return new RealMappings(classes);
	}

	@Override
	public void write(@NotNull BufferedWriter writer, @NotNull Mappings mappings) throws IOException {
		throw new UnsupportedOperationException();
	}

	// https://hub.spigotmc.org/versions/1.21.1.json
	// https://hub.spigotmc.org/stash/projects/SPIGOT/repos/builddata/raw/info.json?at=533b02cd6ba8dbf8c8607250b02bf2d8c36421e8
	// https://hub.spigotmc.org/stash/projects/SPIGOT/repos/builddata/raw/mappings/bukkit-1.21.1-cl.csrg?at=533b02cd6ba8dbf8c8607250b02bf2d8c36421e8
	// https://hub.spigotmc.org/stash/projects/SPIGOT/repos/builddata/raw/mappings/bukkit-1.17.1-members.csrg?at=a4785704979a469daa2b7f6826c84e7fe886bb03


}
