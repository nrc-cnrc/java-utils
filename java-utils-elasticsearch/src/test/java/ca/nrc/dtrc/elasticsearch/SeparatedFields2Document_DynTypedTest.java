package ca.nrc.dtrc.elasticsearch;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.junit.Test;

import ca.nrc.dtrc.elasticsearch.SeparatedFields2Document_DynTyped;
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
		SeparatedFields2Document_DynTyped converter = new SeparatedFields2Document_DynTyped();
		
		String csvStreamContent = 
				  "First,Last,Gender\n"
				+ "Homer,Simpson,M\n"
				+ "Marg,Simpson,F"
				;
		BufferedReader input = new BufferedReader(new StringReader(csvStreamContent));
		StringWriter output = new StringWriter();
		
		converter.convert(input, output);
		
		// By default, the converter assumes that the stream is comma separated.
		// But you can override that
		String tsvStreamContent = 
				  "First\tLast\tGender\n"
				+ "Homer\tSimpson\tM\n"
				+ "Marg\tSimpson\tF"
				;
		input = new BufferedReader(new StringReader(tsvStreamContent));
		output = new StringWriter();
		
		converter.convert(input, output, "\t");
	}
	
	////////////////////////////////////////////////////////
	// VERFICATION TESTS
	////////////////////////////////////////////////////////
	
	@Test
	public void test__SeparatedFields2Document_DynTyped__HappyPath() throws Exception {
		SeparatedFields2Document_DynTyped converter = new SeparatedFields2Document_DynTyped();
		
		String csvStreamContent = 
				  "First,Last,Gender\n"
				+ "Homer,Simpson,M\n"
				+ "Marg,Simpson,F"
				;
		BufferedReader input = new BufferedReader(new StringReader(csvStreamContent));
		StringWriter output = new StringWriter();
		
		converter.convert(input, output);
		
		String gotOutput = output.toString();
		String expOutput = 
				  "{BLAH}\n"
				+ "{BLOB}"
				;
		AssertHelpers.assertDeepEquals("", expOutput, gotOutput);
	}	

}
