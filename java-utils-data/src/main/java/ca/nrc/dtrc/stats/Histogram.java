package ca.nrc.dtrc.stats;

import org.apache.commons.math3.stat.StatUtils;

public class Histogram {

	public int numBins = 0;
	public Bin[] bins = new Bin[0];

	double min = 0.0;
	double max = 0.0;
	double binLength = 0.0;
	
	
	public Histogram() {
		initialize(null);
	}
	
	public Histogram(int _numBins) {
		initialize(_numBins);
	}

	private void initialize(Integer _numBins) {
		if (_numBins == null) {
			_numBins = 10;
		}
		this.numBins = _numBins;
		this.bins = new Bin[_numBins];
	}

	public void load(double[] data) {
		this.min = StatUtils.min(data);
		this.max = StatUtils.max(data);		
		this.binLength = (max - min) / numBins;
		
		for (int ii=0; ii < numBins; ii++) {
			double binStart = min + ii * binLength;
			double binEnd = binStart + binLength;
			Bin aBin = new Bin(binStart, binEnd);
			bins[ii] = aBin;
		}
		
		for (double datum: data) {
			for (Bin aBin: bins) {
				if (datum >= aBin.start && datum <= aBin.end) {
					aBin.freq++;
					break;
				}
			}
		}
	}

	public static class Bin {
		public Double start = null;
		public Double end = null;
		public long freq = 0;

		public Bin(double binStart, double binEnd) {
			initialize(binStart, binEnd, null);
		}

		public Bin(double binStart, double binEnd, Long binFreq) {
			initialize(binStart, binEnd, binFreq);
		}

		public Bin(double binStart, double binEnd, long binFreq) {
			initialize(binStart, binEnd, new Long(binFreq));
		}

		private void initialize(double binStart, double binEnd, Long binFreq) {
			start = binStart;
			end = binEnd;
			
			if (binFreq != null) {
				freq = binFreq;
			}
		}
	}
}
