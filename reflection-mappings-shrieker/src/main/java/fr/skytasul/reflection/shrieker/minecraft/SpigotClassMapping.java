package fr.skytasul.reflection.shrieker.minecraft;

import fr.skytasul.reflection.mappings.Mappings;
import fr.skytasul.reflection.mappings.RealMappings;
import fr.skytasul.reflection.mappings.RealMappings.RealClassMapping;
import fr.skytasul.reflection.mappings.files.MappingType;
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

}
