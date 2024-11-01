package fr.skytasul.reflection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.lang.reflect.*;
import java.util.*;

public class FakeReflectionAccessor implements ReflectionAccessor {

	private final Map<String, FakeClass> classes = new HashMap<>();

	public Collection<FakeClass> classes() {
		return classes.values();
	}

	@Override
	public @NotNull FakeClass getClass(@NotNull String name) throws ClassNotFoundException {
		return classes.computeIfAbsent(name, FakeClass::new);
	}

	public record FakeClass(@NotNull String name, List<FakeField> fields, List<FakeMethod> methods)
			implements ClassAccessor {

		public FakeClass(@NotNull String name) {
			this(name, new ArrayList<>(), new ArrayList<>());
		}

		@Override
		public @NotNull String getTypeName() {
			return name;
		}

		@Override
		public @NotNull Type getArrayType() {
			return Object.class.arrayType();
		}

		@Override
		public @NotNull Class<?> getClassInstance() throws ClassNotFoundException {
			return Object.class; // placeholder because can be used for array creation
		}

		@Override
		public @NotNull FieldAccessor getField(@NotNull String original) throws NoSuchFieldException {
			for (var field : fields) {
				if (field.name.equals(original))
					return field;
			}
			var field = new FakeField(original);
			fields.add(field);
			return field;
		}

		@Override
		public @NotNull MethodAccessor getMethod(@NotNull String original, @NotNull Type... parameterTypes)
				throws NoSuchMethodException, ClassNotFoundException {
			for (var method : methods) {
				if (method.name.equals(original)
						&& ReflectionAccessor.areSameParameters(method.parameterTypes, parameterTypes))
					return method;
			}
			var method = new FakeMethod(original, parameterTypes);
			methods.add(method);
			return method;
		}

		@Override
		public @NotNull ConstructorAccessor getConstructor(@NotNull Type... parameterTypes)
				throws NoSuchMethodException, SecurityException, ClassNotFoundException {
			return new FakeConstructor();
		}

		public record FakeField(@NotNull String name) implements FieldAccessor {

			@Override
			public Field getFieldInstance() throws NoSuchFieldException, SecurityException, ClassNotFoundException {
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

		public record FakeMethod(@NotNull String name, @NotNull Type[] parameterTypes) implements MethodAccessor {

			@Override
			public Method getMethodInstance() throws NoSuchMethodException, SecurityException, ClassNotFoundException {
				return null;
			}

			@Override
			public Object invoke(@Nullable Object instance, @Nullable Object... args)
					throws IllegalAccessException, IllegalArgumentException, InvocationTargetException,
					NoSuchMethodException, SecurityException, ClassNotFoundException {
				return null;
			}

		}

		public class FakeConstructor implements ConstructorAccessor {

			@Override
			public Constructor<?> getConstructorInstance() {
				return null;
			}

			@Override
			public Object newInstance(@Nullable Object... args) throws InstantiationException, IllegalAccessException,
					IllegalArgumentException, InvocationTargetException {
				return null;
			}

		}

	}

}
