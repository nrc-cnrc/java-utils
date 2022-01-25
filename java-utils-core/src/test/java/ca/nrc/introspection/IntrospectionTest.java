package ca.nrc.introspection;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.json.JSONObject;
import org.junit.jupiter.api.*;

import ca.nrc.testing.AssertObject;

public class IntrospectionTest {
	
	@Test
	public void test__downcastAs__HappyPath() {
		Number num = new Integer(1);
		
		Integer intNum = 
					Introspection.downcastTo(Integer.class, num);
		
		Assertions.assertEquals(intNum, new Integer(1));
	}

	@Test
	public void test__fieldValues__MiscCases() throws Exception {
		SomeClass obj = new SomeClass();

		JSONObject expFields = new JSONObject()
			.put("privInstPropWithAccessor", "priv inst prop with accessor")
			.put("pubInstProp", "pub inst prop");
		AssertObject.assertEqualsJsonCompare(
			"Field values for class, public fields only",
			expFields.toString(), Introspection.fieldValues(SomeClass.class));

		expFields = new JSONObject()
			.put("privInstPropWithAccessor", "priv inst prop with accessor")
			.put("pubInstProp", "pub inst prop")
			.put("privInstProp", "priv inst prop")
			.put("protInstProp", "prot inst prop")
			;
		AssertObject.assertEqualsJsonCompare(
			"Field values for class, all fields",
			expFields.toString(), Introspection.fieldValues(SomeClass.class, false));

		obj.pubInstProp = "hello";
		obj.setPrivInstPropWithAccessor("greetings");
		expFields = new JSONObject()
			.put("privInstPropWithAccessor", "greetings")
			.put("pubInstProp", "hello")
			;
		AssertObject.assertEqualsJsonCompare(
			"Field values for instance, public fields only",
			expFields.toString(), Introspection.fieldValues(obj));

		obj.pubInstProp = "hello";
		obj.setPrivInstPropWithAccessor("greetings");
		obj.protInstProp = "howdy";
		expFields = new JSONObject()
			.put("privInstPropWithAccessor", "greetings")
			.put("privInstProp", "priv inst prop")
			.put("pubInstProp", "hello")
			.put("protInstProp", "howdy")
			;
		AssertObject.assertEqualsJsonCompare(
			"Field values for instance, all fields",
			expFields.toString(), Introspection.fieldValues(obj, false));
	}


	@Test
	public void test__fieldValues__ObjectInheritsFromAParentClass__IncludesInheritedAttributes() throws Exception {
		DummySubclass obj = new DummySubclass();
		Map<String,Object> gotFields = Introspection.fieldValues(obj);
		Map<String,Object> expFields = new HashMap<String,Object>();
		{
			expFields.put("pubFieldNoAccessors", "Value of pubFieldNoAccessors");
			expFields.put("privFieldWithBothAccessors", "Value of privFieldWithBothAccessors");
			expFields.put("subclassAttr1", 1);
			expFields.put("subclassAttr2", "hello");
		}
		AssertObject.assertDeepEquals("", expFields, gotFields);
	}
	
	@Test
	public void test__getField__HappyPath() throws Exception {
		Dummy obj = new Dummy();
		
		Assertions.assertEquals(
			Introspection.getFieldValue(obj, "pubFieldNoAccessors"),
			"Value of pubFieldNoAccessors");
		Assertions.assertEquals(
			Introspection.getFieldValue(obj, "privFieldWithBothAccessors"),
			"Value of privFieldWithBothAccessors");
	}
	
	@Test
	public void test__getField__ObjectInheritsFromAParentClass__IncludesInheritedAttributes() throws Exception {
		DummySubclass obj = new DummySubclass();
		Assertions.assertEquals(
			"Value of pubFieldNoAccessors", Introspection.getFieldValue(obj, "pubFieldNoAccessors"));
		Assertions.assertEquals(
			"Value of privFieldWithBothAccessors", Introspection.getFieldValue(obj, "privFieldWithBothAccessors"));
		Assertions.assertEquals(1, Introspection.getFieldValue(obj, "subclassAttr1"));
		Assertions.assertEquals("hello", Introspection.getFieldValue(obj, "subclassAttr2"));
	}

	//////////////////////////////////////////////
	// Test Helpers
	//////////////////////////////////////////////

	public static class SomeClass {
		public static String staticProp = "pub static prop";
		public String pubInstProp = "pub inst prop";
		private String privInstProp = "priv inst prop";
		protected String protInstProp = "prot inst prop";
		private String privInstPropWithAccessor = "priv inst prop with accessor";
		public String getPrivInstPropWithAccessor() {
			return privInstPropWithAccessor;
		}

		@JsonIgnore
		public String pubWithJsonIgnore = "pub with JSONIgnore";

		public void setPrivInstPropWithAccessor(String val) {
			privInstPropWithAccessor = val;
		}
	}
}
