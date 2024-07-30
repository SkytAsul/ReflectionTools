package fr.skytasul.reflection;

import fr.skytasul.reflection.VersionedMappings.MappedClass.MappedConstructor;
import fr.skytasul.reflection.VersionedMappings.MappedClass.MappedField;
import fr.skytasul.reflection.VersionedMappings.MappedClass.MappedMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.lang.reflect.*;
import java.util.*;

/**
 * "Mappings" whoose originals are directly remapped to real Java names, without obfuscation.
 */
public class VersionedMappingsTransparent implements VersionedMappings {

	private final @NotNull Version version;
	private final Map<String, MappedClassTransparent> classes = new HashMap<>();

	public VersionedMappingsTransparent(@NotNull Version version) {
		this.version = version;
	}

	@Override
	public @NotNull Version getVersion() {
		return version;
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

	@Override
	public Collection<? extends MappedClass> getClasses() {
		return classes.values();
	}

	private class MappedClassTransparent implements MappedClass {

		private final @NotNull Class<?> clazz;

		private final List<TransparentField> fields = new ArrayList<>();
		private final List<TransparentMethod> methods = new ArrayList<>();

		protected MappedClassTransparent(@NotNull Class<?> clazz) {
			this.clazz = clazz;
		}

		@Override
		public @NotNull String getOriginalName() {
			return clazz.getName();
		}

		@Override
		public @NotNull String getObfuscatedName() {
			return clazz.getName();
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
		public Collection<? extends MappedField> getFields() {
			return fields;
		}

		@Override
		public Collection<? extends MappedMethod> getMethods() {
			return methods;
		}

		@Override
		public @NotNull MappedField getField(@NotNull String original) throws NoSuchFieldException {
			for (var field : fields)
				if (field.getOriginalName().equals(original))
					return field;

			var field = new TransparentField(clazz.getDeclaredField(original));
			fields.add(field);
			return field;
		}

		@Override
		public @NotNull MappedMethod getMethod(@NotNull String original, @NotNull Type... parameterTypes)
				throws NoSuchMethodException, ClassNotFoundException {
			for (var method : methods)
				if (method.getOriginalName().equals(original) && Arrays.equals(method.getParameterTypes(), parameterTypes))
					return method;

			var method = new TransparentMethod(clazz.getDeclaredMethod(original,
					VersionedMappingsImplementation.getClassesFromHandles(parameterTypes)));
			methods.add(method);
			return method;
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
		public @NotNull String getOriginalName() {
			return field.getName();
		}

		@Override
		public @NotNull String getObfuscatedName() {
			return field.getName();
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
		public @NotNull String getOriginalName() {
			return method.getName();
		}

		@Override
		public @NotNull String getObfuscatedName() {
			return method.getName();
		}

		@Override
		public @NotNull Type @NotNull [] getParameterTypes() {
			return method.getParameterTypes();
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
