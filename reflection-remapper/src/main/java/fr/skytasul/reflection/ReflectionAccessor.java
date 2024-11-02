package fr.skytasul.reflection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.lang.reflect.*;

public interface ReflectionAccessor {

	@NotNull
	ClassAccessor getClass(@NotNull String name) throws ClassNotFoundException;

	default Class<?> getClassInstance(@NotNull String name) throws ClassNotFoundException {
		return getClass(name).getClassInstance();
	}

	interface ClassAccessor extends Type {

		@Override
		@NotNull
		String getTypeName();

		@NotNull
		Type getArrayType();

		@NotNull
		Class<?> getClassInstance() throws ClassNotFoundException;

		@NotNull
		FieldAccessor getField(@NotNull String original) throws NoSuchFieldException;

		@NotNull
		default Field getFieldInstance(@NotNull String original)
				throws NoSuchFieldException, SecurityException, ClassNotFoundException {
			return getField(original).getFieldInstance();
		}

		@NotNull
		MethodAccessor getMethod(@NotNull String original, @NotNull Type... parameterTypes)
				throws NoSuchMethodException, ClassNotFoundException;

		@NotNull
		default Method getMethodInstance(@NotNull String original, @NotNull Type... parameterTypes)
				throws NoSuchMethodException, ClassNotFoundException {
			return getMethod(original, parameterTypes).getMethodInstance();
		}

		@NotNull
		ConstructorAccessor getConstructor(@NotNull Type... parameterTypes)
				throws NoSuchMethodException, SecurityException, ClassNotFoundException;

		@NotNull
		default Constructor<?> getConstructorInstance(@NotNull Type... parameterTypes)
				throws NoSuchMethodException, SecurityException, ClassNotFoundException {
			return getConstructor(parameterTypes).getConstructorInstance();
		}

		interface FieldAccessor {

			Field getFieldInstance() throws NoSuchFieldException, SecurityException, ClassNotFoundException;

			Object get(@Nullable Object instance) throws IllegalArgumentException, IllegalAccessException,
					NoSuchFieldException, SecurityException, ClassNotFoundException;

			void set(@Nullable Object instance, Object value) throws IllegalArgumentException, IllegalAccessException,
					NoSuchFieldException, SecurityException, ClassNotFoundException;

		}

		interface MethodAccessor {

			Method getMethodInstance() throws NoSuchMethodException, SecurityException, ClassNotFoundException;

			Object invoke(@Nullable Object instance, @Nullable Object... args)
					throws IllegalAccessException, IllegalArgumentException, InvocationTargetException,
					NoSuchMethodException, SecurityException, ClassNotFoundException;

		}

		interface ConstructorAccessor {

			Constructor<?> getConstructorInstance();

			Object newInstance(@Nullable Object... args) throws InstantiationException, IllegalAccessException,
					IllegalArgumentException, InvocationTargetException;

		}

	}

	public static boolean areSameParameters(@NotNull Type @NotNull [] types1, @NotNull Type @NotNull [] types2) {
		if (types1.length != types2.length)
			return false;
		for (int i = 0; i < types1.length; i++)
			if (!types1[i].getTypeName().equals(types2[i].getTypeName()))
				return false;
		return true;
	}

}
