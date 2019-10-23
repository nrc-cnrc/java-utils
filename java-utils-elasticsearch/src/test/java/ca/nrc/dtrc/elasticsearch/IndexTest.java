package ca.nrc.dtrc.elasticsearch;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ca.nrc.testing.AssertHelpers;

public class IndexTest {
	
	public String indexName = null;
	
	@Before
	public void seUp() throws Exception {
		StreamlinedClient esClient = ESTestHelpers.makeEmptyTestClient();
		Document doc = new Document().setId("hello").setContent("hello world");
		esClient.putDocument(doc);
		indexName = esClient.getIndexName();
		
		ESTestHelpers.sleepShortTime();
	}
	
	/////////////////////////////////////
	// DOCUMENTATION TESTS
	/////////////////////////////////////

	@Test
	public void test__Index__Synopsis() throws Exception {
		//
		// Use Index class to create and configure 
		// indices
		//
		Index index = new Index(indexName);
		
		// Get the index definition
		IndexDef iDef = index.getDefinition();
		
		// If you want to change an existing index's settings, you 
		// need to "force" it. If you don't, an exception will be raised
		//
		try {
			iDef.totalFieldsLimit = 1999;
			index.setDefinition(iDef);
		} catch (IndexException e) {
			// OK, this is expected
		}

		// Correct way to force setting changes to an existing index
		//
		boolean force = true;
		index.setDefinition(iDef, force);
		
		
	}
	
	/////////////////////////////////////
	// VERIFICATION TESTS
	/////////////////////////////////////
	

	@Test
	public void test__putDefinition_getDefinition__HappyPath() throws Exception  {
		Index index = new Index(indexName);
		IndexDef gotDef = index.getDefinition();
		assertTotalFieldsLimitEquals("Initial setting not as expected", gotDef, null);
		assertMappingsEqual("Initial mappings not as expected", gotDef, null);
		
		
		
		IndexDef newDef = 
				new IndexDef(indexName)
				.setTotalFieldsLimit(9999)
				;
		newDef.getTypeDef("some_es_doctype").getFieldDef("content").setType(FieldDef.Types.text);		
		
		index.setDefinition(newDef, true);

		gotDef = index.getDefinition();
		assertTotalFieldsLimitEquals("Modified setting not as expected", gotDef, 9999);
		assertMappingsEqual("Initial mappings not as expected", gotDef, null);
	}	
	
	
	private void assertMappingsEqual(String mess, IndexDef gotDef, Map<String,Object> expMappings) throws Exception {
		if (expMappings == null) {
			expMappings = new HashMap<String,Object>();
			expMappings.put("mappings", new HashMap<String,Object>());
		}
		Map<String,Object> gotMappings = gotDef.indexMappings();
		AssertHelpers.assertDeepEquals("The index's mappings were not as expected", 
				expMappings, gotMappings);
		
		return;
	}

	private void assertTotalFieldsLimitEquals(String mess, IndexDef gotDef, Integer expLimit) {
		Integer gotLimit = gotDef.totalFieldsLimit;		
		Assert.assertEquals("", expLimit, gotLimit);
	}

	@Test(expected=IndexException.class)
	public void test__putSettings__PuttingUnforcedSettingsToExistingIndex__RaisesException() throws ElasticSearchException  {
		Index index = new Index(indexName);
		IndexDef def = new IndexDef(indexName);
		index.setDefinition(def);
	}	
	
	
}
