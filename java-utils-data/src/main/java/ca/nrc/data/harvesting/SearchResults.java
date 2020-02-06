package ca.nrc.data.harvesting;

import java.util.ArrayList;
import java.util.List;

public class SearchResults {
	public long estTotalHits = 0;

	public List<SearchEngine.Hit> retrievedHits = new ArrayList<SearchEngine.Hit>();
}
