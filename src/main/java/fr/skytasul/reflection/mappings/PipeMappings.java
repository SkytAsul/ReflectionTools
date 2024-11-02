package fr.skytasul.reflection.mappings;

import org.jetbrains.annotations.NotNull;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Stream;

/**
 * i.e.:
 *
 * <pre>
 * from contains mapping:
 *   some.package.class -> a:
 *     someField -> x
 *     someMethod(some.package.class) -> z
 *
 * to contains mapping:
 *   a -> another.classname
 *     x -> aField
 *     z(a) -> aMethod
 *     unmappedField -> mappedField
 *   //some.unmapped.class -> remapped.class
 *
 * this pipe contains mapping:
 *   some.package.class -> another.classname:
 *     someField -> aField
 *     someMethod(some.package.class) -> aMethod
 *     unmappedField -> mappedField
 *   //some.unmapped.class -> remapped.class
 * </pre>
 */
public class PipeMappings implements Mappings {

	private final @NotNull Mappings from;
	private final @NotNull Mappings to;

	private final Map<String, PipeClass> classes = new HashMap<>();

	public PipeMappings(@NotNull Mappings from, @NotNull Mappings to) {
		this.from = from;
		this.to = to;

		for (var fromClass : from.getClasses()) {
			var pipeClass = new PipeClass(fromClass.getOriginalName(), fromClass.getMappedName());

			pipeClass.fields.addAll(fromClass.getFields().stream()
					.map(field -> pipeClass.new PipeField(field.getOriginalName(), field.getMappedName()))
					.toList());

			pipeClass.methods.addAll(fromClass.getMethods().stream()
					.map(method -> pipeClass.new PipeMethod(method.getOriginalName(), method.getMappedName(),
							method.getParameterTypes()))
					.toList());

			classes.put(fromClass.getMappedName(), pipeClass);
		}

		for (var toClass : to.getClasses()) {
			var pipeClass = classes.get(toClass.getOriginalName());
			if (pipeClass == null)
				throw new IllegalArgumentException("Cannot find class " + toClass.getOriginalName() + " in from mapping");

			pipeClass.pipe(toClass);
		}

		for (var pipeClass : classes.values()) {
			int subclassIndex = pipeClass.mapped.indexOf('$');
			if (subclassIndex == -1)
				continue;

			var baseClassMapped = getClassFromMiddleName(pipeClass.mapped.substring(0, subclassIndex));
			if (baseClassMapped.isPresent()) {
				pipeClass.mapped = baseClassMapped.get().mapped + pipeClass.mapped.substring(subclassIndex);
			}
		}
	}

	@Override
	public Collection<? extends ClassMapping> getClasses() {
		return classes.values();
	}

	protected @NotNull Optional<PipeClass> getClassFromMiddleName(@NotNull String middle) {
		for (var pipeClass : classes.values()) {
			if (pipeClass.middle.equals(middle))
				return Optional.of(pipeClass);
		}
		return Optional.empty();
	}

	private abstract class PipedObject {

		protected @NotNull String original;
		protected @NotNull String mapped;
		protected @NotNull String middle;

		protected PipedObject(@NotNull String original, @NotNull String mapped) {
			this.original = original;
			this.mapped = mapped;

			this.middle = mapped;
		}

		public @NotNull String getOriginalName() {
			return original;
		}

		public @NotNull String getMappedName() {
			return mapped;
		}

		protected @NotNull PipedObject setOriginalMiddle() {
			middle = original;
			return this;
		}

	}

	private class PipeClass extends PipedObject implements ClassMapping {

		private final List<PipeField> fields = new ArrayList<>();
		private final List<PipeMethod> methods = new ArrayList<>();

		public PipeClass(@NotNull String original, @NotNull String mapped) {
			super(original, mapped);
		}

		protected void pipe(ClassMapping toClass) {
			this.mapped = toClass.getMappedName();

			for (var toField : toClass.getFields()) {
				var pipeFieldOpt = getField(toField.getOriginalName());

				if (pipeFieldOpt.isPresent()) {
					pipeFieldOpt.get().mapped = toField.getMappedName();
				} else {
					fields.add(new PipeField(toField.getOriginalName(), toField.getMappedName()).setOriginalMiddle());
				}
			}

			for (var toMethod : toClass.getMethods()) {
				var parameterTypes = Stream.of(toMethod.getParameterTypes())
						.map(middleType -> getClassFromMiddleName(middleType.getTypeName())
								.map(x -> (Type) x)
								.orElse(middleType))
						.toArray(Type[]::new);

				var pipeMethodOpt = getMethod(toMethod.getOriginalName(), parameterTypes);

				if (pipeMethodOpt.isPresent()) {
					pipeMethodOpt.get().mapped = toMethod.getMappedName();
				} else {
					methods.add(new PipeMethod(toMethod.getOriginalName(), toMethod.getMappedName(), parameterTypes)
							.setOriginalMiddle());
				}
			}
		}

		@Override
		public Collection<? extends FieldMapping> getFields() {
			return fields;
		}

		@Override
		public Collection<? extends MethodMapping> getMethods() {
			return methods;
		}

		private @NotNull Optional<PipeField> getField(@NotNull String mapped) {
			return fields.stream().filter(field -> field.mapped.equals(mapped)).findAny();
		}

		private @NotNull Optional<PipeMethod> getMethod(@NotNull String mapped, @NotNull Type @NotNull [] parameterTypes) {
			return methods.stream()
					.filter(method -> method.mapped.equals(mapped) && method.isSameParameters(parameterTypes))
					.findAny();
		}

		private class PipeField extends PipedObject implements FieldMapping {

			public PipeField(@NotNull String original, @NotNull String mapped) {
				super(original, mapped);
			}

			@Override
			protected @NotNull PipeField setOriginalMiddle() {
				return (@NotNull PipeField) super.setOriginalMiddle();
			}

		}

		private class PipeMethod extends PipedObject implements MethodMapping {

			private final @NotNull Type @NotNull [] parameterTypes;

			public PipeMethod(@NotNull String original, @NotNull String mapped, @NotNull Type @NotNull [] parameterTypes) {
				super(original, mapped);
				this.parameterTypes = parameterTypes;
			}

			@Override
			public @NotNull Type @NotNull [] getParameterTypes() {
				return parameterTypes;
			}

			@Override
			protected @NotNull PipeMethod setOriginalMiddle() {
				return (@NotNull PipeMethod) super.setOriginalMiddle();
			}

		}

	}

}
