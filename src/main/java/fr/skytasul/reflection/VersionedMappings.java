package fr.skytasul.reflection;

import org.jetbrains.annotations.NotNull;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

public interface VersionedMappings {

	int getMajor();

	int getMinor();

	int getPatch();

	boolean isVersion(int major, int minor, int patch);

	/**
	 * Checks if the version represented by those mappings is after or equal to the version passed as
	 * parameters.
	 *
	 * @param major
	 * @param minor
	 * @param patch
	 * @return <code>true</code> if the current version is after (inclusive) the passed version
	 */
	boolean isAfter(int major, int minor, int patch);

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

	interface MappedClass extends Type {

		@NotNull
		Type getArrayType();

		@NotNull
		Class<?> getMappedClass() throws ClassNotFoundException;

		@NotNull
		Field getMappedField(@NotNull String key) throws NoSuchFieldException, SecurityException, ClassNotFoundException;

		@NotNull
		Method getMappedMethod(@NotNull String key, @NotNull Type... parameterTypes)
				throws NoSuchMethodException, SecurityException, ClassNotFoundException;

	}

}
