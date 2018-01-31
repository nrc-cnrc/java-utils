package ca.nrc.dtrc.elasticsearch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ca.nrc.json.JSONUtils;
import ca.nrc.json.PrettyPrinter;
import ca.nrc.testing.AssertHelpers;
import ca.nrc.datastructure.Pair;
import ca.nrc.file.ResourceGetter;

public class ESTestHelpers {
	
	private static final PlayLine playLinePrototype = new PlayLine();
	
	public static class PlayLine extends Document {
		public Integer line_id = -1;
		public String play_name = "";
        public Integer speech_number = -1;
        public String line_number = "";
        public String speaker = "";
        public String text_entry = "";
        
        public PlayLine() {}
        
        public PlayLine(String _text_entry) {
        	this.text_entry = _text_entry;
        }

		@Override
		public String getKeyFieldName() {
			return "line_number";
		}

		@Override
		public String getKey() {
			return line_number;
		}
	}	

	private static Boolean skipTests = null;
	
	// This test index is guaranteed to be initially empty
	public static final String emptyTestIndex = "es-test";
	
	// This test index is guaranteed to initially contain 
	// all lines from the play 'Hamlet' by Shakespeare
	public static final String hamletTestIndex = "es-test-hamlet";
	
	public static void skipTestsUnlessESIsRunning() throws Exception {
		if (skipTests == null) {
			// Check to see of ElasticSearch is running.
			try {
				StreamlinedClient client = new StreamlinedClient(hamletTestIndex);
				client.listFirstNDocuments(playLinePrototype, 1);
				// Is running... so DON'T skip the tests
				skipTests = false;
			} catch (ElasticSearchException exc) {
				skipTests = false;
				if (exc.getMessage().startsWith("Failed to connect to ElasticSearch server")) {
					// We were unable to connect to the ElasticSearch server.
					// Probably means that the server is not running, so 
					// skip all remaining tests that require ElasticSearch to run
					skipTests = true;
					Assert.fail(
							  "ElasticSearch is either not installed or is not running.\n\n"
							+ "If you want to test the ES utilities, install ES and make sure to start it using the command line 'elasticsearch'.\n\n"
							+ "For now, skipping all remaining ElasticSearch tests.");
				}
			}
		}
		org.junit.Assume.assumeFalse(skipTests);		
	}
	
	public static StreamlinedClient makeEmptyTestClient() throws IOException, ElasticSearchException, InterruptedException {
		// Put a one second delay after each transaction, to give ES time to synchronize all the nodes.
		double sleepSecs = 1.0;
		StreamlinedClient client = new StreamlinedClient(emptyTestIndex, sleepSecs);
		client.deleteIndex();
		
		return client;
	}

	public static StreamlinedClient makeHamletTestClient() throws IOException, ElasticSearchException, InterruptedException {
		// Put a two second delay after each transaction, to give ES time to synchronize all the nodes.
		double sleepSecs = 2.0;
		StreamlinedClient client = new StreamlinedClient(hamletTestIndex, sleepSecs);
		client.clearIndex(false);
		
		String fPath = ResourceGetter.getResourcePath("test_data/ca/nrc/dtrc/elasticsearch/hamlet.json");
		client.bulk(fPath, PlayLine.class);
		
		return client;
	}

	public static void assertIndexIsEmpty(String indexName) throws IOException, ElasticSearchException, InterruptedException {
		StreamlinedClient client = new StreamlinedClient(indexName);
		assertIndexIsEmpty(client);
	}

	public static void assertIndexIsEmpty(StreamlinedClient client) throws IOException, ElasticSearchException, InterruptedException {
		int numHits = 0;
		try {
			client.listAll(new PlayLine());
		} catch (Exception exc) {
			// If exception thrown, it means the index does not exist.
			// So consider that it's OK (non-existant == empty)
		}
		
		Assert.assertEquals("Index should have been empty.", 0, numHits);	
	}

	private static void assertResponseEquals(Map<String,Object> expRespObject, String gotJsonResp) throws JsonParseException, JsonMappingException, IOException {
		Map<String,Object> gotRespObject = new HashMap<String,Object>();
		gotRespObject = new ObjectMapper().readValue(gotJsonResp, gotRespObject.getClass());
		AssertHelpers.assertDeepEquals("", expRespObject, gotRespObject);		
	}

	public static void assertNoError(String jsonResponse) throws JsonParseException, JsonMappingException, IOException {
		Map<String,Object> respObject = JSONUtils.json2ObjectMap(jsonResponse);		
		Assert.assertFalse("The jsonResponse should not have been an error.\nBut it was:\n"+jsonResponse, respObject.containsKey("error"));
		
	}

	public static <T extends Document> void  assertIndexSizeEquals(String indexName, int expNumDocs, T docPrototype) throws IOException, ElasticSearchException, InterruptedException {
		StreamlinedClient client = new StreamlinedClient(indexName);
		SearchResults<T> gotResults = client.listAll(docPrototype);		
		
		int gotNumDocs = 0;
		Iterator<Pair<T,Double>> iter = gotResults.iterator();		
		while (iter.hasNext()) {
			gotNumDocs++;
			iter.next();
		}
		
		Assert.assertEquals("Number of documents in index "+indexName+" was not as expected", expNumDocs, gotNumDocs);	
		
	}

	public static void assertIndexContainsDoc(String indexName, String esDocType, Document expDoc) throws ElasticSearchException, IOException, InterruptedException {
		StreamlinedClient client = new StreamlinedClient(indexName);
		Document gotDoc = client.getDocumentWithID(expDoc.getKey(), expDoc.getClass());
		AssertHelpers.assertDeepEquals("Index "+indexName+" did not contain the expected document", expDoc, gotDoc);
	}

	public static void assertIndexContainsDoc(String indexName, Document expDocument) {
		Assert.fail("Implement this assertion");
	}	
}
