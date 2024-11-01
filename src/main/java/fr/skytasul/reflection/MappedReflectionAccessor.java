package fr.skytasul.reflection;

import fr.skytasul.reflection.mappings.Mappings;
import fr.skytasul.reflection.mappings.Mappings.ClassMapping;
import fr.skytasul.reflection.mappings.Mappings.ClassMapping.ClassArrayType;
import fr.skytasul.reflection.mappings.Mappings.ClassMapping.FieldMapping;
import fr.skytasul.reflection.mappings.Mappings.ClassMapping.MethodMapping;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MappedReflectionAccessor implements ReflectionAccessor {

	private final @NotNull Mappings mappings;

	private final @NotNull Map<String, ClassHandle> classes = new HashMap<>();

	public MappedReflectionAccessor(@NotNull Mappings mappings) {
		this.mappings = mappings;

		for (var classMapping : mappings.getClasses()) {
			classes.put(classMapping.getOriginalName(), new ClassHandle(classMapping));
		}
	}

	@Override
	public @NotNull ClassHandle getClass(@NotNull String original) throws ClassNotFoundException {
		if (classes.containsKey(original))
			return classes.get(original);

		throw new ClassNotFoundException(original);
	}

	// load classes first, then fields and methods
	private class ClassHandle implements ClassAccessor {

		private final @NotNull ClassMapping mapping;

		private final List<FieldHandle> fields;
		private final List<MethodHandle> methods;

		private @Nullable ClassArrayType cachedArrayType;
		private @Nullable Class<?> cachedClass;

		public ClassHandle(@NotNull ClassMapping mapping) {
			this.mapping = mapping;

			this.fields = mapping.getFields().stream().map(FieldHandle::new).toList();
			this.methods = mapping.getMethods().stream().map(MethodHandle::new).toList();
		}

		@Override
		public @NotNull String getTypeName() {
			return mapping.getOriginalName();
		}

		@Override
		public @NotNull Type getArrayType() {
			if (cachedArrayType == null)
				cachedArrayType = mapping.getArrayType();
			return cachedArrayType;
		}

		@Override
		public @NotNull Class<?> getClassInstance() throws ClassNotFoundException {
			if (cachedClass == null)
				cachedClass = Class.forName(mapping.getMappedName());
			return cachedClass;
		}

		@Override
		public @NotNull FieldHandle getField(@NotNull String original) throws NoSuchFieldException {
			for (FieldHandle field : fields)
				if (field.mapping.getOriginalName().equals(original))
					return field;
			throw new NoSuchFieldException(original);
		}

		@Override
		public @NotNull MethodHandle getMethod(@NotNull String original, @NotNull Type... parameterTypes)
				throws NoSuchMethodException {
			for (MethodHandle method : methods)
				if (method.mapping.getOriginalName().equals(original) && method.mapping.isSameParameters(parameterTypes))
					return method;
			throw new NoSuchMethodException(Mappings.getStringForMethod(original, parameterTypes));
		}

		@Override
		public @NotNull ConstructorAccessor getConstructor(@NotNull Type... parameterTypes)
				throws NoSuchMethodException, SecurityException, ClassNotFoundException {
			var constructor = getClassInstance().getDeclaredConstructor(getClassesFromMappingTypes(parameterTypes));
			return new TransparentReflectionAccessor.TransparentConstructor(constructor);
		}

		private class FieldHandle implements FieldAccessor {

			private final @NotNull FieldMapping mapping;

			private @Nullable Field cachedField;

			private FieldHandle(@NotNull FieldMapping mapping) {
				this.mapping = mapping;
			}

			@Override
			public @NotNull Field getFieldInstance() throws NoSuchFieldException, SecurityException, ClassNotFoundException {
				if (cachedField == null) {
					cachedField = getClassInstance().getDeclaredField(mapping.getMappedName());
					cachedField.setAccessible(true);
				}
				return cachedField;
			}

			@Override
			public Object get(@Nullable Object instance) throws IllegalArgumentException, IllegalAccessException,
					NoSuchFieldException, SecurityException, ClassNotFoundException {
				return getFieldInstance().get(instance);
			}

			@Override
			public void set(@Nullable Object instance, Object value)
					throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException,
					ClassNotFoundException {
				getFieldInstance().set(instance, value);
			}

		}

		private class MethodHandle implements MethodAccessor {

			private final @NotNull MethodMapping mapping;

			private @Nullable Method cachedMethod;

			private MethodHandle(@NotNull MethodMapping mapping) {
				this.mapping = mapping;
			}

			@Override
			public @NotNull Method getMethodInstance()
					throws NoSuchMethodException, SecurityException, ClassNotFoundException {
				if (cachedMethod == null) {
					cachedMethod = getClassInstance().getDeclaredMethod(mapping.getMappedName(),
							getClassesFromMappingTypes(mapping.getParameterTypes()));
					cachedMethod.setAccessible(true);
				}
				return cachedMethod;
			}

			@Override
			public Object invoke(@Nullable Object instance, @Nullable Object... args)
					throws IllegalAccessException, IllegalArgumentException, InvocationTargetException,
					NoSuchMethodException, SecurityException, ClassNotFoundException {
				return getMethodInstance().invoke(instance, args);
			}

		}

	}

	protected Class<?>[] getClassesFromMappingTypes(Type[] handles) throws ClassNotFoundException {
		Class<?>[] array = new Class<?>[handles.length];
		for (int i = 0; i < handles.length; i++) {
			Class<?> type;
			if (handles[i] instanceof Class<?> clazz)
				type = clazz;
			else if (handles[i] instanceof ClassMapping mapping)
				type = getClass(mapping.getOriginalName()).getClassInstance();
			else if (handles[i] instanceof ClassArrayType mappingArray)
				type = getClassInstance(mappingArray.componentMapping().getTypeName()).arrayType();
			else
				throw new IllegalArgumentException();
			array[i] = type;
		}
		return array;
	}

}
