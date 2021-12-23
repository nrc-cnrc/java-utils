package ca.nrc.dtrc.elasticsearch.es5;

public class ScoreDistribution {
	Double minValue = null;
		public Double getMinValue() {return this.minValue;}
		public void setMinValue(Double _minValue) {this.minValue = _minValue;}
	
	Double maxValue = null;
		public Double getMaxValue() {return this.maxValue;}
		public void setMaxValue(Double _maxValue) {this.maxValue = _maxValue;}
		
	int n = 0;
		public int getN() {return this.n;}
		public void setN(int _n) {this.n = _n;}
		
	Double average = 0.0;
		public Double getAverage() {return this.average;}
		public void setAverage(Double _average) {this.average = _average;}
	
	Double variance = 0.0;
		public Double getVariance() {return this.variance;}
		public void setVariance(Double _variance) {this.variance = _variance;}
}
