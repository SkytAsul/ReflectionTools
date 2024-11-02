package fr.skytasul.reflection.shrieker;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import fr.skytasul.reflection.shrieker.FakeReflectionAccessor;
import org.junit.jupiter.api.Test;

class FakeReflectionAccessorTest {

	@Test
	void testAll() {
		assertDoesNotThrow(() -> {
			var mappings = new FakeReflectionAccessor();
			var clazz = mappings.getClass("random.package.RandomClass");
			clazz.getMethod("randomMethod", int.class);
			clazz.getField("randomField");

			assertEquals(1, mappings.classes().size());
			clazz = mappings.getClass("random.package.RandomClass");
			assertEquals(1, mappings.classes().size());
			assertEquals(1, clazz.methods().size());
			assertEquals(1, clazz.fields().size());
		});
	}

}
