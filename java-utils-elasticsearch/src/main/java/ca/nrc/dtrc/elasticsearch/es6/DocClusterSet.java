package ca.nrc.dtrc.elasticsearch.es6;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class DocClusterSet {
	
	public enum SortOrder {SIZE, COHESION};
	
	private Map<String,DocCluster> clusters = new HashMap<String,DocCluster>();
	
	private String indexName = null;
		public String getIndexName() {return indexName;}

	private String docTypeName = null;
		public String getDocTypeName() {return docTypeName;}
		
	public DocClusterSet() {
		initialize(null, null);
	}
	
	public DocClusterSet(String _indexName, String _docTypeName) {
		initialize(_indexName, _docTypeName);
	}
	
	public void initialize(String _indexName, String _docTypeName) {
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

	public List<String> getClusterNames() {
		return getClusterNames(SortOrder.SIZE);
	}

	public List<String> getClusterNames(SortOrder order) {
		List<String> names = new ArrayList<String>();
		names.addAll(clusters.keySet());
		
		if (order.equals(SortOrder.SIZE)) {
			Collections.sort(names, (String c1, String c2) -> getCluster(c2).getSize().compareTo(getCluster(c1).getSize()));
		} else if (order.equals(SortOrder.COHESION)) {
			Collections.sort(names, (String c1, String c2) -> getCluster(c2).getCohesion().compareTo(getCluster(c1).getCohesion()));			
		}
		

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

	public void addAll(DocClusterSet otherClusters) {
		for (String clusterName: otherClusters.getClusterNames()) {
			DocCluster cluster = otherClusters.getCluster(clusterName);
			this.add(clusterName, cluster);
		}
		
	}

	private void add(String origName, DocCluster cluster) {
		
		// Avoid cases where there is already a cluster by that name...
		List<String> existingClusters = getClusterNames();
		Integer suffixNum = -1;
		String newName = "";
		while (true) {
			String suffix = "_"+suffixNum.toString();
			if (suffixNum < 0) suffix = "";
			newName = origName + suffix;
			if (!existingClusters.contains(newName)) {
				break;
			} else {
				suffixNum++;
			}
		}
			
		this.clusters.put(newName, cluster);
	}

	public Collection<DocCluster> getClusters() {
		
		Collection<DocCluster> allClusters = clusters.values();
		return allClusters;
	}

}
