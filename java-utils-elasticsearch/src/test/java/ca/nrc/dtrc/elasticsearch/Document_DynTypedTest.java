package ca.nrc.dtrc.elasticsearch;

import static org.junit.Assert.*;

import org.junit.Test;

import ca.nrc.testing.AssertHelpers;

public class Document_DynTypedTest {
	
	/***************************
	 * DOCUMENTATION TESTS
	 ***************************/

	@Test
	public void test__Document_DynTyped__Synopsis() throws Exception {
		// This class allows you to create an ElasticSearch indexable
		// document without requiring you to define the fields at
		// compile time.
		//
		// All you need to do is to specify the name of the field that
		// serves as the key for the documen.

		//
		// First create a document with just the Key field
		//
		String idFieldName = "part_number";
		Document_DynTyped doc = new Document_DynTyped(idFieldName, "X18D98KL9");
		
		//
		// Then add some field values
		//
		doc.setField("name", "6in screw");
		doc.setField("weight_grams", 0.4);
		
		//
		// You can then ask the document for its key
		//
		String key = doc.getKey();
		
		// 
		// Or the value of some of its fields
		//
		Double weight = (Double) doc.getField("weight_grams");
		
		//
		// If you ask for a field that does not exist, you get
		// an exception
		// 
		try {
			String color = (String)doc.getField("color");
			fail("You should never get here, because above line raises a DocumentException");
		} catch (DocumentException exc) {
			// Nothing to do here... we expected that exception
		}
	}
	
	/***************************
	 * VERIFICATON TESTS
	 ***************************/
	
	@Test
	public void test__Document_DynTyped__HappyPath() throws Exception {
		String idFieldName = "part_number";
		Document_DynTyped doc = new Document_DynTyped(idFieldName, "X18D98KL9");
		doc.setField("name", "6in screw");
		doc.setField("weight_grams", 0.4);
		
		String key = doc.getKey();
		AssertHelpers.assertDeepEquals("", key, "X18D98KL9");

		String name = (String) doc.getField("name");
		AssertHelpers.assertDeepEquals("", name, "6in screw");
		
		
		Double weight = (Double) doc.getField("weight_grams");
		AssertHelpers.assertDeepEquals("", weight, 0.4);		
	}
}
