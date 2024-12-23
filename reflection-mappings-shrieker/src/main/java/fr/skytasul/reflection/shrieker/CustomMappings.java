package fr.skytasul.reflection.shrieker;

import fr.skytasul.reflection.mappings.Mappings;
import fr.skytasul.reflection.mappings.Mappings.ClassMapping.FieldMapping;
import fr.skytasul.reflection.mappings.Mappings.ClassMapping.MethodMapping;
import fr.skytasul.reflection.shrieker.CustomMappings.CustomClassMapping.CustomFieldMapping;
import fr.skytasul.reflection.shrieker.CustomMappings.CustomClassMapping.CustomMethodMapping;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class CustomMappings implements Mappings {

	private static final Logger LOGGER = Logger.getLogger("CustomMappings");

	private final Map<String, CustomClassMapping> classes;

	public CustomMappings(Mappings existingMappings) {
		classes = existingMappings.getClasses().stream()
				.map(classMapping -> new CustomClassMapping(classMapping.getOriginalName(), classMapping.getMappedName()))
				.collect(Collectors.toMap(CustomClassMapping::getOriginalName, Function.identity()));

		for (ClassMapping classMapping : existingMappings.getClasses()) {
			var customClassMapping = getClass(classMapping.getOriginalName());
			for (FieldMapping fieldMapping : classMapping.getFields()) {
				customClassMapping.fields
						.add(new CustomFieldMapping(fieldMapping.getOriginalName(), fieldMapping.getMappedName()));
			}
			for (MethodMapping methodMapping : classMapping.getMethods()) {
				customClassMapping.methods
						.add(new CustomMethodMapping(methodMapping.getOriginalName(), methodMapping.getMappedName(),
								methodMapping.getParameterTypes()));
			}

		}
	}

	@Override
	public Collection<CustomClassMapping> getClasses() {
		return classes.values();
	}

	public @Nullable CustomClassMapping getClass(@NotNull String originalName) {
		return classes.get(originalName);
	}

	public @Nullable CustomClassMapping getClassFromMapped(@NotNull String mappedName) {
		return classes.values().stream()
				.filter(classMapping -> classMapping.mapped.equals(mappedName))
				.findAny()
				.orElse(null);
	}

	public static class CustomClassMapping implements ClassMapping {

		private @NotNull String original;
		private @NotNull String mapped;
		private final @NotNull List<CustomFieldMapping> fields = new ArrayList<>();
		private final @NotNull List<CustomMethodMapping> methods = new ArrayList<>();

		public CustomClassMapping(@NotNull String original, @NotNull String mapped) {
			this.original = original;
			this.mapped = mapped;
		}

		@Override
		public @NotNull String getOriginalName() {
			return original;
		}

		public void setOriginalName(@NotNull String original) {
			this.original = original;
		}

		@Override
		public @NotNull String getMappedName() {
			return mapped;
		}

		public void setMappedName(@NotNull String mapped) {
			this.mapped = mapped;
		}

		@Override
		public List<CustomFieldMapping> getFields() {
			return fields;
		}

		@Override
		public List<CustomMethodMapping> getMethods() {
			return methods;
		}

		public CustomClassMapping inheritsFrom(@NotNull CustomClassMapping classMapping) {
			for (CustomFieldMapping inheritedFieldMapping : classMapping.fields) {
				var fieldMapping =
						fields.stream().filter(field -> field.original.equals(inheritedFieldMapping.original)).findAny();
				if (fieldMapping.isPresent()) {
					if (!fieldMapping.get().mapped.equals(inheritedFieldMapping.mapped))
						LOGGER.warning("Asked to add inheritance to equal mappings (%s, field %s)".formatted(original,
								inheritedFieldMapping.original));
				} else {
					fields.add(inheritedFieldMapping);
				}
			}

			for (CustomMethodMapping inheritedMethodMapping : classMapping.methods) {
				var methodMapping = methods.stream().filter(method -> method.original.equals(inheritedMethodMapping.original)
						&& method.isSameParameters(inheritedMethodMapping.parameterTypes)).findAny();
				if (methodMapping.isPresent()) {
					if (!methodMapping.get().mapped.equals(inheritedMethodMapping.mapped))
						LOGGER.warning("Asked to add inheritance to equal mappings (%s, method %s)".formatted(original,
								Mappings.getStringForMethod(inheritedMethodMapping.original,
										inheritedMethodMapping.parameterTypes)));
				} else {
					methods.add(inheritedMethodMapping);
				}
			}
			return this;
		}

		public static record CustomFieldMapping(@NotNull String original, @NotNull String mapped) implements FieldMapping {

			@Override
			public @NotNull String getOriginalName() {
				return original;
			}

			@Override
			public @NotNull String getMappedName() {
				return mapped;
			}

		}

		public static record CustomMethodMapping(@NotNull String original, @NotNull String mapped,
				@NotNull Type @NotNull [] parameterTypes) implements MethodMapping {

			@Override
			public @NotNull String getOriginalName() {
				return original;
			}

			@Override
			public @NotNull String getMappedName() {
				return mapped;
			}

			@Override
			public @NotNull Type @NotNull [] getParameterTypes() {
				return parameterTypes;
			}

		}

	}

}
