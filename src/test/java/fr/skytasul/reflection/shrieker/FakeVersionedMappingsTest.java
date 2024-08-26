package fr.skytasul.reflection.shrieker;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import fr.skytasul.reflection.Version;
import org.junit.jupiter.api.Test;

class FakeVersionedMappingsTest {

	@Test
	void testAll() {
		assertDoesNotThrow(() -> {
			var mappings = new FakeVersionedMappings(Version.ZERO);
			var clazz = mappings.getClass("random.package.RandomClass");
			clazz.getMethod("randomMethod", int.class);
			clazz.getField("randomField");

			assertEquals(1, mappings.getClasses().size());
			clazz = mappings.getClass("random.package.RandomClass");
			assertEquals(1, mappings.getClasses().size());
			assertEquals(1, clazz.getMethods().size());
			assertEquals(1, clazz.getFields().size());
		});
	}

}
