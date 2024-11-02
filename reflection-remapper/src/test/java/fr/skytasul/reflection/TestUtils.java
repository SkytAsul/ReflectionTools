package fr.skytasul.reflection;

import java.util.List;

public class TestUtils {

	public static List<String> getLines(String string) {
		return List.of(string.split("\\n"));
	}

}
