package ca.nrc.dtrc.stats;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class FrequencyHistogram<T> {
	
	Map<T,Long> freq4value = new HashMap<T,Long>();
	Long _min = null;
	Long _max = null;

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
		long freq = 0;
		if (freq4value.containsKey(value)) {
			freq = freq4value.get(value);
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
}
