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

	/**
	 * Default implementation computes the cost as the total number 
	 * of tokens affected by the diff.
	 * 
	 * @param tokens1
	 * @param tokens2
	 * @param diff
	 * @return
	 */
	public abstract double cost(String[] tokens1, String[] tokens2, 
			List<StringTransformation> diff);
	
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
