package ca.nrc.dtrc.elasticsearch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

public class Hit<T extends Document> {
	public T document;
	public Double score;
		public void setScore(Double _score) {this.score = _score;}
	public Map<String, List<String>> snippets;
	
	/**
	 * Hit found by Elastic Search
	 * @param _document Document found
	 * @param _score Double score for the match
	 * @param _snippets JsonNode containing the highlights returned from ElasticSearch
	 */
	public Hit(T _document, Double _score, JsonNode _snippets) {		
		initialize(_document, _score, _snippets);
	}

	private void initialize(T _document, Double _score, JsonNode _snippets) {
		this.document = _document;
		this.score = _score;
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

	public Map<String, List<String>> getSnippets() {
		return snippets;
	}		
	
	private Map<String, List<String>> getSnippets(JsonNode highlightNode){
		Map<String, List<String>> snippets = new HashMap<String, List<String>>();
		
		if(null != highlightNode) {
			Iterator<Map.Entry<String,JsonNode>> itEntries = highlightNode.fields();
			while(itEntries.hasNext()) {
				List<String> snippetsField = new ArrayList<>();
				Map.Entry<String,JsonNode> entry = itEntries.next();
				Iterator<JsonNode> itElements = entry.getValue().elements();
				while(itElements.hasNext()) {
					snippetsField.add(itElements.next().asText());
				}
				snippets.put(entry.getKey(), snippetsField);
			}
		}
		
		return snippets;
	}
}
