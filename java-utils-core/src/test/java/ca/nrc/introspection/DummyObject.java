package ca.nrc.introspection;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class DummyObject {
	
	public String pubFieldNoAccessors = "Value of pubFieldNoAccessors";

	public String privFieldWithBothAccessors = "Value of privFieldWithBothAccessors";
		public void setPrivFieldWithBothAccessors(String val) {privFieldWithBothAccessors = val;}
		public String getPrivFieldWithBothAccessors() {return privFieldWithBothAccessors;}

	private String privFieldWithOnlySetter = "Value of privFieldWithOnlySetter";
		public void setPrivFieldWithOnlySetter(String val) {privFieldWithOnlySetter = val;}
	
	@JsonIgnore
	private String getThisIsNotAnAccessor() {return "Return value of getThisIsNotAnAccessor";}
}
