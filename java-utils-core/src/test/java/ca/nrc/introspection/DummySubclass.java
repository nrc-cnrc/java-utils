package ca.nrc.introspection;

public class DummySubclass extends Dummy {
	
	public int subclassAttr1 = 1;
	
	private String subclassAttr2 = "hello";
		public String getSubclassAttr2() {
			return subclassAttr2;
		}

}
