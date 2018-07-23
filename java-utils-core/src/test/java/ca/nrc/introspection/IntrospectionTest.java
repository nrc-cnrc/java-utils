package ca.nrc.introspection;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonIgnore;

import ca.nrc.introspection.Introspection;
import ca.nrc.testing.AssertHelpers;

public class IntrospectionTest {
	
	@Test
	public void test__downcastAs__HappyPath() {
		Number num = new Integer(1);
		
		Integer intNum = 
					Introspection.downcastTo(Integer.class, num);
		
		Assert.assertEquals("", intNum, new Integer(1));
	}
	
	@Test
	public void test__publicFields__HappyPath() throws Exception {
		DummyObject obj = new DummyObject();
		Map<String,Object> gotFields = Introspection.publicFields(obj);
		Map<String,Object> expFields = new HashMap<String,Object>();
		{
			expFields.put("pubFieldNoAccessors", "Value of pubFieldNoAccessors");
			expFields.put("privFieldWithBothAccessors", "Value of privFieldWithBothAccessors");
		}
		AssertHelpers.assertDeepEquals("", expFields, gotFields);
		
		
	}
}
