package fr.skytasul.reflection.shrieker.minecraft;

import fr.skytasul.reflection.mappings.Mappings;
import fr.skytasul.reflection.mappings.RealMappings;
import fr.skytasul.reflection.mappings.RealMappings.RealClassMapping;
import fr.skytasul.reflection.mappings.RealMappings.RealClassMapping.RealMethodMapping;
import fr.skytasul.reflection.mappings.files.MappingType;
import org.jetbrains.annotations.NotNull;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class SpigotMemberMapping implements MappingType {

	// https://docs.oracle.com/javase%2F7%2Fdocs%2Fjdk%2Fapi%2Fjpda%2Fjdi%2F%2F/com/sun/jdi/doc-files/signature.html

	private static final Map<Character, Class<?>> PRIMITIVES = Map.of(
			'Z', boolean.class,
			'B', byte.class,
			'C', char.class,
			'S', short.class,
			'I', int.class,
			'J', long.class,
			'F', float.class,
			'D', double.class);

	private static final Pattern METHOD_SIGNATURE_PATTERN = Pattern.compile("^\\((?<parameters>.*?)\\).+$");
	private static final Pattern TYPE_PATTERN = Pattern.compile("(?<array>\\[?)(?:L(?<type>[\\w/$]*?);|(?<primitive>\\w))");

	private static final Logger LOGGER = Logger.getLogger("SpigotMemberMapping");

	private final boolean failOnLineParse;

	public SpigotMemberMapping(boolean failOnLineParse) {
		this.failOnLineParse = failOnLineParse;
	}

	@Override
	public @NotNull Mappings parse(@NotNull List<String> lines) {
		var classes = new HashMap<String, RealClassMapping>();
		for (String line : lines) {
			if (line.startsWith("#"))
				continue;

			String[] columns = line.split(" ");
			if (columns.length == 4) {
				String className = columns[0].replace('/', '.');
				var classMapping = classes.computeIfAbsent(className,
						__ -> new RealClassMapping(className, className, List.of(), new ArrayList<>()));

				String original = columns[1];
				String signature = columns[2];
				String mapped = columns[3];

				var matcher = METHOD_SIGNATURE_PATTERN.matcher(signature);
				if (!matcher.matches())
					throw new IllegalArgumentException("Failed to parse method signature " + signature);
				String parameters = matcher.group("parameters");

				classMapping.methods().add(new RealMethodMapping(original, mapped, parseParameters(parameters)));
			} else {
				if (failOnLineParse)
					throw new IllegalArgumentException("Failed to parse line " + line);
				else
					LOGGER.log(Level.WARNING, "Failed to parse line {0}", line);
			}
		}
		return new RealMappings(classes.values());
	}

	protected @NotNull Type @NotNull [] parseParameters(@NotNull String parameters) {
		var matcher = TYPE_PATTERN.matcher(parameters);

		var types = new ArrayList<Type>(2);
		while (matcher.find()) {
			boolean isArray = !matcher.group("array").isEmpty();
			String typeName = matcher.group("type");
			String primitiveName = matcher.group("primitive");

			Class<?> clazz = null;
			Type type = null;

			if (primitiveName != null) {
				clazz = PRIMITIVES.get(primitiveName.charAt(0));
			} else {
				typeName = typeName.replace('/', '.');
				try {
					clazz = Class.forName(typeName);
				} catch (ClassNotFoundException ex) {
					type = new FakeType(typeName);
				}
			}

			if (clazz != null)
				type = isArray ? clazz.arrayType() : clazz;
			else if (isArray)
				type = new Mappings.ClassMapping.ClassArrayType(type);

			types.add(type);
		}
		return types.toArray(Type[]::new);
	}

	@Override
	public void write(@NotNull BufferedWriter writer, @NotNull Mappings mappings) throws IOException {
		throw new UnsupportedOperationException();
	}

	protected static record FakeType(String name) implements Type {
		@Override
		public String getTypeName() {
			return name;
		}
	}

}
