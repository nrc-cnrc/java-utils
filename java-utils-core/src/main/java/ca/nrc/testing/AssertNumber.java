package ca.nrc.testing;

import org.junit.Assert;

public class AssertNumber {
	
	public static void isGreaterOrEqualTo(Number gotNum, Number minNum) {
		isGreaterOrEqualTo("", gotNum, minNum);
	}

	public static void isGreaterOrEqualTo(String mess, Number gotNum, Number minNum) {
		Assert.assertTrue(mess+"\nNumber was not as large as expected.\n   Got : "+gotNum+"\n   Expected at least: "+minNum,
				gotNum.floatValue() >= minNum.floatValue());
	}

	public static void isGreaterOrEqualTo(String mess, Double gotNum, Double minNum) {
		Assert.assertTrue(mess+"\nNumber was not as large as expected.\n   Got : "+gotNum+"\n   Expected at least: "+minNum,
				gotNum.floatValue() >= minNum.floatValue());
	}

	public static void isLessOrEqualTo(Long gotNum, Long maxNum) {
		isLessOrEqualTo("", gotNum, maxNum);
	}
	
	public static void isLessOrEqualTo(String mess, Long gotNum, Long maxNum) {
		Assert.assertTrue(mess+"\nNumber was larger than expected.\n   Got : "+gotNum+"\n   Expected at most: "+maxNum,
				gotNum.floatValue() <= maxNum.floatValue());
	}

	public static void isLessOrEqualTo(String mess, Double gotNum, Double maxNum) {
		Assert.assertTrue(mess+"\nNumber was larger than expected.\n   Got : "+gotNum+"\n   Expected at most: "+maxNum,
				gotNum.floatValue() <= maxNum.floatValue());
	}
}
