package fr.skytasul.reflection.shrieker;

import fr.skytasul.reflection.MappingFileReader;
import fr.skytasul.reflection.VersionedMappings;
import fr.skytasul.reflection.VersionedMappingsImplementation;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

public class MappingsShrieker {

	private final Consumer<VersionedMappings> initializeFunction;

	public MappingsShrieker(Consumer<VersionedMappings> initializeFunction) {
		this.initializeFunction = initializeFunction;
	}

	public void initializeVersion(int major, int minor, int patch, @NotNull Path mappingsPath)
			throws IOException, ReflectiveOperationException {
		var fakeMappings = new FakeVersionedMappings(major, minor, patch);
		initializeFunction.accept(fakeMappings);

		var reader = new MappingFileReader(mappingsPath, major, minor, patch);
		reader.parseMappings();
		var fullMappings = (VersionedMappingsImplementation) reader.getAvailableVersions().get(0);

		var reducedMappings = new VersionedMappingsImplementation(major, minor, patch);
		for (var fakeClass : fakeMappings.classes.values()) {
			var mappedClass = new VersionedMappingsImplementation.ClassHandle(fakeClass.key,
					fullMappings.getClass(fakeClass.key).getMappedName());

			for (var fakeField : fakeClass.fields.values()) {
				var mappedField =
						mappedClass.new FieldHandle(fakeField.key(), mappedClass.getField(fakeField.key()).remapped);
			}
		}
	}

}
