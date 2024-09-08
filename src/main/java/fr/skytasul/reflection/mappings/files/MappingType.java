package fr.skytasul.reflection.mappings.files;

import fr.skytasul.reflection.mappings.Mappings;
import org.jetbrains.annotations.NotNull;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

public interface MappingType {

	@NotNull
	Mappings parse(@NotNull List<String> lines);

	void write(@NotNull BufferedWriter writer, @NotNull Mappings mappings) throws IOException;

}
