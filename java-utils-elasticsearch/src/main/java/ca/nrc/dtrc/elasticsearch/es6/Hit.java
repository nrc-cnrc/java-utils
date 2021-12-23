package ca.nrc.dtrc.elasticsearch.es6;

import org.json.JSONObject;

public class Hit<T extends Document> {
	public T document;
	public Double score = 0.0;
		public void setScore(Double _score) {this.score = _score;}
	public JSONObject snippets;
	
	/**
	 * Hit found by Elastic Search
	 * @param _document Document found
	 * @param _score Double score for the match
	 * @param _snippets JsonNode containing the highlights returned from ElasticSearch
	 */
	public Hit(T _document, Double _score, JSONObject _snippets) {
		initialize(_document, _score, _snippets);
	}

	public Hit(T _document) {
		initialize(_document, null, null);
	}
	
	private void initialize(T _document, Double _score, JSONObject _snippets) {
		this.document = _document;
		if (_score != null) this.score = _score;
		this.snippets = getSnippets(_snippets);
	}

	public Hit() {
		initialize(null, null, null);
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
