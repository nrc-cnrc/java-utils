package ca.nrc.dtrc.stats;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class FrequencyHistogram<T> {
	
	Map<T,Long> freq4value = new HashMap<T,Long>();
	Long _min = null;
	Long _max = null;

	// Empty constructor for Jackson serialization
	public FrequencyHistogram() {}

	public void updateFreq(T value) {
		updateFreq(value, 1);
	}

	public void updateFreq(T value, int incr) {
		if (!freq4value.containsKey(value)) {
			freq4value.put(value, new Long(0));
		}
		Long oldFreq = freq4value.get(value);
		Long newFreq = oldFreq + incr;
		freq4value.put(value, newFreq);
		if (_min == null || _min > newFreq) {
			_min = newFreq;
		}
		if (_max == null || _max < newFreq) {
			_max = newFreq;
		}
	}

	public Set<T> allValues() {
		Set<T> vals = freq4value.keySet();
		return vals;
	}

	public long totalOccurences() {
		long total = 0;
		for (T aVal: allValues()) {
			total += frequency(aVal);
		}
		return total;
	}

	public long frequency(T value) {
		Long freq = new Long(0);
		if (freq4value.containsKey(value)) {
			Number freqNumber = freq4value.get(value);
			if (freqNumber.getClass() == Integer.class) {
				// For some unfathomable reason, when a histogram is created from a
				// using fromMap(Map<String,Long>), the freq4value map actually
				// contains Integers for its values.
				//
				// I don't understand how this is even possible because a
				// Map<String,Integer> can't be cast as a Map<String,Long>.
				//
				// In any case, if that happens, simply convert the Integer
				// frequency to a Long one.
				//
				freq = new Long((Integer) freqNumber);
			} else {
				freq = (Long)freqNumber;
			}
		}
		return freq;
	}

	public double relativeFrequency(T value) {
		double relFreq = 1.0 * frequency(value) / totalOccurences();
		return relFreq;
	}

	public String relativeFrequency(T value, int numDecimals) {
		double percent = 100 * relativeFrequency(value);
		String format = "#";
		for (int ii=0; ii < numDecimals; ii++) {
			if (ii == 0) format += ".";
			format += "#";
		}
		String percentStr = new DecimalFormat(format).format(percent)+"%";
		
		return percentStr;
	}

	public long min() {
		return _min;
	}

	public long max() {
		return _max;
	}

	public Map<T,Long> toMap() {
		return freq4value;
	}

	public static <T> FrequencyHistogram fromMap(Map<T,Long> freqMap) {
		FrequencyHistogram<T> hist = new FrequencyHistogram<T>();
		hist.freq4value = freqMap;
		return hist;
	}
}
