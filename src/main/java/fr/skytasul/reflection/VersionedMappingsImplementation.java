package fr.skytasul.reflection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

public class VersionedMappingsImplementation implements VersionedMappings {

	public final int major;
	public final int minor;
	public final int patch;

	public List<ClassHandle> classes;

	public VersionedMappingsImplementation(int major, int minor, int patch) {
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
	public @NotNull ClassHandle getClass(@NotNull String key) throws ClassNotFoundException {
		for (ClassHandle clazz : classes)
			if (clazz.key.equals(key))
				return clazz;
		throw new ClassNotFoundException(key);
	}

	// load classes first, then fields and methods
	public static class ClassHandle implements MappedClass {

		public final @NotNull String key, remapped;

		private @Nullable ClassHandleArray arrayType;

		private @Nullable Class<?> cachedClass;

		protected List<FieldHandle> fields;
		protected List<MethodHandle> methods;

		public ClassHandle(String key, String remapped) {
			this.key = key;
			this.remapped = remapped;
		}

		@Override
		public String getTypeName() {
			return key;
		}

		public @NotNull String getMappedName() {
			return remapped;
		}

		@Override
		public @NotNull Type getArrayType() {
			if (arrayType == null)
				arrayType = new ClassHandleArray();
			return arrayType;
		}

		public @NotNull List<@NotNull FieldHandle> getFields() {
			return fields;
		}

		@Override
		public @NotNull Class<?> getMappedClass() throws ClassNotFoundException {
			if (cachedClass == null)
				cachedClass = Class.forName(remapped);
			return cachedClass;
		}

		@Override
		public @NotNull FieldHandle getField(@NotNull String key) throws NoSuchFieldException {
			for (FieldHandle field : fields)
				if (field.key.equals(key))
					return field;
			throw new NoSuchFieldException(key);
		}

		@Override
		public @NotNull MethodHandle getMethod(@NotNull String key, @NotNull Type... parameterTypes)
				throws NoSuchMethodException {
			for (MethodHandle method : methods)
				if (method.key.equals(key) && Arrays.equals(method.parameterTypes, parameterTypes))
					return method;
			throw new NoSuchMethodException(key);
		}

		@Override
		public @NotNull Method getMappedMethod(@NotNull String key, @NotNull Type... parameterTypes)
				throws NoSuchMethodException, SecurityException, ClassNotFoundException {
			return getMethod(key, parameterTypes).getMappedMethod();
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
				return o.key.equals(key);
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

			public final @NotNull String key, remapped;

			private @Nullable Field cachedField;

			public FieldHandle(@NotNull String key, @NotNull String remapped) {
				this.key = key;
				this.remapped = remapped;
			}

			@Override
			public @NotNull Field getMappedField() throws NoSuchFieldException, SecurityException, ClassNotFoundException {
				if (cachedField == null) {
					cachedField = getMappedClass().getDeclaredField(remapped);
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

			public final @NotNull String key, remapped;
			public final @NotNull Type @NotNull [] parameterTypes;

			private @Nullable Method cachedMethod;

			public MethodHandle(@NotNull String key, @NotNull String remapped,
					@NotNull Type @NotNull [] parameterTypes) {
				this.key = key;
				this.remapped = remapped;
				this.parameterTypes = parameterTypes;
			}

			@Override
			public @NotNull Method getMappedMethod()
					throws NoSuchMethodException, SecurityException, ClassNotFoundException {
				if (cachedMethod == null) {
					cachedMethod = getMappedClass().getDeclaredMethod(remapped, getClassesFromHandles(parameterTypes));
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
