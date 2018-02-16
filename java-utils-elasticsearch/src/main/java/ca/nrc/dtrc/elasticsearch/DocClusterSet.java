package ca.nrc.dtrc.elasticsearch;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class DocClusterSet {
	
	private Map<String,DocCluster> clusters = new HashMap<String,DocCluster>();

	public void addToCluster(String clusterName, Document doc) {
		DocCluster cluster = null;
		if (clusters.containsKey(clusterName)) {
			cluster = clusters.get(clusterName);
		} else {
			cluster = new DocCluster();
			clusters.put(clusterName, cluster);
		}
		cluster.addDocID(doc.getKey());
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

}
