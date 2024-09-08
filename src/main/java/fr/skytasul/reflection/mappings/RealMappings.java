package fr.skytasul.reflection.mappings;

import org.jetbrains.annotations.NotNull;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

public record RealMappings(@NotNull Collection<RealClassMapping> classes) implements Mappings {

	@Override
	public Collection<? extends ClassMapping> getClasses() {
		return classes;
	}

	public static record RealClassMapping(@NotNull String original, @NotNull String mapped,
			@NotNull List<RealFieldMapping> fields, @NotNull List<RealMethodMapping> methods) implements ClassMapping {

		@Override
		public @NotNull String getOriginalName() {
			return original;
		}

		@Override
		public @NotNull String getMappedName() {
			return mapped;
		}

		@Override
		public Collection<? extends FieldMapping> getFields() {
			return fields;
		}

		@Override
		public Collection<? extends MethodMapping> getMethods() {
			return methods;
		}

		public static record RealFieldMapping(@NotNull String original, @NotNull String mapped) implements FieldMapping {

			@Override
			public @NotNull String getOriginalName() {
				return original;
			}

			@Override
			public @NotNull String getMappedName() {
				return mapped;
			}

		}

		public static record RealMethodMapping(@NotNull String original, @NotNull String mapped,
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
