package fr.skytasul.reflection;

import java.util.List;
import java.util.stream.Stream;

public class TestUtils {

	public static Version[] getVersionArray(String... versions) {
		return Stream.of(versions).map(Version::parse).toArray(Version[]::new);
	}

	public static List<String> getLines(String string) {
		return List.of(string.split("\\n"));
	}

}
