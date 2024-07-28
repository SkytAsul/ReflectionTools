package fr.skytasul.reflection;

import fr.skytasul.reflection.VersionedMappings.MappedClass.MappedConstructor;
import fr.skytasul.reflection.VersionedMappings.MappedClass.MappedField;
import fr.skytasul.reflection.VersionedMappings.MappedClass.MappedMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * "Mappings" whoose keys are directly remapped to real Java names, without obfuscation.
 * <p>
 * Wrapped reflection methods such as {@link MappedField#get(Object)} will return <code>null</code>
 * if the underlying fields do not exist (useful for generating mappings on different software
 * versions just by following code path).
 */
public class VersionedMappingsTransparent implements VersionedMappings {

	private final int major;
	private final int minor;
	private final int patch;

	private final Map<String, MappedClassTransparent> classes = new HashMap<>();

	public VersionedMappingsTransparent(int major, int minor, int patch) {
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
		var clazz = classes.get(name);
		if (clazz == null) {
			clazz = new MappedClassTransparent(Class.forName(name));
			classes.put(name, clazz);
		}
		return clazz;
	}

	private class MappedClassTransparent implements MappedClass {

		private final @NotNull Class<?> clazz;

		private final List<Field> fields = new ArrayList<>();
		private final List<Method> methods = new ArrayList<>();

		protected MappedClassTransparent(@NotNull Class<?> clazz) {
			this.clazz = clazz;
		}

		@Override
		public @NotNull Type getArrayType() {
			return clazz.arrayType();
		}

		@Override
		public @NotNull Class<?> getMappedClass() throws ClassNotFoundException {
			return clazz;
		}

		@Override
		public @NotNull MappedField getField(@NotNull String key) throws NoSuchFieldException {
			var field = clazz.getDeclaredField(key);
			fields.add(field);
			return new TransparentField(field);
		}

		@Override
		public @NotNull MappedMethod getMethod(@NotNull String key, @NotNull Type... parameterTypes)
				throws NoSuchMethodException, ClassNotFoundException {
			var method = clazz.getDeclaredMethod(key, VersionedMappingsImplementation.getClassesFromHandles(parameterTypes));
			methods.add(method);
			return new TransparentMethod(method);
		}

		@Override
		public @NotNull MappedConstructor getConstructor(@NotNull Type... parameterTypes)
				throws NoSuchMethodException, SecurityException, ClassNotFoundException {
			var constructor =
					clazz.getDeclaredConstructor(VersionedMappingsImplementation.getClassesFromHandles(parameterTypes));
			return new TransparentConstructor(constructor);
		}

	}

	protected static class TransparentField implements MappedField {

		private final @NotNull Field field;

		public TransparentField(@NotNull Field field) {
			this.field = field;
		}

		@Override
		public Field getMappedField() {
			return field;
		}

		@Override
		public Object get(@Nullable Object instance) throws IllegalArgumentException, IllegalAccessException {
			field.setAccessible(true);
			return field.get(instance);
		}

		@Override
		public void set(@Nullable Object instance, Object value) throws IllegalArgumentException, IllegalAccessException {
			field.setAccessible(true);
			field.set(instance, value);
		}

	}

	protected static class TransparentMethod implements MappedMethod {

		private final @NotNull Method method;

		public TransparentMethod(@NotNull Method method) {
			this.method = method;
		}

		@Override
		public Method getMappedMethod() {
			return method;
		}

		@Override
		public Object invoke(@Nullable Object instance, @Nullable Object... args)
				throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
			method.setAccessible(true);
			return method.invoke(instance, args);
		}

	}

	protected static class TransparentConstructor implements MappedConstructor {

		private final @NotNull Constructor<?> constructor;

		public TransparentConstructor(@NotNull Constructor<?> constructor) {
			this.constructor = constructor;
		}

		@Override
		public Constructor<?> getMappedConstructor() {
			return constructor;
		}

		@Override
		public Object newInstance(@Nullable Object... args)
				throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
			return constructor.newInstance(args);
		}

	}

}
