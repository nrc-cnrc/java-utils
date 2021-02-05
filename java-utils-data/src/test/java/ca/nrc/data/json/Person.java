package ca.nrc.data.json;

public class Person  {
	public String name = null;
	public String gender = null;

	public Person() {}

	public Person(String _name) {
		this.name = _name;
	}

	public Person(String _name, String _gender) {
		this.name = _name;
		this.gender = _gender;
	};

}
