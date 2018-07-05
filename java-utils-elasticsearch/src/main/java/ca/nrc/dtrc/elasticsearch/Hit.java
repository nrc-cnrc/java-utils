package ca.nrc.dtrc.elasticsearch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

public class Hit<T extends Document> {
	private T document;
	private Double score;
	private Map<String, List<String>> snippets;
	
	/**
	 * Hit found by Elastic Search
	 * @param document Document found
	 * @param score Double score for the match
	 * @param snippets JsonNode containing the highlights returned from ElasticSearch
	 */
	public Hit(T document, Double score, JsonNode snippets) {		
		this.document = document;
		this.score = score;
		this.snippets = getSnippets(snippets);
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
