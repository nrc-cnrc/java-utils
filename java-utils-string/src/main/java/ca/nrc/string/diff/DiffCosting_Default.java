package ca.nrc.string.diff;

import java.util.List;

public class DiffCosting_Default extends DiffCosting {

	@Override
	public double cost(DiffResult diff) {
		double cost = 1.0 * numAffectedTokens(diff.origTokens, diff.revTokens, diff.transformations);
		return cost;
	}
}
