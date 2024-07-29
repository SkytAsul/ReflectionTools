package fr.skytasul.reflection.shrieker;

import fr.skytasul.reflection.VersionedMappings;
import fr.skytasul.reflection.VersionedMappings.MappedClass.MappedField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.lang.reflect.*;
import java.util.*;

/**
 * Wrapped reflection methods such as {@link MappedField#get(Object)} will return <code>null</code>,
 * which is useful for generating mappings on different software versions just by following code
 * path).
 */
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

	@Override
	public Collection<? extends MappedClass> getClasses() {
		return classes.values();
	}

	class FakeMappedClass implements MappedClass {

		public final @NotNull String original;

		public final Map<String, FakeMappedField> fields = new HashMap<>(4);
		public final List<FakeMappedMethod> methods = new ArrayList<>(4);

		public FakeMappedClass(@NotNull String original) {
			this.original = original;
		}

		@Override
		public @NotNull String getOriginalName() {
			return original;
		}

		@Override
		public @NotNull String getObfuscatedName() {
			throw new UnsupportedOperationException();
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
		public @NotNull MappedField getField(@NotNull String original) throws NoSuchFieldException {
			return fields.computeIfAbsent(original, FakeMappedField::new);
		}

		@Override
		public Collection<? extends MappedField> getFields() {
			return fields.values();
		}

		@Override
		public @NotNull MappedMethod getMethod(@NotNull String original, @NotNull Type... parameterTypes)
				throws NoSuchMethodException, ClassNotFoundException {
			for (var mappedMethod : methods) {
				if (mappedMethod.original.equals(original) && Arrays.equals(mappedMethod.parameterTypes, parameterTypes))
					return mappedMethod;
			}
			var mappedMethod = new FakeMappedMethod(original, parameterTypes);
			methods.add(mappedMethod);
			return mappedMethod;
		}

		@Override
		public Collection<? extends MappedMethod> getMethods() {
			return methods;
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

		record FakeMappedField(@NotNull String original) implements MappedField {

			@Override
			public @NotNull String getOriginalName() {
				return original;
			}

			@Override
			public @NotNull String getObfuscatedName() {
				throw new UnsupportedOperationException();
			}

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

		record FakeMappedMethod(@NotNull String original, @NotNull Type[] parameterTypes) implements MappedMethod {

			@Override
			public @NotNull String getOriginalName() {
				return original;
			}

			@Override
			public @NotNull String getObfuscatedName() {
				throw new UnsupportedOperationException();
			}

			@Override
			public @NotNull Type @NotNull [] getParameterTypes() {
				return parameterTypes;
			}

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
