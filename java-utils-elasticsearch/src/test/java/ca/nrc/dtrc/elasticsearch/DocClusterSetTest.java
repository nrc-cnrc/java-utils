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
		String indexName = "es-test";
		String docTypeName = "test-docs";
		DocClusterSet set = new DocClusterSet(indexName, docTypeName);
		
		//
		// Add some documents to particular clusters
		//
		String clusterName = "mentionsPlace";
		set.addToCluster(clusterName, "doc1");
		set.addToCluster(clusterName, "doc2");
		
		clusterName = "mentionsPerson";
		set.addToCluster(clusterName, "doc3");
		set.addToCluster(clusterName, "doc4");
		set.addToCluster(clusterName, "doc5");
		
		//
		// You can then retrieve particular clusters
		//
		DocCluster cluster = set.getCluster("mentionsPlace");
		
		//
		// You can then get some stats about the cluster. For example.
		//
		Set<String> ids = cluster.getDocIDs();
		int size = cluster.getSize();
		
		//
		// Note that the clusters only contain the IDs of documents.
		//
		// But the DocClusterSet does specify the ElasticSearch
		// index and document type that these documents were taken
		// from, and you can use that information to retrieve the full
		// content of the documents.
		//
		String esIndexName = set.getIndexName();
		String esDocTypeName = set.getDocTypeName();
		// You can retrieve the full content of those documents using
		// StreamlinedClient.getDocumentWithID(), passing it the
		// name of the index and 
		
	}
	
	/***********************
	 * VERIFICATION TESTS
	 ***********************/
	
	@Test
	public void test__DocClusterSet__HappyPath() throws Exception {
		DocClusterSet set = new DocClusterSet("test-index", "test-documents");
		
		String clusterName = "mentionsPlace";
		set.addToCluster(clusterName, "doc1");
		set.addToCluster(clusterName, "doc2");
		
		clusterName = "mentionsPerson";
		set.addToCluster(clusterName, "doc3");
		set.addToCluster(clusterName, "doc4");
		set.addToCluster(clusterName, "doc5");
		
		DocCluster cluster = set.getCluster("mentionsPlace");
		String[] expIDs = new String[] {"doc1", "doc2"};
		AssertHelpers.assertDeepEquals("", expIDs, cluster.getDocIDs());
		
		cluster = set.getCluster("mentionsPerson");
		expIDs = new String[] {"doc3", "doc4", "doc5"};
		AssertHelpers.assertDeepEquals("", expIDs, cluster.getDocIDs());
	}
}
