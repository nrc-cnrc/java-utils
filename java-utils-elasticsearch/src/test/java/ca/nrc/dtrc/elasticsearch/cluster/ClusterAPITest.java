package ca.nrc.dtrc.elasticsearch.cluster;

import ca.nrc.dtrc.elasticsearch.DocCluster;
import ca.nrc.dtrc.elasticsearch.DocClusterSet;
import ca.nrc.dtrc.elasticsearch.ESFactory;
import ca.nrc.dtrc.elasticsearch.ESTestHelpers;
import static ca.nrc.dtrc.elasticsearch.ESTestHelpers.*;

import ca.nrc.testing.AssertHelpers;
import ca.nrc.testing.AssertString;
import org.junit.jupiter.api.Test;

import java.util.Set;

public abstract class ClusterAPITest {

	protected abstract int esVersion();

	@Test
	public void test__ClusterAPI__Synopsis() throws Exception {
		// Use ClusterAPI_v7 for clustering ES documents.
		// You should always obtain a ClusterAPI_v7 from a concrete ESFactory instance that is
		// designed to interact with the version of ESFactory you are running.
		ESFactory factory = new ESTestHelpers(esVersion()).makeHamletTestIndex();
		ClusterAPI clusterAPI = factory.clusterAPI();

		//
		// You can cluster a set of documents.
		// The  set of documents to be clustered is specified by a free-form query.
		//
		// For this example, we will use a ClusterAPI_v7  that is connected
		// to an index containing all the lines from Shakespeare's play 'Hamlet'
		//
		// We will cluster all the lines that are spoken by Hamlet
		//
		String query = "speaker:Hamlet";
		Integer maxDocs = 1000; // Only cluster the first 1000 hits. We recommend no less than 100

		// Specify the clustering algorithm.
		//
		// Possible values are: lingo (default), stc and kmeans.
		//
		// For details on each algorithm, see:
		//    http://doc.carrot2.org/#section.advanced-topics.fine-tuning.choosing-algorithm
		//
		String algName = "kmeans";

		String esDocTypeName = new PlayLine().getClass().getName();
		String[] useFields = new String[] {"longDescription"};
		DocClusterSet clusters = clusterAPI.clusterDocuments(query, esDocTypeName, useFields, algName, maxDocs);

		// You can then look at the various clusters...
		for (String clusterLabel: clusters.getClusterNames()) {
			DocCluster aCluster = clusters.getCluster(clusterLabel);
			// Get some info about the cluster
			Set<String> idsOfDocsInCluster = aCluster.getDocIDs();
			int size = aCluster.getSize();
			// and so on...
		}
	}

	@Test
	public void test__clusterDocumentJsonBody__HappyPath() throws Exception {
		ESFactory esFactory = new ESTestHelpers(esVersion()).makeEmptyTestIndex();
		ClusterAPI clusterAPI = esFactory.clusterAPI();

		String[] useFields = new String[] {"longDescription"};
		String gotJson = clusterAPI.clusterDocumentJsonBody("speaker:hamlet", "testdoc", useFields, "kmeans", 1000);
		String expJson =
				"{\"search_request\":{"+
				  "\"_source\":[\"longDescription\"],"+
		          "\"query\":"+
				    "{\"query_string\":"+
		              "{\"query\":\"speaker:hamlet\"}"+
				    "},"+
		            "\"size\":1000"+
				  "},"+
		          "\"query_hint\":\"\","+
				  "\"algorithm\":\"kmeans\","+
				  "\"field_mapping\":{"+
		            "\"content\":[\"_source.longDescription\"]"+
		          "}"+
		        "}";
		AssertString.assertStringEquals(expJson, gotJson);
	}

	// Note: Disabled for now because we can't get a version of carrot cluster
	//   plugin to install
	@Test
	public void test__clusterDocuments__HappyPath() throws Exception {
		ESFactory esFactory = new ESTestHelpers(esVersion()).makeHamletTestIndex();
		ClusterAPI clusterAPI = esFactory.clusterAPI();
		String query = "additionalFields.speaker:Hamlet";
		Integer maxDocs = 1000;
		String algName = "stc";
		String esDocTypeName = new PlayLine().getClass().getName();
		String[] useFields = new String[] {"content"};
		DocClusterSet clusters = clusterAPI.clusterDocuments(query, esDocTypeName, useFields, algName, maxDocs);

		String[] expClusterNamesSuperset = new String[] {
				"Ay",
				"Dost Thou", "Dost Thou Hear",
				"Enter", "Enter King CLAUDIUS", "\"Enter King CLAUDIUS, Queen GERTRUDE\"", "Eyes",
				"Father",
				"Good Friends", "GUILDENSTERN, ROSENCRANTZ",
				"Heaven", "Hold", "Horatio",
				"King", "King CLAUDIUS", "Know",
				"Lord", "Love",
				"Matter", "Mother",
				"Nay",
				"Other Topics",
				"Play", "Players",
				"QUEEN GERTRUDE",
				"ROSENCRANTZ, GUILDENSTERN", "ROSENCRANTZ and GUILDENSTERN",
				"Shall", "Sir", "Soul", "Speak",
				"Thou", "Thee", "Thy", "Tis"
		};
		Object[] gotClusterNames =  clusters.getClusterNames().toArray();

		// The algorithm does not seem to be completely deterministic and the exact
		// set of clusters returned is not always exactly the same from run to run.
		//
		// But they tend to be very similar, so we can just check to make sure that
		// the clusters we get are a subset of the set of clusters we typically see.
		//
		// If this assertion fails, check if the new cluster that was generated makes sense,
		// and if it does, then add it to the superset of expected clusters.
		//
		AssertHelpers.assertContainsAll("Cluster names not as expected\nNOTE: The exact clusters produced in this test are non-deterministic. If the test fail, it may be because you need to add new words to the expectation.", expClusterNamesSuperset, gotClusterNames);

		String clusterName = "Heaven";
		String[] expIDs = new String[] {
				"1.2.143", "1.2.144", "1.2.184", "1.4.44", "1.4.94", "1.5.109", "1.5.185", "1.5.97",
				"2.2.434", "2.2.595", "3.2.128", "3.3.81", "3.3.85", "3.3.96", "3.4.165",
				"3.4.55", "4.3.36", "5.2.52", "5.2.344", "5.2.357"
		};
		String[] gotIDs = clusters.getCluster("Heaven").getDocIDs().toArray(new String[]{});
		AssertHelpers.assertContainsAll("Cluster IDs not as expected", expIDs, gotIDs);
	}
}
