package ca.nrc.testing;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.jupiter.api.*;

public class AssertNumber {

	public static enum PerfChange {SAME, WORSENED, IMPROVED};

	public static void isGreaterOrEqualTo(Number gotNum, Number minNum) {
		isGreaterOrEqualTo("", gotNum, minNum);
	}

	public static void isGreaterOrEqualTo(String mess, Number gotNum, Number minNum) {
		Assertions.assertTrue(gotNum.floatValue() >= minNum.floatValue(),
		mess+"\nNumber was not as large as expected.\n   Got : "+gotNum+"\n   Expected at least: "+minNum);
	}

	public static void isLessOrEqualTo(Number gotNum, Number maxNum) {
		isLessOrEqualTo("", gotNum, maxNum);
	}
	
	public static void isLessOrEqualTo(String mess, Number gotNum, Number maxNum) {
		Assertions.assertTrue(gotNum.floatValue() <= maxNum.floatValue(),
		mess+"\nNumber was larger than expected.\n   Got : "+gotNum+"\n   Expected at most: "+maxNum);
	}

		public static void differsFrom(Number num, Number otherNum) {
		isGreaterOrEqualTo("", num, otherNum);
	}

	public static void differsFrom(String mess, Number num, Number otherNum) {
		Boolean differ = null;
		if (num == null && otherNum != null ||
			num != null & otherNum == null) {
			differ = true;
		}
		if (differ == null) {
			differ = (num.floatValue() != otherNum.floatValue());
		}

		Assertions.assertTrue(differ,
		mess+"\nThe two numbers should have differed, but the were both equal to "+num);
	}


	public static void performanceHasNotChanged(String ofWhat,
		  Double gotPerf, Double oldPerf, Double tolerance) {
		performanceHasNotChanged(
			ofWhat, gotPerf, oldPerf, Pair.of(tolerance,tolerance),
			(Boolean) null, "");
	}


	public static void performanceHasNotChanged(String ofWhat,
		Double gotPerf, Double oldPerf, Double tolerance,
		Boolean highIsGood) {
		performanceHasNotChanged(ofWhat, gotPerf, oldPerf,
			Pair.of(tolerance, tolerance),
			highIsGood, (String)null);
	}

	public static void performanceHasNotChanged(String ofWhat,
		Double gotPerf, Double oldPerf,
		Pair<Double,Double> tolerances, Boolean highIsGood) {
		performanceHasNotChanged(ofWhat, gotPerf, oldPerf,
			tolerances, highIsGood, (String)null);
	}


	public static void performanceHasNotChanged(
		String ofWhat, Double gotPerf, Double oldPerf,
		Pair<Double,Double> tolerances) {

		performanceHasNotChanged(ofWhat, gotPerf, oldPerf,
			tolerances, (Boolean) null, (String) null);

	}

	public static void performanceHasNotChanged(String ofWhat, 
		Double gotPerf, Double oldPerf, Pair<Double,Double> tolerances,
		Boolean highIsGood, String mess) {
		if (highIsGood == null) {
			highIsGood = true;
		}

		Pair<PerfChange,Double> change =
			performanceChange(gotPerf, oldPerf, highIsGood);
		PerfChange changeType = change.getLeft();
		Double delta = change.getRight();
		if (changeType != PerfChange.SAME) {
			String chanteTypeStr = null;
			if (changeType == PerfChange.WORSENED) {
				Double absTolerance = tolerances.getLeft();
				if (absTolerance != null && delta > absTolerance) {
					chanteTypeStr = "WORSENED";
				}
			} else {
				Double absTolerance = tolerances.getRight();
				if (absTolerance != null && delta > absTolerance) {
					chanteTypeStr = "IMPROVED";
				}
			}
			if (chanteTypeStr != null) {
				mess +=
				"\nPerformance of '" + ofWhat + "' has significantly " +
				chanteTypeStr;
				mess +=
				"\n" +
				"New performance : " + gotPerf + "\n" +
				"Old performance : " + oldPerf + "\n" +
				"Delta           : " + delta + "\n" +
				"Max tolerances  : worsening <= " + tolerances.getLeft() + ", improv. <= " + tolerances.getRight() + "]";
				;
				Assertions.fail(mess);
			}
		}
		
	}

	private static Pair<PerfChange, Double> performanceChange(
		Double gotPerf, Double oldPerf, Boolean highIsGood) {

		PerfChange changeType = PerfChange.SAME;
		Double delta = gotPerf - oldPerf;
		Double absDelta = Math.abs(delta);
		if (delta < 0) {
			if (highIsGood) {
				changeType = PerfChange.WORSENED;
			} else {
				changeType = PerfChange.IMPROVED;
			}
		} else if (delta > 0) {
			if (highIsGood) {
				changeType = PerfChange.IMPROVED;
			} else {
				changeType = PerfChange.WORSENED;
			}
		}

		return Pair.of(changeType, absDelta);
	}

	public static void assertEquals(String mess, Number exp, Number got, 
			double tolerance) {
		if (exp == null && got == null) {
			// Nothing to do. All is OK
		} else if (exp == null && got != null ||
				got == null && exp != null) {
			Assertions.fail(
				mess+"\n"+
				"One number was null but not the other\n"+
				"  Exp : "+exp+"\n"+
				"  Got : "+got);
		} else {
			Double expDouble = exp.doubleValue();
			Double gotDouble = got.doubleValue();
			Assert.assertEquals(mess, expDouble, gotDouble, tolerance);
		}
	}
}
