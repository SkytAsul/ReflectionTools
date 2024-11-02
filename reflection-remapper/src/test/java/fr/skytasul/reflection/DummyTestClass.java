package fr.skytasul.reflection;

public class DummyTestClass {

	private String field;

	public DummyTestClass(String parameter) {
		this.field = parameter;
	}

	public String getParameter() {
		return field;
	}

	@SuppressWarnings("unused")
	private int privateMethod() {
		return field.length();
	}

}
