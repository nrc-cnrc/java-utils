package ca.nrc.dtrc.elasticsearch;

import ca.nrc.testing.AssertString;
import org.json.JSONObject;
import org.junit.Test;

import ca.nrc.dtrc.elasticsearch.FieldDef.Types;

public class FieldDefTest {

	
	/*******************************
	 * DOCUMENTATION TESTS
	 *******************************/
	
	@Test
	public void test__FieldDef__Synopsis() {
		
		// Use a FieldDef to define the characteristics of a field of a 
		// document type in ElasticSearch
		//
		String fldName = "book_summary";
		Types fldType = FieldDef.Types.text;
		
		// This particular text field is NOT analyzed (meaning, no stemming, no stopword removal)
		FieldDef fDef = new FieldDef(fldType);
		
		// If we want this text field to be stemmed and to have stopwords removed, we have to do this
		fDef.setAnalyzer("english");
		
		// Once you have created a FieldDef, you can convert it to a Map<String,Object>, which can then
		// be passed to ElasticSearch to provide the fields's definition
		JSONObject json = fDef.toJson();
	}

	@Test
	public void test__toJson__AnalyzedTextField() throws Exception {
		
		FieldDef fDef = new FieldDef(Types.text).setAnalyzer("en");
		JSONObject gotJSON= fDef.toJson();
		
		JSONObject expJSON = new JSONObject()
			.put("analyzer", "english")
			.put("type", "text")
		;
		AssertString.assertStringEquals(
			"JSON gnerated for field was not as expected",
			expJSON.toString(), gotJSON.toString()
		);
	}
}
