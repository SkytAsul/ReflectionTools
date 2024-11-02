package fr.skytasul.reflection;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.abort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;

class TransparentReflectionAccessorTest {

	private TransparentReflectionAccessor instance;

	@BeforeEach
	void setUp() throws Exception {
		this.instance = new TransparentReflectionAccessor();
	}

	@Test
	void testClassExisting() {
		var clazz = DummyTestClass.class;
		assertDoesNotThrow(() -> {
			var mappedClass = instance.getClass(clazz.getName());

			assertEquals(clazz, mappedClass.getClassInstance());
			assertEquals(clazz.arrayType(), mappedClass.getArrayType());
		});
	}

	@Test
	void testClassNotExisting() {
		assertThrows(ClassNotFoundException.class, () -> instance.getClass("foo.bar.Lol"));
	}

	@Test
	void testField() {
		String fieldName = "field";
		String paramValue1 = "paramValue1";
		String paramValue2 = "paramValue2";
		try {
			Field realField = DummyTestClass.class.getDeclaredField(fieldName);

			assertDoesNotThrow(() -> {
				var mappedClass = instance.getClass(DummyTestClass.class.getName());

				assertEquals(realField, mappedClass.getFieldInstance(fieldName));

				var mappedField = mappedClass.getField(fieldName);

				assertEquals(realField, mappedField.getFieldInstance());

				var classInstance = new DummyTestClass(paramValue1);
				assertEquals(paramValue1, mappedField.get(classInstance));
				mappedField.set(classInstance, paramValue2);
				assertEquals(paramValue2, mappedField.get(classInstance));
			});
		} catch (NoSuchFieldException | SecurityException ex) {
			abort("Cannot find field");
		}

	}

}
