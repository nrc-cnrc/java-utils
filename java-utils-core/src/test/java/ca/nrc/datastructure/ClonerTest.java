package ca.nrc.datastructure;

import ca.nrc.testing.AssertObject;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ClonerTest {

	/*****************************
	 * DOCUMENTATION TESTS
	 *****************************/
	
	@Test
	public void test__Cloner__Synopsis() throws Exception {
		// Use Cloner to clone an object
		String[] obj1 = new String[] {"hello", "world"};
		String[] obj2 = Cloner.clone(obj1);
	}
	
	/*****************************
	 * VERIFICATION TESTS
	 *****************************/
	
	@Test
	public void test__Cloner__HappyPath() throws Exception {
		String[] orig = new String[] {"hello", "world"};
		String[] clone = Cloner.clone(orig);
		clone[1] = "universe";
		
		Assertions.assertEquals(
			orig[0], clone[0],
			"First element should have been the same because it was not changed in the clone");
		Assertions.assertTrue(
			! orig[1].equals(clone[1]),
			"Second element should have differed because it was changed in the clone.");
	}

	@Test
	public void test__Cloner__HashMapKeySet() throws Exception {
		Map<String,String> map = new HashMap<String,String>();
		map.put("hello", "world");
		Set<String> orig = map.keySet();
		Set<String> clone = Cloner.clone(orig);
		String[] expClone = new String[] {"hello"};
		AssertObject.assertDeepEquals("Clone not as expected", expClone, orig);
	}

}
