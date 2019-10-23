package ca.nrc.dtrc.stats;

import org.apache.commons.math3.stat.StatUtils;

import ca.nrc.dtrc.stats.HistogramException;

public class Histogram {

	public int numBins = 0;
	public Bin[] bins = new Bin[0];

	Double startBinsAt = null;
	Double endBinsAt = null;
	double binLength = 0.0;
	
	
	public Histogram() {
		initialize(null, null, null);
	}
	
	public Histogram(int _numBins) {
		initialize(_numBins, null, null);
	}

	public Histogram(int _numBins, Double _startBinsAt, Double _endBinsAt) {
		initialize(_numBins, _startBinsAt, _endBinsAt);
	}

	private void initialize(Integer _numBins, Double _startBinsAt, Double _endBinsAt) {
		if (_numBins == null) {
			_numBins = 10;
		}
		this.startBinsAt = _startBinsAt;
		this.endBinsAt = _endBinsAt;
		this.numBins = _numBins;
		this.bins = new Bin[_numBins];
	}

	public void load(double[] data) throws HistogramException {
		if (startBinsAt == null) {
			startBinsAt = StatUtils.min(data);
		}
		if (endBinsAt == null) {
			endBinsAt = StatUtils.max(data);		
		}
		this.binLength = (endBinsAt - startBinsAt) / numBins;
		
		for (int ii=0; ii < numBins; ii++) {
			
			double binStart = -1.0;
			if (ii == 0) {
				binStart = startBinsAt;
			} else {
				binStart = bins[ii-1].end;
			}

			double binEnd = -1.0;
			if (ii == bins.length-1) {
				binEnd = endBinsAt; 
			} else {
				binEnd = binStart + binLength;
			}
			
			bins[ii] = new Bin(binStart, binEnd);
		}
		
		for (double datum: data) {
			Bin foundBin = null;
			for (Bin aBin: bins) {
				if (datum >= aBin.start && datum <= aBin.end) {
					aBin.freq++;
					foundBin = aBin;
					break;
				}
			}
			if (foundBin == null) {
				throw new HistogramException("Datum "+datum+" was higher than the maxium bin "+endBinsAt);
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
		
		public String toString() {
			String toS = "["+start+","+end+"]: "+freq;
			return toS;
		}
	}
}
