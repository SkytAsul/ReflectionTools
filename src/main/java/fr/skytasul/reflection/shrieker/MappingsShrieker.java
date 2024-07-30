package fr.skytasul.reflection.shrieker;

import fr.skytasul.reflection.*;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class MappingsShrieker {

	private final @NotNull Consumer<@NotNull VersionedMappings> initializeFunction;
	private final @NotNull List<@NotNull VersionedMappings> allReducedMappings = new ArrayList<>();

	/**
	 * Creates a shrieker instance.
	 *
	 * @param initializeFunction function that is called for each call to
	 *        {@link #registerVersionMappings(int, int, int, Path)}. All reflection accesses made to the
	 *        {@link VersionedMappings} passed as parameter to this Consumer will be recorded and used
	 *        to create the reduced mappings.
	 */
	public MappingsShrieker(@NotNull Consumer<@NotNull VersionedMappings> initializeFunction) {
		this.initializeFunction = initializeFunction;
	}

	/**
	 * Register the mappings used for a specific version.
	 *
	 * @param version the version of the mappings
	 * @param mappingsPath path to the mappings file of this version
	 * @throws IOException if an error happened while loading the mappings file
	 */
	public void registerVersionMappings(@NotNull Version version, @NotNull Path mappingsPath)
			throws IOException {
		// First step: fill in the fake mappings with the classes/fields/methods actually needed
		var fakeMappings = new FakeVersionedMappings(version);
		initializeFunction.accept(fakeMappings);

		// Second step: load the real version mappings to get the obfuscated names
		var reader = new MappingFileReader(mappingsPath, version);
		reader.parseMappings();
		var fullMappings = (VersionedMappingsImplementation) reader.getParsedMappings(version);

		// Third step: construct reduced mappings by merging the fake one with the obfuscated names from the
		// real one.
		var reducedMappings = new VersionedMappingsImplementation(version);
		try {
			for (var fakeClass : fakeMappings.classes.values()) {
				var mappedClass = new VersionedMappingsImplementation.ClassHandle(fakeClass.getOriginalName(),
						fullMappings.getClass(fakeClass.getOriginalName()).getObfuscatedName());

				mappedClass.fields = new ArrayList<>();
				for (var fakeField : fakeClass.fields.values()) {
					mappedClass.fields.add(mappedClass.new FieldHandle(fakeField.getOriginalName(),
							mappedClass.getField(fakeField.getOriginalName()).getObfuscatedName()));
				}

				mappedClass.methods = new ArrayList<>();
				for (var fakeMethod : fakeClass.methods) {
					mappedClass.methods.add(mappedClass.new MethodHandle(fakeMethod.getOriginalName(),
							mappedClass.getField(fakeMethod.getOriginalName()).getObfuscatedName(),
							fakeMethod.getParameterTypes()));
				}

				reducedMappings.classes.add(mappedClass);
			}
		} catch (ReflectiveOperationException ex) {
			throw new Error("This exception cannot happen.", ex);
		}

		// Finally, we can add the reduced mappings to the list of completed mappings.
		allReducedMappings.add(reducedMappings);
	}

	public @NotNull List<@NotNull VersionedMappings> getReducedMappings() {
		return allReducedMappings;
	}

	/**
	 * Write all construced reduced mappings to the path specified.
	 *
	 * @param mappingsPath
	 * @throws IOException if an error occurred while writing the mappings file
	 */
	public void writeMappingsFile(@NotNull Path mappingsPath) throws IOException {
		new MappingFileWriter(mappingsPath, allReducedMappings).writeAll();
	}

}
