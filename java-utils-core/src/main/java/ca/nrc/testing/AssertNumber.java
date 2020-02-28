package ca.nrc.testing;

import org.junit.Assert;

public class AssertNumber {
	
	public static void isGreaterOrEqualTo(Number gotNum, Number minNum) {
		isGreaterOrEqualTo("", gotNum, minNum);
	}

	public static void isGreaterOrEqualTo(String mess, Number gotNum, long minNum) {
		isGreaterOrEqualTo(mess, gotNum, new Long(minNum));
	}
	
	public static void isGreaterOrEqualTo(String mess, Number gotNum, Number minNum) {
		Assert.assertTrue(mess+"\nNumber was not as large as expected.\n   Got : "+gotNum+"\n   Expected at least: "+minNum,
				gotNum.floatValue() >= minNum.floatValue());
	}

	public static void isLessOrEqualTo(Number gotNum, Long maxNum) {
		isLessOrEqualTo("", gotNum, maxNum);
	}
	
	public static void isLessOrEqualTo(String mess, Number gotNum, Long maxNum) {
		Assert.assertTrue(mess+"\nNumber was larger than expected.\n   Got : "+gotNum+"\n   Expected at most: "+maxNum,
				gotNum.floatValue() <= maxNum.floatValue());
	}

}
