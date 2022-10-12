package ca.nrc.dtrc.stats;

import static org.junit.Assert.*;

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import ca.nrc.testing.AssertObject;
import org.junit.jupiter.api.Assertions;

public class FrequencyHistogramTest {
	
	double tolerance = 0.01;

	//////////////////////////////
	// DOCUMENTATION TESTS
	//////////////////////////////
	
	@Test
	public void test__FequencyHistogram__Synopsis() {
		// Use this class to compute a histogram of frequencies
		// for discrete values
		//
		FrequencyHistogram<String> hist = new FrequencyHistogram<String>();
		
		// If you invoke updateFreq with just one argument, it increments that 
		// value's frequency by +1
		//
		hist.updateFreq("hello");
		hist.updateFreq("world");
		
		// You can also increment the frequency of a value by a specific number
		//
		hist.updateFreq("hello", 13);
		
		// Once you are done adding data, you can ask for all sorts of info
		//
		
		// List of values encountered
		Set<String> values = hist.allValues();
		
		// Total number of occurences of all values
		long totalOccurences = hist.totalOccurences();
		
		// Frequency of "hello", i.e. total number of occurences
		long freq = hist.frequency("hello");
		
		// Relative frequency of "hello" = 
		//   Number of occurences / total occurences
		double relFreq = hist.relativeFrequency("hello");
		
		// Relative frequency formatted as a percentage with single
		// decimal place.
		String relFreqPerc = hist.relativeFrequency("hello", 1);

		// Min max frequencies
		long minFreq = hist.min();
		long maxFreq = hist.max();
	}

	//////////////////////////////
	// DOCUMENTATION TESTS
	//////////////////////////////
	
	@Test
	public void test__FequencyHistogram__HappyPath() throws Exception {
		
		FrequencyHistogram<String> hist = new FrequencyHistogram<String>();
		hist.updateFreq("hello");
		hist.updateFreq("world");
		hist.updateFreq("hello");

		// Increment freq by +1
		String[] expValues = new String[] {"hello", "world"};
		AssertObject.assertDeepEquals(
			"List of values not as expected", expValues, hist.allValues());
		assertTotalOccurencesEquals(hist, 3);
		
		assertFrequencyEquals(hist, "hello", 2);
		assertRelFreqEquals(hist, "hello", 0.666);
		assertRelFreqPercEquals(hist, "hello", "66.7%");
		
		assertFrequencyEquals(hist, "world", 1);
		assertRelFreqEquals(hist, "world", 0.333);
		assertRelFreqPercEquals(hist, "world", "33.3%");
		
		// Increment freq by num > 1
		// You can also increment the frequency of a value by a specific number
		//
		hist.updateFreq("hello", 13);
		assertFrequencyEquals(hist, "hello", 15);
		assertTotalOccurencesEquals(hist, 16);
		
		
		long totalOccurences = hist.totalOccurences();
		
		// Frequency of "hello", i.e. total number of occurences
		long freq = hist.frequency("hello");
		
		// Relative frequency of "hello" = 
		//   Number of occurences / total occurences
		double relFreq = hist.relativeFrequency("hello");
		
		// Relative frequency formatted as a percentage with single
		// decimal place.
		String relFreqPerc = hist.relativeFrequency("hello", 1);

		Assertions.assertEquals(1, hist.min(),
			"Min frequency not as expected");
		Assertions.assertEquals(15, hist.max(),
			"Max frequency not as expected");
	}

	
	private void assertRelFreqPercEquals(FrequencyHistogram<String> hist, 
			String value, String expPercent) {
		Assert.assertEquals(
			"Percent freq not as expected for value "+value, 
			expPercent, hist.relativeFrequency(value, 1));
	}

	private void assertRelFreqEquals(FrequencyHistogram<String> hist, 
			String value, double expFreq) {
		Assert.assertEquals("Relative frequency not as expected for value "+value, 
			expFreq, hist.relativeFrequency(value), tolerance);
		
	}

	private void assertTotalOccurencesEquals(
		FrequencyHistogram<String> hist, long expTotalOcc) {
		Assert.assertEquals(
			"Total number of occurences not as expected.",
			expTotalOcc, hist.totalOccurences());
	}

	private void assertFrequencyEquals(FrequencyHistogram<String> hist, 
			String value, long expFreq) {
		long gotFreq = hist.frequency(value);
		Assert.assertEquals("Frequency not as expected for value "+value,
				expFreq, gotFreq);
		
	}
}
