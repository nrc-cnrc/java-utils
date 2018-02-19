package ca.nrc.dtrc.elasticsearch;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class DocClusterSet {
	
	private Map<String,DocCluster> clusters = new HashMap<String,DocCluster>();
	
	private String indexName = null;
		public String getIndexName() {return indexName;}

	private String docTypeName = null;
		public String getDocTypeName() {return docTypeName;}
		
	public DocClusterSet(String _indexName, String _docTypeName) {
		this.indexName = _indexName;
		this.docTypeName = _docTypeName;
	}

	public void addToCluster(String clusterName, String docID) {
		DocCluster cluster = null;
		if (clusters.containsKey(clusterName)) {
			cluster = clusters.get(clusterName);
		} else {
			cluster = new DocCluster();
			clusters.put(clusterName, cluster);
		}
		cluster.addDocID(docID);
	}

	@JsonIgnore
	public DocCluster getCluster(String clusterName) {
		DocCluster cluster = null;
		if (clusters.containsKey(clusterName)) {
			cluster = clusters.get(clusterName);
		}
		return cluster;
	}

	public Set<String> getClusterNames() {
		Set<String> names = clusters.keySet();
		return names;
	}

	public int getTotalDocs() {
		return getAllDocIDs().size();
	}
	
	public Set<String> getAllDocIDs() {
		Set<String> uniqueDocIDs = new HashSet<String>();
		for (String clusterName: getClusterNames()) {
			uniqueDocIDs.addAll(getCluster(clusterName).getDocIDs());
		}
		
		return uniqueDocIDs;
	}

}
