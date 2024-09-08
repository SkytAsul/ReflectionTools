package fr.skytasul.reflection.mappings.files;

import fr.skytasul.reflection.mappings.Mappings;
import fr.skytasul.reflection.mappings.RealMappings;
import fr.skytasul.reflection.mappings.RealMappings.RealClassMapping;
import fr.skytasul.reflection.mappings.RealMappings.RealClassMapping.RealFieldMapping;
import fr.skytasul.reflection.mappings.RealMappings.RealClassMapping.RealMethodMapping;
import org.jetbrains.annotations.NotNull;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Reads ProGuard obfuscation map.
 * <p>
 * Difference with the original format:
 * <ul>
 * <li>for fields and methods, it is acceptable to only have the original name and obfuscated name,
 * without the type of the field or method.
 * <li>for methods, it is acceptable not to have the line index prefix
 * </ul>
 */
public class ProguardMapping implements MappingType {

	private static final Map<String, Class<?>> PRIMITIVES = Map.of(
			"boolean", boolean.class,
			"byte", byte.class,
			"short", short.class,
			"int", int.class,
			"long", long.class,
			"float", float.class,
			"double", double.class,
			"char", char.class);

	private static final Logger LOGGER = Logger.getLogger("MappingReader");

	private static final Pattern CLASS_REGEX = Pattern.compile("(?<original>[\\w.$]+) -> (?<obfuscated>[\\w.$]+):");
	private static final Pattern METHOD_REGEX = Pattern.compile(
			"    (?:(?:\\d+:\\d+:)?[\\w.$\\[\\]]+ )?(?<original>[\\w<>$]+)\\((?<parameters>[\\w.$, \\[\\]]*)\\) -> (?<obfuscated>[\\w<>]+)");
	private static final Pattern FIELD_REGEX =
			Pattern.compile("    (?:[\\w.$\\[\\]]+ )?(?<original>[\\w$]+) -> (?<obfuscated>\\w+)");

	private static final Pattern METHOD_PARAMETERS_REGEX = Pattern.compile("([\\w.$]+)(\\[\\])?,?");

	private final boolean failOnLineParse;

	public ProguardMapping(boolean failOnLineParse) {
		this.failOnLineParse = failOnLineParse;
	}

	@Override
	public Mappings parse(@NotNull List<String> lines) {
		List<ObfuscatedClass> parsedClasses = new ArrayList<>();

		String classOriginal = null, classObfuscated = null;
		List<ObfuscatedMethod> classMethods = null;
		List<ObfuscatedField> classFields = null;

		for (String line : lines) {
			if (line.startsWith("#") || line.contains("package-info"))
				continue; // comment, ignore

			Matcher classMatch, methodMatch, fieldMatch;
			if ((classMatch = CLASS_REGEX.matcher(line)).matches()) {
				// class: we need to close the previous class
				if (classOriginal != null)
					parsedClasses.add(new ObfuscatedClass(classOriginal, classObfuscated, classMethods, classFields));

				classOriginal = classMatch.group("original");
				classObfuscated = classMatch.group("obfuscated");
				classMethods = new ArrayList<>();
				classFields = new ArrayList<>();
			} else if ((methodMatch = METHOD_REGEX.matcher(line)).matches()) {
				classMethods.add(new ObfuscatedMethod(methodMatch.group("original"), methodMatch.group("obfuscated"),
						methodMatch.group("parameters")));
			} else if ((fieldMatch = FIELD_REGEX.matcher(line)).matches()) {
				classFields.add(new ObfuscatedField(fieldMatch.group("original"), fieldMatch.group("obfuscated")));
			} else {
				if (failOnLineParse)
					throw new IllegalArgumentException("Failed to parse line " + line);
				else
					LOGGER.log(Level.WARNING, "Failed to parse line {0}", line);
			}
		}
		// we close the last class
		if (classOriginal != null)
			parsedClasses.add(new ObfuscatedClass(classOriginal, classObfuscated, classMethods, classFields));

		LOGGER.log(Level.FINE, "Found {0} classes to remap", parsedClasses.size());

		var fakeHandles = new HashMap<String, RealClassMapping>();
		var classes = parsedClasses.stream()
				.map(clazz -> new RealClassMapping(clazz.original, clazz.obfuscated, new ArrayList<>(), new ArrayList<>()))
				.collect(Collectors.toMap(RealClassMapping::getOriginalName, Function.identity()));
		for (var parsedClass : parsedClasses) {
			var classMapping = classes.get(parsedClass.original);

			classMapping.fields().addAll(parsedClass.fields
					.stream()
					.map(field -> new RealFieldMapping(field.original, field.obfuscated))
					.toList());
			classMapping.methods().addAll(parsedClass.methods
					.stream()
					.map(method -> new RealMethodMapping(method.original, method.obfuscated,
							parseParameters(method.parameters, fakeHandles, classes)))
					.toList());
		}
		return new RealMappings(classes.values());
	}

	protected @NotNull Type @NotNull [] parseParameters(@NotNull String parameters,
			Map<@NotNull String, RealClassMapping> fakeHandles, Map<@NotNull String, RealClassMapping> classes) {
		List<Type> types = new ArrayList<>(2);

		Matcher matcher = METHOD_PARAMETERS_REGEX.matcher(parameters);
		while (matcher.find()) {
			String typeName = matcher.group(1);
			boolean isArray = matcher.group(2) != null;

			Class<?> clazz = null;
			RealClassMapping handle = classes.get(typeName);
			if (handle == null) {
				// the type is not present in the mappings: must be a primitive or a Java library type
				clazz = PRIMITIVES.get(typeName);
				if (clazz == null) {
					// the type is not a primitive: must be a library type
					try {
						clazz = Class.forName(typeName);
					} catch (ClassNotFoundException __) {
						if (!fakeHandles.containsKey(typeName)) {
							LOGGER.log(Level.FINER, "Cannot find class {0}", typeName);
							fakeHandles.put(typeName, new RealClassMapping(typeName, typeName, Collections.emptyList(),
									Collections.emptyList())); // not ideal
						}
						handle = fakeHandles.get(typeName);
					}
				}
			}

			Type type;
			if (handle != null)
				type = isArray ? handle.getArrayType() : handle;
			else
				type = isArray ? clazz.arrayType() : clazz;

			types.add(type);
		}

		return types.toArray(Type[]::new);
	}

	@Override
	public void write(@NotNull BufferedWriter writer, @NotNull Mappings mappings) throws IOException {
		for (var mappedClass : mappings.getClasses()) {
			writer.append("%s -> %s:".formatted(mappedClass.getOriginalName(), mappedClass.getMappedName()));
			writer.newLine();

			for (var mappedField : mappedClass.getFields()) {
				writer.append("    %s -> %s".formatted(mappedField.getOriginalName(), mappedField.getMappedName()));
				writer.newLine();
			}

			for (var mappedMethod : mappedClass.getMethods()) {
				String parameters = Stream.of(mappedMethod.getParameterTypes())
						.map(parameter -> parameter.getTypeName())
						.collect(Collectors.joining(","));
				writer.append("    %s(%s) -> %s".formatted(mappedMethod.getOriginalName(), parameters,
						mappedMethod.getMappedName()));
				writer.newLine();
			}
		}
	}

	private record ObfuscatedClass(String original, String obfuscated, List<ObfuscatedMethod> methods,
			List<ObfuscatedField> fields) {
	}

	private record ObfuscatedMethod(String original, String obfuscated, String parameters) {
	}

	private record ObfuscatedField(String original, String obfuscated) {
	}

}
