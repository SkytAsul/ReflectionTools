package fr.skytasul.reflection.mappings;

import org.jetbrains.annotations.NotNull;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface Mappings {

	Collection<? extends ClassMapping> getClasses();

	interface MappedObject {

		@NotNull
		String getOriginalName();

		@NotNull
		String getMappedName();

	}

	interface ClassMapping extends MappedObject, Type {

		@Override
		default @NotNull String getTypeName() {
			return getOriginalName();
		}

		@NotNull
		default ClassArrayType getArrayType() {
			return new ClassArrayType(this);
		}

		Collection<? extends FieldMapping> getFields();

		Collection<? extends MethodMapping> getMethods();

		interface FieldMapping extends MappedObject {
		}

		interface MethodMapping extends MappedObject {

			@NotNull
			Type @NotNull [] getParameterTypes();

		}

		record ClassArrayType(@NotNull ClassMapping componentMapping) implements Type {
			@Override
			public @NotNull String getTypeName() {
				return componentMapping().getTypeName() + "[]";
			}
		}

	}

	public static @NotNull String getStringForMethod(@NotNull String methodName, @NotNull Type... parameterTypes) {
		return methodName + Stream.of(parameterTypes).map(Type::getTypeName).collect(Collectors.joining(", ", "(", ")"));
	}

}
