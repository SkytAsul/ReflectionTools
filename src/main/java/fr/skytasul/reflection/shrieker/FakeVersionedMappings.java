package fr.skytasul.reflection.shrieker;

import fr.skytasul.reflection.VersionedMappings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.lang.reflect.*;
import java.util.*;

public class FakeVersionedMappings implements VersionedMappings {

	private final int major;
	private final int minor;
	private final int patch;

	public final Map<String, FakeMappedClass> classes = new HashMap<>();

	public FakeVersionedMappings(int major, int minor, int patch) {
		this.major = major;
		this.minor = minor;
		this.patch = patch;
	}

	@Override
	public int getMajor() {
		return major;
	}

	@Override
	public int getMinor() {
		return minor;
	}

	@Override
	public int getPatch() {
		return patch;
	}

	@Override
	public @NotNull MappedClass getClass(@NotNull String name) throws ClassNotFoundException {
		return classes.computeIfAbsent(name, FakeMappedClass::new);
	}

	class FakeMappedClass implements MappedClass {

		public final @NotNull String key;

		public final Map<String, FakeMappedField> fields = new HashMap<>(4);
		public final List<FakeMappedMethod> methods = new ArrayList<>(4);

		public FakeMappedClass(@NotNull String key) {
			this.key = key;
		}

		@Override
		public @NotNull Type getArrayType() {
			return Object.class.arrayType();
		}

		@Override
		public @NotNull Class<?> getMappedClass() throws ClassNotFoundException {
			return Object.class; // placeholder because can be used for array creation
		}

		@Override
		public @NotNull MappedField getField(@NotNull String key) throws NoSuchFieldException {
			return fields.computeIfAbsent(key, FakeMappedField::new);
		}

		@Override
		public @NotNull MappedMethod getMethod(@NotNull String key, @NotNull Type... parameterTypes)
				throws NoSuchMethodException, ClassNotFoundException {
			for (var mappedMethod : methods) {
				if (mappedMethod.key.equals(key) && Arrays.equals(mappedMethod.parameterTypes, parameterTypes))
					return mappedMethod;
			}
			var mappedMethod = new FakeMappedMethod(key, parameterTypes);
			methods.add(mappedMethod);
			return mappedMethod;
		}

		@Override
		public @NotNull MappedConstructor getConstructor(@NotNull Type... parameterTypes)
				throws NoSuchMethodException, SecurityException, ClassNotFoundException {
			return new MappedConstructor() {
				@Override
				public Object newInstance(@Nullable Object... args)
						throws InstantiationException, IllegalAccessException, IllegalArgumentException,
						InvocationTargetException {
					return null;
				}

				@Override
				public Constructor<?> getMappedConstructor() {
					return null;
				}
			};
		}

		record FakeMappedField(@NotNull String key) implements MappedField {

			@Override
			public Field getMappedField() throws NoSuchFieldException, SecurityException, ClassNotFoundException {
				return null;
			}

			@Override
			public Object get(@Nullable Object instance) throws IllegalArgumentException, IllegalAccessException,
					NoSuchFieldException, SecurityException, ClassNotFoundException {
				return null;
			}

			@Override
			public void set(@Nullable Object instance, Object value) throws IllegalArgumentException, IllegalAccessException,
					NoSuchFieldException, SecurityException, ClassNotFoundException {}

		}

		record FakeMappedMethod(@NotNull String key, @NotNull Type[] parameterTypes) implements MappedMethod {

			@Override
			public Method getMappedMethod() throws NoSuchMethodException, SecurityException, ClassNotFoundException {
				return null;
			}

			@Override
			public Object invoke(@Nullable Object instance, @Nullable Object... args)
					throws IllegalAccessException, IllegalArgumentException, InvocationTargetException,
					NoSuchMethodException, SecurityException, ClassNotFoundException {
				return null;
			}

		}

	}

}
