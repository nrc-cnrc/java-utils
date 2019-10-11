package ca.nrc.dtrc.stats;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import ca.nrc.testing.AssertHelpers;

public class HistogramTest {

	///////////////////////////////////
	// DOCUMENTATION TESTS
	///////////////////////////////////
	
	@Test
	public void test__Histogram__Synopsis() throws Exception {
		// Use this class to build a histogram of numbers
		//

		// Assume we have the following numbers
		double[] data = new double[100];
		for (int ii=0; ii < 100; ii++) {
			data[ii] = 100*Math.random();
		}

		int numBins = 10;
		Histogram hist = new Histogram(numBins, 0.0, 100.0);
		hist.load(data);
		
		for (int ii=0; ii < hist.numBins; ii++) {
			Histogram.Bin aBin = hist.bins[ii];
			double binStart = aBin.start;
			double binEnd = aBin.end;
			long binFreq = aBin.freq;
		}
	}

	///////////////////////////////////
	// VERIFICATION TESTS
	///////////////////////////////////
	
	@Test
	public void test__Histogram__HappyPath() throws Exception {
		// Assume we have the following numbers
		double[] data = new double[] {
				3.0, 4.0, 99.0, 51.0, 15.
		};
		
		int numBins = 4;
		Histogram hist = new Histogram(numBins, 0.0, 100.0);
		hist.load(data);
		HashMap<String,String> blah;
		
		List<Histogram.Bin> expBins = new ArrayList<Histogram.Bin>();
		{
			expBins.add(new Histogram.Bin(0.0, 25.0, 3));
			expBins.add(new Histogram.Bin(25.0, 50.0, 0));
			expBins.add(new Histogram.Bin(50, 75.0, 1));
			expBins.add(new Histogram.Bin(75.0, 100.0, 1));
		};
		Histogram.Bin[] gotBins = hist.bins;
		AssertHelpers.assertDeepEquals("Bins were not as expected", 
				expBins, gotBins);
	}
}

