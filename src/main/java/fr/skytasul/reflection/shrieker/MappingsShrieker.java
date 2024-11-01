package fr.skytasul.reflection.shrieker;

import fr.skytasul.reflection.FakeReflectionAccessor;
import fr.skytasul.reflection.ReflectionAccessor;
import fr.skytasul.reflection.Version;
import fr.skytasul.reflection.mappings.Mappings;
import fr.skytasul.reflection.mappings.Mappings.ClassMapping.FieldMapping;
import fr.skytasul.reflection.mappings.Mappings.ClassMapping.MethodMapping;
import fr.skytasul.reflection.mappings.RealMappings;
import fr.skytasul.reflection.mappings.RealMappings.RealClassMapping;
import fr.skytasul.reflection.mappings.RealMappings.RealClassMapping.RealFieldMapping;
import fr.skytasul.reflection.mappings.RealMappings.RealClassMapping.RealMethodMapping;
import fr.skytasul.reflection.mappings.files.MappingFileWriter;
import fr.skytasul.reflection.mappings.files.MappingType;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * <b>Warning:</b> make sure all necessary classes (outside the reflected ones) are present in the
 * classpath, otherwise it would not work!
 */
public class MappingsShrieker {

	private final @NotNull ReflectionInitializer initializeFunction;
	private final @NotNull MappingType mappingType;

	private final @NotNull Map<Version, Mappings> allReducedMappings = new HashMap<>();

	/**
	 * Creates a shrieker instance.
	 *
	 * @param mappingType type of mappings in the output file
	 * @param initializeFunction function that is called for each call to
	 *        {@link #registerVersionMappings(Version, Path)}. All reflection accesses made to the
	 *        {@link Mappings} passed as parameter to this function will be recorded and used to create
	 *        the reduced mappings.
	 */
	public MappingsShrieker(@NotNull MappingType mappingType, @NotNull ReflectionInitializer initializeFunction) {
		this.mappingType = mappingType;
		this.initializeFunction = initializeFunction;
	}

	/**
	 * Register the mappings used for a specific version.
	 *
	 * @param version the version of the mappings
	 * @param mappings mappings of this version
	 * @throws ReflectiveOperationException if an error happened while initializing the reflection
	 */
	public void registerVersionMappings(@NotNull Version version, @NotNull Mappings mappings)
			throws ReflectiveOperationException {
		// First step: fill in the fake mappings with the classes/fields/methods actually needed
		var fakeReflection = new FakeReflectionAccessor();
		initializeFunction.initializeReflection(fakeReflection, version);

		// Second step: construct reduced mappings by merging the fake one with the obfuscated names from
		// the real one.
		// NOTE: we do not make use of Stream.map(...).toList() chains because we want to be able to throw
		// exceptions.
		var reducedMappings = new RealMappings(new ArrayList<>());
		for (var fakeClass : fakeReflection.classes()) {
			var fullClass = mappings.getClasses().stream()
					.filter(x -> x.getOriginalName().equals(fakeClass.name()))
					.findAny().orElseThrow(() -> new ClassNotFoundException(fakeClass.name()));

			var reducedFields = new ArrayList<RealFieldMapping>(fakeClass.fields().size());
			for (var fakeField : fakeClass.fields()) {
				FieldMapping fullField = fullClass.getFields().stream()
						.filter(x -> x.getOriginalName().equals(fakeField.name()))
						.findAny().orElseThrow(() -> new NoSuchFieldException(fakeClass.name() + "." + fakeField.name()));
				reducedFields.add(new RealFieldMapping(fakeField.name(), fullField.getMappedName()));
			}

			var reducedMethods = new ArrayList<RealMethodMapping>(fakeClass.methods().size());
			for (var fakeMethod : fakeClass.methods()) {
				MethodMapping fullMethod = fullClass.getMethods().stream()
						.filter(x -> x.getOriginalName().equals(fakeMethod.name())
								&& x.isSameParameters(fakeMethod.parameterTypes()))
						.findAny().orElseThrow(() -> new NoSuchMethodException(fakeClass.name() + "."
								+ Mappings.getStringForMethod(fakeMethod.name(), fakeMethod.parameterTypes())));
				reducedMethods.add(new RealMethodMapping(fakeMethod.name(), fullMethod.getMappedName(),
						fullMethod.getParameterTypes()));
			}

			var mappedClass =
					new RealClassMapping(fakeClass.name(), fullClass.getMappedName(), reducedFields, reducedMethods);

			reducedMappings.classes().add(mappedClass);
		}

		// Finally, we can add the reduced mappings to the list of completed mappings.
		allReducedMappings.put(version, reducedMappings);
	}

	public @NotNull Map<Version, Mappings> getReducedMappings() {
		return allReducedMappings;
	}

	/**
	 * Write all constructed reduced mappings to the path specified.
	 *
	 * @param mappingsPath
	 * @throws IOException if an error occurred while writing the mappings file
	 */
	public void writeMappingsFile(@NotNull Path mappingsPath) throws IOException {
		new MappingFileWriter(mappingType, mappingsPath, allReducedMappings).writeAll();
	}

	@FunctionalInterface
	public static interface ReflectionInitializer {

		void initializeReflection(@NotNull ReflectionAccessor reflection, @NotNull Version version)
				throws ReflectiveOperationException;

	}

}
