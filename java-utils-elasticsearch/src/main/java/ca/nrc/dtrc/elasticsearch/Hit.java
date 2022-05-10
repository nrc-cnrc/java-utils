package ca.nrc.dtrc.elasticsearch;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Hit found by Elastic Search
 */
public class Hit<T extends Document> {
	public T document;
	public Double score = 0.0;
		public void setScore(Double _score) {this.score = _score;}

	// When one or more sort criteria are provided for the list of hits, this
	// will provide the list of values that were used for sorting.
	public JSONArray sortValues = null;
	public JSONObject snippets;

	public Hit() {
		initialize(null, null, null, null);
	}


	public Hit(T _document) {
		initialize(_document, null, null, (JSONArray)null);
	}

	public Hit(T _document, Double _score, JSONObject _snippets) {
		initialize(_document, _score, _snippets, (JSONArray)null);
	}

	public Hit(T _document, Double _score, JSONObject _snippets,
		JSONArray _sortValues) {
		initialize(_document, _score, _snippets, _sortValues);
	}

	private void initialize(T _document, Double _score, JSONObject _snippets,
		JSONArray _sortValues) {
		this.document = _document;
		if (_score != null) this.score = _score;
		this.snippets = getSnippets(_snippets);
		this.sortValues = _sortValues;
	}

	public T getDocument() {
		return document;
	}

	public Double getScore() {
		return score;
	}

	public JSONObject getSnippets() {
		return snippets;
	}		
	
	private JSONObject getSnippets(JSONObject highlightNode){
		JSONObject snippets = highlightNode;

		return snippets;
	}
}
