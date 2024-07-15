package fr.skytasul.reflection;

import fr.skytasul.reflection.VersionedMappings.ClassHandle;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MappingReader {

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

	private static final Pattern CLASS_REGEX = Pattern.compile("(?'original'[\\w.$]+) -> (?'obfuscated'[\\w$]+)");
	private static final Pattern METHOD_REGEX = Pattern.compile(
			"    (?:\\d+:\\d+:)?[\\w.$\\[\\]]+ (?'original'[\\w<>]+)\\((?'parameters'[\\w.$, ]*)\\) -> (?'obfuscated'[\\w<>]+)");
	private static final Pattern FIELD_REGEX =
			Pattern.compile("    [\\w.$\\[\\]]+ (?'original'\\w+) -> (?'obfuscated'\\w+)");

	private static final Pattern METHOD_PARAMETERS_REGEX = Pattern.compile("([\\w.$]+)(\\[\\])?,?");

	public void parseAndFill(@NotNull VersionedMappings mappings, @NotNull List<String> lines) {
		List<ObfuscatedClass> classes = new ArrayList<>();

		String classOriginal = null, classObfuscated = null;
		List<ObfuscatedMethod> classMethods = null;
		List<ObfuscatedField> classFields = null;

		for (String line : lines) {
			if (line.startsWith("#"))
				continue; // comment, ignore

			Matcher classMatch, methodMatch, fieldMatch;
			if ((classMatch = CLASS_REGEX.matcher(line)).matches()) {
				// class: we need to close the previous class
				if (classOriginal != null)
					classes.add(new ObfuscatedClass(classOriginal, classObfuscated, classMethods, classFields));

				classOriginal = classMatch.group("original");
				classObfuscated = classMatch.group("obfuscated");
				classMethods = new ArrayList<>();
				classFields = new ArrayList<>();
			} else if ((methodMatch = METHOD_REGEX.matcher(line)).matches()) {
				classMethods.add(new ObfuscatedMethod(methodMatch.group("original"), methodMatch.group("obfuscated"),
						methodMatch.group("parameters")));
			} else if ((fieldMatch = FIELD_REGEX.matcher(line)).matches()) {
				classFields.add(new ObfuscatedField(fieldMatch.group("original"), fieldMatch.group("obfuscated")));
			} else
				LOGGER.log(Level.WARNING, "Failed to parse line {}", line);
		}

		// we first create all the ClassHandles BEFORE creating the methods because we need references to
		// the ClassHandle for method parameters
		mappings.classes = classes.stream().map(clazz -> new ClassHandle(clazz.original, clazz.obfuscated)).toList();
		for (var clazz : classes) {
			try {
				ClassHandle handle = mappings.getClass(clazz.original);

				handle.fields = clazz.fields.stream().map(field -> handle.new FieldHandle(field.original, field.obfuscated))
						.toList();
				handle.methods = clazz.methods.stream().map(method -> {

					return handle.new MethodHandle(method.original, method.obfuscated, parameters);
				}).toList();
			} catch (ClassNotFoundException ex) {
				// cannot happen
				throw new Error(ex);
			}
		}
	}

	private static @NotNull ClassHandle @NotNull [] parseParameters(@NotNull String parameters,
			@NotNull VersionedMappings mappings) {
		Matcher matcher = METHOD_PARAMETERS_REGEX.matcher(parameters);
		while (matcher.find()) {
			String typeName = matcher.group(1);
			boolean isArray = matcher.group(2) != null;

			Class<?> type = PRIMITIVES.get(typeName);
			if (type)
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
