package fr.skytasul.reflection;

import org.jetbrains.annotations.NotNull;
import java.lang.reflect.Type;
import java.util.Collection;

public interface MappingsHolder {

	interface MappingObject {

		@NotNull
		String original();

		@NotNull
		String obfuscated();

	}

	Collection<? extends MappingClass> classes();

	interface MappingClass extends MappingObject {

		Collection<? extends MappingField> fields();

		Collection<? extends MappingMethod> methods();

		interface MappingField extends MappingObject {
		}

		interface MappingMethod extends MappingObject {

			@NotNull
			Type @NotNull [] getParameterTypes();

		}

	}

}
