package ca.nrc.dtrc.elasticsearch;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ca.nrc.datastructure.Pair;

//
// ElasticSearch does not support use of filters with MLT (More Like This) search.
// This class allows you to run a simple filter on the results of an MLT search.
//

public class MLT_HitsFilter {
	
	Set<Pair<String,String>> includeFields = new HashSet<Pair<String,String>>();
	Set<Pair<String,String>> excludeFields = new HashSet<Pair<String,String>>();
	
	public MLT_HitsFilter(String filterSpecs) {
		initialize(filterSpecs);
	}

	public MLT_HitsFilter() {
		initialize(null);
	}

	private void initialize(String filterSpecs) {
		// TODO Auto-generated method stub
		
	}

	public List<Document> filter(List<Document> origDocs) {
		List<Document> filteredDocs = origDocs;
		return filteredDocs;
	}

}
