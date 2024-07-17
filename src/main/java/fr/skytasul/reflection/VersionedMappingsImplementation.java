package fr.skytasul.reflection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

public class VersionedMappingsImplementation implements VersionedMappings {

	public final int major;
	public final int minor;
	public final int patch;

	protected List<ClassHandle> classes;

	protected VersionedMappingsImplementation(int major, int minor, int patch) {
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
	public boolean isVersion(int major, int minor, int patch) {
		return this.major == major && this.minor == minor && this.patch == patch;
	}

	@Override
	public boolean isAfter(int major, int minor, int patch) {
		if (this.major > major)
			return true;
		if (this.major < major)
			return false;

		if (this.minor > minor)
			return true;
		if (this.minor < minor)
			return false;

		return this.patch >= patch;
	}

	@Override
	public @NotNull ClassHandle getClass(@NotNull String key) throws ClassNotFoundException {
		for (ClassHandle clazz : classes)
			if (clazz.key.equals(key))
				return clazz;
		throw new ClassNotFoundException(key);
	}

	// load classes first, then fields and methods
	public class ClassHandle implements VersionedMappings.MappedClass {

		public final @NotNull String key, remapped;

		private @Nullable ClassHandleArray arrayType;

		private @Nullable Class<?> cachedClass;

		protected List<FieldHandle> fields;
		protected List<MethodHandle> methods;

		protected ClassHandle(String key, String remapped) {
			this.key = key;
			this.remapped = remapped;
		}

		@Override
		public String getTypeName() {
			return key;
		}

		@Override
		public @NotNull Type getArrayType() {
			if (arrayType == null)
				arrayType = new ClassHandleArray();
			return arrayType;
		}

		@Override
		public @NotNull Class<?> getMappedClass() throws ClassNotFoundException {
			if (cachedClass == null)
				cachedClass = Class.forName(remapped);
			return cachedClass;
		}

		public @NotNull FieldHandle getField(@NotNull String key) throws NoSuchFieldException {
			for (FieldHandle field : fields)
				if (field.key.equals(key))
					return field;
			throw new NoSuchFieldException(key);
		}

		@Override
		public @NotNull Field getMappedField(@NotNull String key)
				throws NoSuchFieldException, SecurityException, ClassNotFoundException {
			return getField(key).getMappedField();
		}

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

		public class FieldHandle {

			public final @NotNull String key, remapped;

			private @Nullable Field cachedField;

			protected FieldHandle(@NotNull String key, @NotNull String remapped) {
				this.key = key;
				this.remapped = remapped;
			}

			public @NotNull Field getMappedField() throws NoSuchFieldException, SecurityException, ClassNotFoundException {
				if (cachedField == null) {
					cachedField = getMappedClass().getDeclaredField(remapped);
					cachedField.setAccessible(true);
				}
				return cachedField;
			}

		}

		public class MethodHandle {

			public final @NotNull String key, remapped;
			public final @NotNull Type @NotNull [] parameterTypes;

			private @Nullable Method cachedMethod;

			protected MethodHandle(@NotNull String key, @NotNull String remapped,
					@NotNull Type @NotNull [] parameterTypes) {
				this.key = key;
				this.remapped = remapped;
				this.parameterTypes = parameterTypes;
			}

			public @NotNull Method getMappedMethod()
					throws NoSuchMethodException, SecurityException, ClassNotFoundException {
				if (cachedMethod == null) {
					cachedMethod = getMappedClass().getDeclaredMethod(remapped, getClassesFromHandles(parameterTypes));
					cachedMethod.setAccessible(true);
				}
				return cachedMethod;
			}

		}

		private static Class<?>[] getClassesFromHandles(Type[] handles) throws ClassNotFoundException {
			Class<?>[] array = new Class<?>[handles.length];
			for (int i = 0; i < handles.length; i++) {
				Class<?> type;
				if (handles[i] instanceof Class<?> clazz)
					type = clazz;
				else if (handles[i] instanceof ClassHandle handle)
					type = handle.getMappedClass();
				else if (handles[i] instanceof ClassHandleArray handleArray)
					type = handleArray.getMappedClass();
				else
					throw new IllegalArgumentException();
				array[i] = type;
			}
			return array;
		}

	}

}
