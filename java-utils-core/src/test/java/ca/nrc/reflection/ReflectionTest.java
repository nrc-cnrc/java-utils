package ca.nrc.reflection;

import org.junit.Assert;
import org.junit.Test;

public class ReflectionTest {
	
	@Test
	public void test__downcastAs__HappyPath() {
		Number num = new Integer(1);
		
		Integer intNum = 
					Reflection.downcastTo(Integer.class, num);
		
		Assert.assertEquals("", intNum, new Integer(1));
	}
}
