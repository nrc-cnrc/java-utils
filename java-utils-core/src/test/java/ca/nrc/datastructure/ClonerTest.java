package ca.nrc.datastructure;

import org.junit.jupiter.api.*;

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
}
