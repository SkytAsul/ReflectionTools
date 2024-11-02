package fr.skytasul.reflection.shrieker.minecraft;

import fr.skytasul.reflection.mappings.Mappings;
import fr.skytasul.reflection.mappings.RealMappings;
import fr.skytasul.reflection.mappings.RealMappings.RealClassMapping;
import fr.skytasul.reflection.mappings.RealMappings.RealClassMapping.RealFieldMapping;
import fr.skytasul.reflection.mappings.RealMappings.RealClassMapping.RealMethodMapping;
import org.jetbrains.annotations.NotNull;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class SpigotMappingsMerger {

	private SpigotMappingsMerger() {}

	/**
	 * Merges:
	 *
	 * <pre>
	 * Class mapping:
	 *   a -> net.minecraft.SomeClass
	 *   b -> net.minecraft.AnotherClass
	 * Members mapping:
	 *   net.minecraft.SomeClass.z(net.minecraft.AnotherClass) -> someMethod
	 * To:
	 *   a -> net.minecraft.SomeClass:
	 *     z(b) -> someMethod
	 *   b -> net.minecraft.AnotherClass
	 * </pre>
	 *
	 * @param classMappings
	 * @param membersMappings
	 * @return
	 */
	public static @NotNull Mappings merge(@NotNull Mappings classMappings, @NotNull Mappings membersMappings) {
		Map<String, RealClassMapping> classes = classMappings.getClasses().stream().collect(Collectors.toMap(
				x -> x.getMappedName(),
				x -> new RealClassMapping(
						x.getOriginalName(),
						x.getMappedName(),
						x.getFields().stream()
								.map(field -> new RealFieldMapping(field.getOriginalName(), field.getMappedName()))
								.toList(),
						new ArrayList<>())));
		Map<String, RealClassMapping> classesByMapped =
				classes.values().stream().collect(Collectors.toMap(x -> x.getMappedName(), x -> x));

		for (var classWithMembers : membersMappings.getClasses()) {
			var classToFill = classes.get(classWithMembers.getOriginalName());

			if (classToFill == null)
				throw new IllegalArgumentException("Cannot find class " + classWithMembers.getOriginalName());

			for (var method : classWithMembers.getMethods()) {
				var newParameters = Stream.of(method.getParameterTypes()).map(oldType -> {
					boolean isArray = false;
					String typeName = oldType.getTypeName();
					if (oldType instanceof Mappings.ClassMapping.ClassArrayType array) {
						isArray = true;
						typeName = array.componentMapping().getTypeName();
					}
					Type newType = classesByMapped.get(typeName);
					if (newType == null)
						return oldType;
					return isArray ? new Mappings.ClassMapping.ClassArrayType(newType) : newType;
				}).toArray(Type[]::new);

				classToFill.methods()
						.add(new RealMethodMapping(method.getOriginalName(), method.getMappedName(), newParameters));
			}
		}

		return new RealMappings(classes.values());
	}

}
