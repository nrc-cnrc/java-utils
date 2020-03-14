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
				gotNum <= maxNum);
	}

	public static void isLessOrEqualTo(String mess, Double gotNum, Double maxNum) {
		Assert.assertTrue(mess+"\nNumber was larger than expected.\n   Got : "+gotNum+"\n   Expected at most: "+maxNum,
				gotNum  <= maxNum);
	}

	public static void performanceHasNotChanged(String ofWhat, 
			Double gotPerf, Double oldPerf, Double tolerance) {
		
		performanceHasNotChanged(ofWhat, 
				gotPerf, oldPerf, tolerance, null);
	}
	
	
	public static void performanceHasNotChanged(String ofWhat, 
		Double gotPerf, Double oldPerf, Double tolerance, 
		Boolean highIsGood) {
		if (highIsGood == null) {
			highIsGood = true;
		}
		
		Double delta = gotPerf - oldPerf;
		if (Math.abs(delta) > tolerance) {
			String mess = "Performance of "+ofWhat+" has significantly "+
					"DECREASED.";
			if ((delta > 0 && highIsGood) ||
					delta < 0 && !highIsGood) {
				mess = "Performance of "+ofWhat+" has significantly "+
					"IMPROVED.\n\nYou might want to change the expectations for "+
					"this test, so that you don't loose that gain.";
			}
			mess += "\nNew performance : "+gotPerf+"\n"+
					"Old performance : "+oldPerf+"\n"+
					"Delta           : "+delta+"\n"+
					"Max tolerance   : "+tolerance;
			
					;
			Assert.fail(mess);			
		}
	}
}
