package fr.skytasul.reflection;

import fr.skytasul.reflection.ReflectionAccessor.ClassAccessor.ConstructorAccessor;
import fr.skytasul.reflection.ReflectionAccessor.ClassAccessor.FieldAccessor;
import fr.skytasul.reflection.ReflectionAccessor.ClassAccessor.MethodAccessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reflection accessor where originals are directly remapped to real Java names, without
 * obfuscation.
 */
public class TransparentReflectionAccessor implements ReflectionAccessor {

	private final Map<String, MappedClassTransparent> classes = new HashMap<>();

	@Override
	public @NotNull ClassAccessor getClass(@NotNull String name) throws ClassNotFoundException {
		var clazz = classes.get(name);
		if (clazz == null) {
			clazz = new MappedClassTransparent(Class.forName(name));
			classes.put(name, clazz);
		}
		return clazz;
	}

	private class MappedClassTransparent implements ClassAccessor {

		private final @NotNull Class<?> clazz;

		private final List<TransparentField> fields = new ArrayList<>();
		private final List<TransparentMethod> methods = new ArrayList<>();

		protected MappedClassTransparent(@NotNull Class<?> clazz) {
			this.clazz = clazz;
		}

		@Override
		public @NotNull String getTypeName() {
			return clazz.getTypeName();
		}

		@Override
		public @NotNull Type getArrayType() {
			return clazz.arrayType();
		}

		@Override
		public @NotNull Class<?> getClassInstance() throws ClassNotFoundException {
			return clazz;
		}

		@Override
		public @NotNull FieldAccessor getField(@NotNull String original) throws NoSuchFieldException {
			for (var field : fields)
				if (field.field.getName().equals(original))
					return field;

			var field = new TransparentField(clazz.getDeclaredField(original));
			fields.add(field);
			return field;
		}

		@Override
		public @NotNull MethodAccessor getMethod(@NotNull String original, @NotNull Type... parameterTypes)
				throws NoSuchMethodException, ClassNotFoundException {
			for (var method : methods)
				if (method.getMethodInstance().getName().equals(original)
						&& ReflectionAccessor.areSameParameters(parameterTypes,
								method.getMethodInstance().getParameterTypes()))
					return method;

			var method = new TransparentMethod(clazz.getDeclaredMethod(original, getClassesFromUserTypes(parameterTypes)));
			methods.add(method);
			return method;
		}

		@Override
		public @NotNull ConstructorAccessor getConstructor(@NotNull Type... parameterTypes)
				throws NoSuchMethodException, SecurityException, ClassNotFoundException {
			var constructor = clazz.getDeclaredConstructor(getClassesFromUserTypes(parameterTypes));
			return new TransparentConstructor(constructor);
		}

	}

	protected static class TransparentField implements FieldAccessor {

		private final @NotNull Field field;

		public TransparentField(@NotNull Field field) {
			this.field = field;
			field.setAccessible(true);
		}

		@Override
		public Field getFieldInstance() {
			return field;
		}

		@Override
		public Object get(@Nullable Object instance) throws IllegalArgumentException, IllegalAccessException {
			return field.get(instance);
		}

		@Override
		public void set(@Nullable Object instance, Object value) throws IllegalArgumentException, IllegalAccessException {
			field.set(instance, value);
		}

	}

	protected static class TransparentMethod implements MethodAccessor {

		private final @NotNull Method method;

		public TransparentMethod(@NotNull Method method) {
			this.method = method;
			method.setAccessible(true);
		}

		@Override
		public Method getMethodInstance() {
			return method;
		}

		@Override
		public Object invoke(@Nullable Object instance, @Nullable Object... args)
				throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
			return method.invoke(instance, args);
		}

	}

	protected static class TransparentConstructor implements ConstructorAccessor {

		private final @NotNull Constructor<?> constructor;

		public TransparentConstructor(@NotNull Constructor<?> constructor) {
			this.constructor = constructor;
			constructor.setAccessible(true);
		}

		@Override
		public Constructor<?> getConstructorInstance() {
			return constructor;
		}

		@Override
		public Object newInstance(@Nullable Object... args)
				throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
			return constructor.newInstance(args);
		}

	}

	protected Class<?>[] getClassesFromUserTypes(Type[] handles) throws ClassNotFoundException {
		Class<?>[] array = new Class<?>[handles.length];
		for (int i = 0; i < handles.length; i++) {
			Class<?> type;
			if (handles[i] instanceof Class<?> clazz)
				type = clazz;
			else if (handles[i] instanceof MappedClassTransparent mapped)
				type = mapped.getClassInstance();
			else
				throw new IllegalArgumentException();
			array[i] = type;
		}
		return array;
	}

}
