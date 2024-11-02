package fr.skytasul.reflection.shrieker.minecraft;

import static fr.skytasul.reflection.shrieker.TestUtils.getLines;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Arrays;

class SpigotMemberMappingTest {

	private SpigotMemberMapping reader;

	@BeforeEach
	void setUp() {
		reader = new SpigotMemberMapping(true);
	}

	@Test
	void testParseFullClass() {
		var mappings = reader.parse(getLines("""
				net/minecraft/world/entity/Entity a (Lnet/minecraft/sounds/SoundEffect;FF)V playSound
				"""));
		assertEquals(1, mappings.getClasses().size());

		var parsedClass = mappings.getClasses().iterator().next();
		assertEquals(1, parsedClass.getMethods().size());
		assertEquals(0, parsedClass.getFields().size());
		assertEquals("net.minecraft.world.entity.Entity", parsedClass.getOriginalName());
		assertEquals("net.minecraft.world.entity.Entity", parsedClass.getMappedName());
		var parsedMethod = parsedClass.getMethods().iterator().next();
		assertEquals("a", parsedMethod.getOriginalName());
		assertEquals("playSound", parsedMethod.getMappedName());
		assertArrayEquals(new String[] {"net.minecraft.sounds.SoundEffect", "float", "float"},
				Arrays.stream(parsedMethod.getParameterTypes()).map(Type::getTypeName).toArray());
	}

	@Test
	void testParseEmptyParameters() {
		assertArrayEquals(new Type[] {}, reader.parseParameters(""));
	}

	@Test
	void testParsePrimitiveParameter() {
		assertArrayEquals(new Type[] {int.class}, reader.parseParameters("I"));
		assertArrayEquals(new Type[] {int.class, boolean.class}, reader.parseParameters("IZ"));
	}

	@Test
	void testParseJavaParameters() {
		assertArrayEquals(new Type[] {String.class}, reader.parseParameters("Ljava/lang/String;"));
		assertArrayEquals(new Type[] {String.class, java.nio.file.Files.class},
				reader.parseParameters("Ljava/lang/String;Ljava/nio/file/Files;"));
	}

	@Test
	void testParseJavaArrayParameters() {
		assertArrayEquals(new Type[] {String[].class, int[].class},
				reader.parseParameters("[Ljava/lang/String;[I"));
	}

	@Test
	void testParseSpigotMappings() {
		assertDoesNotThrow(() -> {
			try (var inputReader =
					new BufferedReader(
							new InputStreamReader(getClass().getResourceAsStream("/bukkit-1.17.1-members.csrg")))) {
				var lines = inputReader.lines().toList();
				reader.parse(lines);
			}
		});
	}

}
