package ca.nrc.testing;

import org.junit.jupiter.api.*;

public class AssertNumber {
	
	public static void isGreaterOrEqualTo(Number gotNum, Number minNum) {
		isGreaterOrEqualTo("", gotNum, minNum);
	}

	public static void isGreaterOrEqualTo(String mess, Number gotNum, Number minNum) {
		Assertions.assertTrue(gotNum.floatValue() >= minNum.floatValue(),
		mess+"\nNumber was not as large as expected.\n   Got : "+gotNum+"\n   Expected at least: "+minNum);
	}

	public static void isGreaterOrEqualTo(String mess, Double gotNum, Double minNum) {
		Assertions.assertTrue(gotNum.floatValue() >= minNum.floatValue(),
		mess+"\nNumber was not as large as expected.\n   Got : "+gotNum+"\n   Expected at least: "+minNum);
	}

	public static void isLessOrEqualTo(Long gotNum, Long maxNum) {
		isLessOrEqualTo("", gotNum, maxNum);
	}
	
	public static void isLessOrEqualTo(String mess, Long gotNum, Long maxNum) {
		Assertions.assertTrue(gotNum <= maxNum,
		mess+"\nNumber was larger than expected.\n   Got : "+gotNum+"\n   Expected at most: "+maxNum);
	}

	public static void isLessOrEqualTo(String mess, Double gotNum, Double maxNum) {
		Assertions.assertTrue(gotNum  <= maxNum,
		mess+"\nNumber was larger than expected.\n   Got : "+gotNum+"\n   Expected at most: "+maxNum);
	}

	public static void performanceHasNotChanged(String ofWhat, 
			Double gotPerf, Double oldPerf, Double tolerance) {
		
		performanceHasNotChanged(ofWhat, 
				gotPerf, oldPerf, tolerance, (Boolean)null);
	}

	public static void performanceHasNotChanged(String ofWhat,
		Double gotPerf, Double oldPerf, Double tolerance,
		Boolean highIsGood) {
		performanceHasNotChanged(ofWhat, gotPerf, oldPerf, tolerance,
			highIsGood, (String)null);
	}

	public static void performanceHasNotChanged(String ofWhat, 
		Double gotPerf, Double oldPerf, Double tolerance, 
		Boolean highIsGood, String mess) {
		if (highIsGood == null) {
			highIsGood = true;
		}
		
		Double delta = gotPerf - oldPerf;
		if (Math.abs(delta) > tolerance) {
			mess += "\nPerformance of '"+ofWhat+"' has significantly "+
					"DECREASED.";
			if ((delta > 0 && highIsGood) ||
					delta < 0 && !highIsGood) {
				mess = "Performance of '"+ofWhat+"' has significantly "+
					"IMPROVED.\n\nYou might want to change the expectations for "+
					"this test, so that you don't loose that gain.";
			}
			mess += "\nNew performance : "+gotPerf+"\n"+
					"Old performance : "+oldPerf+"\n"+
					"Delta           : "+delta+"\n"+
					"Max tolerance   : "+tolerance;
					;
			Assertions.fail(mess);			
		}
	}
	
	public static void assertEquals(String mess, Number exp, Number got, 
			double tolerance) {
		if (exp == null && got != null ||
				got == null && exp != null) {
			Assertions.fail(
				mess+"\n"+
				"One number was null but not the other\n"+
				"  Exp : "+exp+"\n"+
				"  Got : "+got);
		}
	}
}
