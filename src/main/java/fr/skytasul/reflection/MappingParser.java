package fr.skytasul.reflection;

import org.jetbrains.annotations.NotNull;
import java.util.List;


public interface MappingParser {

	MappingParser setFailOnLineParse(boolean failOnLineParse);

	void parseAndFill(List<String> lines);

}
