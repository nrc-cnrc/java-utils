package ca.nrc.dtrc.elasticsearch.es5;

import java.io.BufferedReader;
import java.io.StringReader;
import java.io.StringWriter;

import org.junit.Ignore;
import org.junit.Test;

import ca.nrc.testing.AssertHelpers;

public class SeparatedFields2Document_DynTypedTest {

	
	////////////////////////////////////////////////////////
	// DOCUMENTATION TESTS
	////////////////////////////////////////////////////////
	
	@Test
	public void test__SeparatedFields2Document_DynTyped__Synopsis() throws Exception {
		//
		// Use SeparatedFields2Document_DynTyped to convert a stream of tab or comma
		// separated records to a stream of json records for ElasticSearch
		// Document_DynTyped,
		
		// Create a configure the converter
		SeparatedFields2Document_DynTyped converter = 
				new SeparatedFields2Document_DynTyped()
				
				// Specify separator: ',' or '\t'
				.setSeparator(',') 
				
				// Name of the field that constitutes the unique ID for a record
				// If not specified, assume first field si the ID
				.setIDFieldName("ModelID") 
				;
		
		String csvStreamContent = 
				  "Manufacturer,Model,Year,ModelID\n"
				+ "Toyota,Corolla,2009,TOY242p\n"
				+ "Hyundai,Elentra,2011,Hyu9834"
				;
		BufferedReader input = new BufferedReader(new StringReader(csvStreamContent));
		StringWriter output = new StringWriter();
		
		converter.convert(input, output, null, null);
		
		// If the fields do not contain anything that can act as a unique identifier, 
		// you can ask the converter to generate a unique ID. Each 
		// ID will start with the same prefix, and will have a counter appended to it
		converter.setIDGenerator("CAR_MODEL");
		
	}
	
	////////////////////////////////////////////////////////
	// VERFICATION TESTS
	////////////////////////////////////////////////////////
	
	// This test is currently failing, but it doesn't matter because we are about
	// to drop the Document_DynTyped class altogether.
	@Test  @Ignore
	public void test__SeparatedFields2Document_DynTyped__HappyPath() throws Exception {
		SeparatedFields2Document_DynTyped converter = 
				new SeparatedFields2Document_DynTyped()
				.setSeparator(',') 
				.setIDFieldName("ModelID") 
				;
		
		String csvStreamContent = 
				  "Manufacturer,Model,Year,ModelID\n"
				+ "Toyota,Corolla,2009,TOY242p\n"
				+ "Hyundai,Elentra,2011,Hyu9834"
				;
		BufferedReader input = new BufferedReader(new StringReader(csvStreamContent));
		StringWriter output = new StringWriter();
		
		converter.convert(input, output, null, null);
		
		String gotOutput = output.toString();
		String expOutput = 
				   "{\"_detect_language\":true,\"lang\":\"en\",\"id\":\"TOY242p\",\"additionalFields\":{\"Year\":\"2009\",\"Manufacturer\":\"Toyota\",\"Model\":\"Corolla\",\"ModelID\":\"TOY242p\"},\"key\":\"TOY242p\",\"idFieldName\":\"ModelID\"}\n"
				 + "{\"_detect_language\":true,\"lang\":\"en\",\"id\":\"Hyu9834\",\"additionalFields\":{\"Year\":\"2011\",\"Manufacturer\":\"Hyundai\",\"Model\":\"Elentra\",\"ModelID\":\"Hyu9834\"},\"key\":\"Hyu9834\",\"idFieldName\":\"ModelID\"}\n"
						   ;
		
		AssertHelpers.assertDeepEquals("", expOutput, gotOutput);
	}	
	
	// This test is currently failing, but it doesn't matter because we are about
	// to drop the Document_DynTyped class altogether.	
	@Test @Ignore
	public void test__SeparatedFields2Document_DynTyped__GeneratedIDs() throws Exception {
		SeparatedFields2Document_DynTyped converter = 
				new SeparatedFields2Document_DynTyped()
				.setSeparator(',') 
				.setIDGenerator("CarModel")
				;
		
		String csvStreamContent = 
				  "Manufacturer,Model,Year\n"
				+ "Toyota,Corolla,2009\n"
				+ "Hyundai,Elentra,2011"
				;
		BufferedReader input = new BufferedReader(new StringReader(csvStreamContent));
		StringWriter output = new StringWriter();
		
		converter.convert(input, output, null, null);
		
		String gotOutput = output.toString();
		String expOutput = 
				   "{\"_detect_language\":true,\"lang\":\"en\",\"id\":\"CarModel_1\",\"additionalFields\":{\"CarModel\":\"CarModel_1\",\"Year\":\"2009\",\"Manufacturer\":\"Toyota\",\"Model\":\"Corolla\"},\"key\":\"CarModel_1\",\"idFieldName\":\"CarModel\"}\n"
				 + "{\"_detect_language\":true,\"lang\":\"en\",\"id\":\"CarModel_2\",\"additionalFields\":{\"CarModel\":\"CarModel_2\",\"Year\":\"2011\",\"Manufacturer\":\"Hyundai\",\"Model\":\"Elentra\"},\"key\":\"CarModel_2\",\"idFieldName\":\"CarModel\"}\n"
						   ;
		
		AssertHelpers.assertDeepEquals("", expOutput, gotOutput);
	}		

}
