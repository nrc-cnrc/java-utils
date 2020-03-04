package ca.nrc.string.diff;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Diff costing scheme that uses synonym sets to assign lower cost to 
 * transformations that replace a word with a synonym.
 * 
 * @author desilets
 *
 */
public class DiffCosting_SynSets extends DiffCosting {

	Set<Set<String>> synonyms = null;
	
	public  DiffCosting_SynSets() {
		if (synonyms == null) {
			synonyms = new HashSet<Set<String>>();
		}
	}
	
	public void addSynSet(String[] syns) {
		Set<String> synSet = new HashSet<String>();
		for (String aSyn: syns) {
			synSet.add(aSyn.toLowerCase());
		}
		synonyms.add(synSet);
		
	}
	
	@Override
	public double cost(String[] tokens1, String[] tokens2, 
			List<StringTransformation> diff) {
		
		
		double _cost = 0.0;
		for (StringTransformation transf: diff) {
			String orig = String.join(" ", transf.origTokens).toLowerCase();
			String rev = String.join(" ", transf.revisedTokens).toLowerCase();
			boolean isSynonym = false;
			for (Set<String> synSet: synonyms) {
				if (synSet.contains(orig) && synSet.contains(rev)) {
					isSynonym = true;
					break;
				}
			}
			
			int numAffected = transf.origTokens.length + transf.revisedTokens.length;
			
			if (isSynonym) {
				_cost += 0.1 * numAffected;
			} else {
				_cost += 1.0 * numAffected;
			}
		}
		
		return _cost;
	}
	
}
	
