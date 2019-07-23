package ca.nrc.dtrc.elasticsearch;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ca.nrc.datastructure.Pair;
import ca.nrc.dtrc.elasticsearch.HitFilter;

//
// ElasticSearch does not support use of filters with MLT (More Like This) search.
// This class allows you to run a simple filter on the results of an MLT search.
//

public class HitFilter<T extends Document> {
	
	Set<Pair<String,String>> terms = new HashSet<Pair<String,String>>();

	boolean filterIsPositive = true;
	boolean termsAreANDed = true;
	
	public HitFilter(String filterSpecs) {
		initialize(filterSpecs);
	}

	public HitFilter() {
		initialize(null);
	}

	private void initialize(String filterSpecs) {
		if (filterSpecs != null) {
			Matcher matcher = 
					Pattern.compile("^\\s*([+-])?\\s*((AND|OR)\\s+)?([\\s\\S]+)$", Pattern.CASE_INSENSITIVE)
					.matcher(filterSpecs);
			if (matcher.matches()) {
				String plusMinus = matcher.group(1);
				String andOr = matcher.group(2);
				if (plusMinus != null && plusMinus.equals("-")) filterIsPositive = false;
				if (andOr != null && andOr.toLowerCase().startsWith("or")) termsAreANDed = false; 
				
				String termsStr = matcher.group(4);
				matcher = Pattern.compile("([^\\s:]+):([^\"\\s]+|\"[^\"]+\")").matcher(termsStr);
				while (matcher.find()) {
					String fieldName = matcher.group(1);
					String fieldValue = matcher.group(2);
					fieldValue =fieldValue.replaceAll("\"", "");
					this.terms.add(Pair.of(fieldName,fieldValue));
				}
				
			}
		}
		
	}

	public boolean keep(Hit<T> aHit) throws HitFilterException {
		
		Document doc = aHit.getDocument();
		int totalTermsMatched = 0;
		for (Pair<String,String> aTerm: terms) {
			String fldName = aTerm.getFirst();
			String termFldValue = aTerm.getSecond();
			String docFldValue = null;
			
			try {
				Object docFldValueObj = doc.getField(fldName, false);
				if (docFldValueObj != null) { docFldValue = docFldValueObj.toString(); }
			} catch (DocumentException e) {
				throw new HitFilterException("Could not obtain value of field '"+fldName+"'", e);
			}
			if (termFldValue.equals(docFldValue)) {totalTermsMatched++;}
		}
		
		boolean conditionIsMet;
		if (termsAreANDed) {
			conditionIsMet = (totalTermsMatched == terms.size());
		} else {
			// Terms are ORed
			conditionIsMet = (totalTermsMatched > 0);
		}
		
		boolean _keep = conditionIsMet;
		if (!filterIsPositive) {
			_keep = !conditionIsMet;
		}
		
		return _keep;
	}

}
