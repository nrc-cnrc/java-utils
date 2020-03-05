package ca.nrc.string.diff;

import java.util.List;

/**
 * This class is used to assign a "cost" to the transformations 
 * required to transform one string into another.
 * 
 * @author desilets
 *
 */
public abstract class DiffCosting {
	
	public static final double INFINITE = 1000000;
	

	public abstract double cost(DiffResult diff);

//	public double cost(String[] tokens1, String[] tokens2, 
//			List<StringTransformation> diff) {
//		DiffResult dRes = new DiffResult(tokens1, tokens2, diff);
//		double _cost = cost(dRes);
//		
//		return _cost;
//	}
	
	
	protected double numAffectedTokens(String[] tokens1, String[] tokens2, 
			List<StringTransformation> diff) {
		
		double numAffected = 0;
		
		for (StringTransformation transf: diff) {
			numAffected += transf.origTokens.length;
			numAffected += transf.revisedTokens.length;
		}
		
		return numAffected;
	}
	
}
