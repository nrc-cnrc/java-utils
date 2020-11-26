package ca.nrc.introspection;

import java.util.HashMap;
import java.util.Map;

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
	public void test__publicFields__HappyPath() throws Exception {
		Dummy obj = new Dummy();
		Map<String,Object> expFields = new HashMap<String,Object>();
		{
			expFields.put("pubFieldNoAccessors", "Value of pubFieldNoAccessors");
			expFields.put("privFieldWithBothAccessors", "Value of privFieldWithBothAccessors");
		}
		AssertObject.assertDeepEquals("", expFields, Introspection.publicFields(obj));
		AssertObject.assertDeepEquals("", expFields, Introspection.publicFields(Dummy.class));
	}
	
	@Test
	public void test__publicFields__ObjectInheritsFromAParentClass__IncludesInheritedAttributes() throws Exception {
		DummySubclass obj = new DummySubclass();
		Map<String,Object> gotFields = Introspection.publicFields(obj);
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
}
