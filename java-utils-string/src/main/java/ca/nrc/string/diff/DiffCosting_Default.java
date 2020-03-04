package ca.nrc.string.diff;

import java.util.List;

public class DiffCosting_Default extends DiffCosting {

	@Override
	public double cost(String[] tokens1, String[] tokens2, 
			List<StringTransformation> diff) {
		double cost = 1.0 * numAffectedTokens(tokens1, tokens2, diff);
		return cost;
	}

}
