package fr.skytasul.reflection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

public class VersionedMappings {

	public final int major;
	public final int minor;
	public final int patch;

	protected List<ClassHandle> classes;

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

	public ClassHandle getClass(@NotNull String key) throws ClassNotFoundException {
		for (ClassHandle clazz : classes)
			if (clazz.key.equals(key))
				return clazz;
		throw new ClassNotFoundException(key);
	}

	// load classes first, then fields and methods
	public class ClassHandle implements Type {

		public final @NotNull String key, remapped;

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

		public @NotNull MethodHandle getMethod(@NotNull String key, @NotNull ClassHandle... parameterTypes)
				throws NoSuchMethodException {
			for (MethodHandle method : methods)
				if (method.key.equals(key) && Arrays.equals(method.parameterTypes, parameterTypes))
					return method;
			throw new NoSuchMethodException(key);
		}

		public @NotNull MethodHandle getMethod(@NotNull String key, @NotNull Class<?>... parameterTypes)
				throws NoSuchMethodException {
			return getMethod(key, getHandlesFromClasses(parameterTypes));
		}

		public @NotNull MethodHandle getMethod(@NotNull String key) throws NoSuchMethodException {
			// to disambiguate
			return getMethod(key, new Class[0]);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Type t)
				return t.getTypeName().equals(key);
			return false;
		}

		public class FieldHandle {

			public final @NotNull String key, remapped;

			private @Nullable Field cachedField;

			protected FieldHandle(@NotNull String key, @NotNull String remapped) {
				this.key = key;
				this.remapped = remapped;
			}

			public @NotNull Field getMappedField() throws ReflectiveOperationException {
				if (cachedField == null) {
					cachedField = getMappedClass().getDeclaredField(remapped);
					cachedField.setAccessible(true);
				}
				return cachedField;
			}

		}

		public class MethodHandle {

			public final @NotNull String key, remapped;
			public final @NotNull ClassHandle @NotNull [] parameterTypes;

			private @Nullable Method cachedMethod;

			protected MethodHandle(@NotNull String key, @NotNull String remapped,
					@NotNull ClassHandle @NotNull [] parameterTypes) {
				this.key = key;
				this.remapped = remapped;
				this.parameterTypes = parameterTypes;
			}

			protected @NotNull Method getMappedMethod() throws ReflectiveOperationException {
				if (cachedMethod == null) {
					cachedMethod = getMappedClass().getDeclaredMethod(remapped, getClassesFromHandles(parameterTypes));
					cachedMethod.setAccessible(true);
				}
				return cachedMethod;
			}

		}

		public static ClassHandle of(Class<?> existingClass) {
			return new ClassHandle(existingClass.getName(), existingClass.getName());
		}

		private static Class<?>[] getClassesFromHandles(ClassHandle[] handles) throws ClassNotFoundException {
			Class<?>[] array = new Class<?>[handles.length];
			for (int i = 0; i < handles.length; i++) {
				array[i] = handles[i].getMappedClass();
			}
			return array;
		}

		private static ClassHandle[] getHandlesFromClasses(Class<?>[] classes) {
			ClassHandle[] array = new ClassHandle[classes.length];
			for (int i = 0; i < classes.length; i++) {
				array[i] = ClassHandle.of(classes[i]);
			}
			return array;
		}

	}

}
