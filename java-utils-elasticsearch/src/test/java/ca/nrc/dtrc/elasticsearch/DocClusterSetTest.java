package ca.nrc.dtrc.elasticsearch;

import java.util.List;
import java.util.Set;

import org.junit.Test;

import ca.nrc.dtrc.elasticsearch.ESTestHelpers.SimpleDoc;
import ca.nrc.testing.AssertHelpers;

public class DocClusterSetTest {
	
	/***********************
	 * DOCUMENTATION TESTS
	 ***********************/
	
	@SuppressWarnings("unused")
	@Test
	public void test__DocClusterSet__Synopsis() {
		// Use a DocClusterSet to create a collection of 
		// document clusters.
		//
		// First create an instance...
		//
		DocClusterSet set = new DocClusterSet();
		
		//
		// Add some documents to particular clusters
		//
		String clusterName = "mentionsPlace";
		set.addToCluster(clusterName, new SimpleDoc("doc1", "I live in Montreal"));
		set.addToCluster(clusterName, new SimpleDoc("doc2", "I traveled through Africa"));
		
		clusterName = "mentionsPerson";
		set.addToCluster(clusterName, new SimpleDoc("doc3", "Homer Simpson is a cartoon character"));
		set.addToCluster(clusterName, new SimpleDoc("doc4", "Barack Obama was elected President of the USA."));
		set.addToCluster(clusterName, new SimpleDoc("doc5", "I will speak with John Smith about this."));
		
		//
		// You can then retrieve particular clusters
		//
		DocCluster cluster = set.getCluster("mentionsPlace");
		
		//
		// You can then get some stats about the cluster. For example.
		//
		Set<String> ids = cluster.getDocIDs();
		int size = cluster.getSize();
		
	}
	
	/***********************
	 * VERIFICATION TESTS
	 ***********************/
	
	@Test
	public void test__DocClusterSet__HappyPath() throws Exception {
		DocClusterSet set = new DocClusterSet();
		
		String clusterName = "mentionsPlace";
		set.addToCluster(clusterName, new SimpleDoc("doc1", "I live in Montreal"));
		set.addToCluster(clusterName, new SimpleDoc("doc2", "I traveled through Africa"));
		
		clusterName = "mentionsPerson";
		set.addToCluster(clusterName, new SimpleDoc("doc3", "Homer Simpson is a cartoon character"));
		set.addToCluster(clusterName, new SimpleDoc("doc4", "Barack Obama was elected President of the USA."));
		set.addToCluster(clusterName, new SimpleDoc("doc5", "I will speak with John Smith about this."));
		
		DocCluster cluster = set.getCluster("mentionsPlace");
		String[] expIDs = new String[] {"doc1", "doc2"};
		AssertHelpers.assertDeepEquals("", expIDs, cluster.getDocIDs());
		
		cluster = set.getCluster("mentionsPerson");
		expIDs = new String[] {"doc3", "doc4", "doc5"};
		AssertHelpers.assertDeepEquals("", expIDs, cluster.getDocIDs());
	}
}
