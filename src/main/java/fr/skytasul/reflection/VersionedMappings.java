package fr.skytasul.reflection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.lang.reflect.*;
import java.util.Collection;

public interface VersionedMappings {

	int getMajor();

	int getMinor();

	int getPatch();

	default boolean isVersion(int major, int minor, int patch) {
		return getMajor() == major && getMinor() == minor && getPatch() == patch;
	}

	/**
	 * Checks if the version represented by those mappings is after or equal to the version passed as
	 * parameters.
	 *
	 * @param major
	 * @param minor
	 * @param patch
	 * @return <code>true</code> if the current version is after (inclusive) the passed version
	 */
	default boolean isAfter(int major, int minor, int patch) {
		if (getMajor() > major)
			return true;
		if (getMajor() < major)
			return false;

		if (getMinor() > minor)
			return true;
		if (getMinor() < minor)
			return false;

		return getPatch() >= patch;
	}

	/**
	 * Checks if the version represented by those mappings is strictly before the version passed as
	 * parameters.
	 *
	 * @param major
	 * @param minor
	 * @param patch
	 * @return <code>true</code> if the current version is before (exclusive) the passed version
	 */
	default boolean isBefore(int major, int minor, int patch) {
		return !isAfter(major, minor, patch);
	}

	@NotNull
	MappedClass getClass(@NotNull String name) throws ClassNotFoundException;

	Collection<? extends MappedClass> getClasses();

	interface MappedNamedObject {

		@NotNull
		String getOriginalName();

		@NotNull
		String getObfuscatedName();

	}

	interface MappedClass extends Type, MappedNamedObject {

		@NotNull
		Type getArrayType();

		@NotNull
		Class<?> getMappedClass() throws ClassNotFoundException;

		@NotNull
		MappedField getField(@NotNull String original) throws NoSuchFieldException;

		@NotNull
		default Field getMappedField(@NotNull String original)
				throws NoSuchFieldException, SecurityException, ClassNotFoundException {
			return getField(original).getMappedField();
		}

		Collection<? extends MappedField> getFields();

		@NotNull
		MappedMethod getMethod(@NotNull String original, @NotNull Type... parameterTypes)
				throws NoSuchMethodException, ClassNotFoundException;

		@NotNull
		default Method getMappedMethod(@NotNull String original, @NotNull Type... parameterTypes)
				throws NoSuchMethodException, ClassNotFoundException {
			return getMethod(original).getMappedMethod();
		}

		Collection<? extends MappedMethod> getMethods();

		@NotNull
		MappedConstructor getConstructor(@NotNull Type... parameterTypes)
				throws NoSuchMethodException, SecurityException, ClassNotFoundException;

		@NotNull
		default Constructor<?> getMappedConstructor(@NotNull Type... parameterTypes)
				throws NoSuchMethodException, SecurityException, ClassNotFoundException {
			return getConstructor(parameterTypes).getMappedConstructor();
		}

		interface MappedField extends MappedNamedObject {

			Field getMappedField() throws NoSuchFieldException, SecurityException, ClassNotFoundException;

			Object get(@Nullable Object instance) throws IllegalArgumentException, IllegalAccessException,
					NoSuchFieldException, SecurityException, ClassNotFoundException;

			void set(@Nullable Object instance, Object value) throws IllegalArgumentException, IllegalAccessException,
					NoSuchFieldException, SecurityException, ClassNotFoundException;

		}

		interface MappedMethod extends MappedNamedObject {

			@NotNull
			Type @NotNull [] getParameterTypes();

			Method getMappedMethod() throws NoSuchMethodException, SecurityException, ClassNotFoundException;

			Object invoke(@Nullable Object instance, @Nullable Object... args)
					throws IllegalAccessException, IllegalArgumentException, InvocationTargetException,
					NoSuchMethodException, SecurityException, ClassNotFoundException;

		}

		interface MappedConstructor {

			Constructor<?> getMappedConstructor();

			Object newInstance(@Nullable Object... args) throws InstantiationException, IllegalAccessException,
					IllegalArgumentException, InvocationTargetException;

		}

	}

}
