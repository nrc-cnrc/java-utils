package ca.nrc.dtrc.elasticsearch.es5;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import ca.nrc.dtrc.elasticsearch.es5.FieldDef.Types;

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
		Map<String,Object> map = fDef.toMap();
	}

	@Test
	public void test__toMap__AnalyzedTextField() {
		
		FieldDef fDef = new FieldDef(Types.text).setAnalyzer("en");
		Map<String,Object> gotMap = fDef.toMap();
		
		Map<String,Object> expMap = new HashMap<String,Object>();
	}
}
