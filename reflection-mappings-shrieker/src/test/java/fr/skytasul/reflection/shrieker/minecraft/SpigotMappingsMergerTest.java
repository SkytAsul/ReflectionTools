package fr.skytasul.reflection.shrieker.minecraft;

import static fr.skytasul.reflection.shrieker.TestUtils.getLines;
import static fr.skytasul.reflection.shrieker.TestUtils.readLines;
import static fr.skytasul.reflection.shrieker.TestUtils.writeMappings;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import fr.skytasul.reflection.mappings.files.ProguardMapping;
import org.junit.jupiter.api.Test;

class SpigotMappingsMergerTest {

	private static final ProguardMapping MAPPING_TYPE = new ProguardMapping(true);

	@Test
	void test() {
		var classMapping = new SpigotClassMapping(true).parse(getLines("""
				p net/minecraft/EnumChatFormat
				ab net/minecraft/SharedConstants
				"""));
		var membersMapping = new SpigotMemberMapping(true).parse(getLines("""
				net/minecraft/EnumChatFormat c ()Z isFormat
				net/minecraft/SharedConstants a (C)Z isAllowedChatCharacter
				net/minecraft/SharedConstants b ()Lcom/mojang/bridge/game/GameVersion; getGameVersion
				"""));

		var merged = SpigotMappingsMerger.merge(classMapping, membersMapping);
		assertEquals("""
				ab -> net.minecraft.SharedConstants:
				    a(char) -> isAllowedChatCharacter
				    b() -> getGameVersion
				p -> net.minecraft.EnumChatFormat:
				    c() -> isFormat
				""", writeMappings(MAPPING_TYPE, merged));
	}

	@Test
	void testParseSpigotMappings() {
		assertDoesNotThrow(() -> {
			var classesLines = readLines(getClass().getResource("/bukkit-1.17.1-cl.csrg"));
			var membersLines = readLines(getClass().getResource("/bukkit-1.17.1-members.csrg"));
			var classMappings = new SpigotClassMapping(true).parse(classesLines);
			var membersMappings = new SpigotMemberMapping(true).parse(membersLines);
			SpigotMappingsMerger.merge(classMappings, membersMappings);
		});
	}

}
