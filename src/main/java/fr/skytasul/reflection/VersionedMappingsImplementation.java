package fr.skytasul.reflection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class VersionedMappingsImplementation implements VersionedMappings {

	private final @NotNull Version version;
	public List<ClassHandle> classes;

	public VersionedMappingsImplementation(@NotNull Version version) {
		this.version = version;
	}

	@Override
	public @NotNull Version getVersion() {
		return version;
	}

	@Override
	public Collection<? extends MappedClass> getClasses() {
		return classes;
	}

	@Override
	public @NotNull ClassHandle getClass(@NotNull String original) throws ClassNotFoundException {
		for (ClassHandle clazz : classes)
			if (clazz.original.equals(original))
				return clazz;
		throw new ClassNotFoundException(original);
	}

	// load classes first, then fields and methods
	public static class ClassHandle implements MappedClass {

		public final @NotNull String original, obfuscated;

		private @Nullable ClassHandleArray arrayType;

		private @Nullable Class<?> cachedClass;

		public List<FieldHandle> fields;
		public List<MethodHandle> methods;

		public ClassHandle(String original, String obfuscated) {
			this.original = original;
			this.obfuscated = obfuscated;
		}

		@Override
		public @NotNull String getOriginalName() {
			return original;
		}

		@Override
		public @NotNull String getObfuscatedName() {
			return obfuscated;
		}

		@Override
		public String getTypeName() {
			return original;
		}

		@Override
		public @NotNull Type getArrayType() {
			if (arrayType == null)
				arrayType = new ClassHandleArray();
			return arrayType;
		}

		@Override
		public List<FieldHandle> getFields() {
			return fields;
		}

		@Override
		public List<MethodHandle> getMethods() {
			return methods;
		}

		@Override
		public @NotNull Class<?> getMappedClass() throws ClassNotFoundException {
			if (cachedClass == null)
				cachedClass = Class.forName(obfuscated);
			return cachedClass;
		}

		@Override
		public @NotNull FieldHandle getField(@NotNull String original) throws NoSuchFieldException {
			for (FieldHandle field : fields)
				if (field.original.equals(original))
					return field;
			throw new NoSuchFieldException(original);
		}

		@Override
		public @NotNull MethodHandle getMethod(@NotNull String original, @NotNull Type... parameterTypes)
				throws NoSuchMethodException {
			for (MethodHandle method : methods)
				if (method.original.equals(original) && Arrays.equals(method.parameterTypes, parameterTypes))
					return method;
			throw new NoSuchMethodException(original);
		}

		@Override
		public @NotNull Method getMappedMethod(@NotNull String original, @NotNull Type... parameterTypes)
				throws NoSuchMethodException, SecurityException, ClassNotFoundException {
			return getMethod(original, parameterTypes).getMappedMethod();
		}

		@Override
		public @NotNull MappedConstructor getConstructor(@NotNull Type... parameterTypes)
				throws NoSuchMethodException, SecurityException, ClassNotFoundException {
			var constructor = getMappedClass().getDeclaredConstructor(getClassesFromHandles(parameterTypes));
			return new VersionedMappingsTransparent.TransparentConstructor(constructor);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof ClassHandle o)
				return o.original.equals(original);
			return false;
		}

		private class ClassHandleArray implements Type {

			public Class<?> getMappedClass() throws ClassNotFoundException {
				return ClassHandle.this.getMappedClass().arrayType();
			}

			@Override
			public String getTypeName() {
				return ClassHandle.this.getTypeName() + "[]";
			}

		}

		public class FieldHandle implements MappedField {

			public final @NotNull String original, obfuscated;

			private @Nullable Field cachedField;

			public FieldHandle(@NotNull String original, @NotNull String obfuscated) {
				this.original = original;
				this.obfuscated = obfuscated;
			}

			@Override
			public @NotNull String getOriginalName() {
				return original;
			}

			@Override
			public @NotNull String getObfuscatedName() {
				return obfuscated;
			}

			@Override
			public @NotNull Field getMappedField() throws NoSuchFieldException, SecurityException, ClassNotFoundException {
				if (cachedField == null) {
					cachedField = getMappedClass().getDeclaredField(obfuscated);
					cachedField.setAccessible(true);
				}
				return cachedField;
			}

			@Override
			public Object get(@Nullable Object instance) throws IllegalArgumentException, IllegalAccessException,
					NoSuchFieldException, SecurityException, ClassNotFoundException {
				return getMappedField().get(instance);
			}

			@Override
			public void set(@Nullable Object instance, Object value)
					throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException,
					ClassNotFoundException {
				getMappedField().set(instance, value);
			}

		}

		public class MethodHandle implements MappedMethod {

			public final @NotNull String original, obfuscated;
			public final @NotNull Type @NotNull [] parameterTypes;

			private @Nullable Method cachedMethod;

			public MethodHandle(@NotNull String original, @NotNull String obfuscated,
					@NotNull Type @NotNull [] parameterTypes) {
				this.original = original;
				this.obfuscated = obfuscated;
				this.parameterTypes = parameterTypes;
			}

			@Override
			public @NotNull String getOriginalName() {
				return original;
			}

			@Override
			public @NotNull String getObfuscatedName() {
				return obfuscated;
			}

			@Override
			public @NotNull Type @NotNull [] getParameterTypes() {
				return null;
			}

			@Override
			public @NotNull Method getMappedMethod()
					throws NoSuchMethodException, SecurityException, ClassNotFoundException {
				if (cachedMethod == null) {
					cachedMethod = getMappedClass().getDeclaredMethod(obfuscated, getClassesFromHandles(parameterTypes));
					cachedMethod.setAccessible(true);
				}
				return cachedMethod;
			}

			@Override
			public Object invoke(@Nullable Object instance, @Nullable Object... args)
					throws IllegalAccessException, IllegalArgumentException, InvocationTargetException,
					NoSuchMethodException, SecurityException, ClassNotFoundException {
				return getMappedMethod().invoke(instance, args);
			}

		}

	}

	protected static Class<?>[] getClassesFromHandles(Type[] handles) throws ClassNotFoundException {
		Class<?>[] array = new Class<?>[handles.length];
		for (int i = 0; i < handles.length; i++) {
			Class<?> type;
			if (handles[i] instanceof Class<?> clazz)
				type = clazz;
			else if (handles[i] instanceof MappedClass mapped)
				type = mapped.getMappedClass();
			else if (handles[i] instanceof ClassHandle.ClassHandleArray handleArray)
				type = handleArray.getMappedClass();
			else
				throw new IllegalArgumentException();
			array[i] = type;
		}
		return array;
	}

}
