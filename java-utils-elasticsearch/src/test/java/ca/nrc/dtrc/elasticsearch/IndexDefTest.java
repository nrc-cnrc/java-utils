package ca.nrc.dtrc.elasticsearch;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.nrc.dtrc.elasticsearch.FieldDef.Types;
import ca.nrc.testing.AssertHelpers;

public class IndexDefTest {
	
	/**************************
	 * DOCUMENTATION TESTS
	 **************************/

	@Test
	public void test__IndexDef__Synopsis() {
		//
		// Use a FieldDef to define the characteristics of a field of a 
		// document type in ElasticSearch
		//
		String indexName = "some_index";
		IndexDef iDef = new IndexDef(indexName);
		
		//
		// Here is how you get the definition of a particular type in that index.
		//
		String typeName = "books";;
		TypeDef tDef = iDef.getTypeDef(typeName);
		
		// Here is now you get the definition of a particular field in that type
		//
		String fldName = "title";
		FieldDef fDef = tDef.getFieldDef(fldName);
		
		//
		// Once you have the definition of a field in a type of the index, you can 
		// configure it as follows...
		//
		// This particular field is of type 'text', and it is analyzed using the "engllish"
		// analyzer (which means stopwords and stemming will be performed)
		//
		fDef.setType(FieldDef.Types.text);
		fDef.setAnalyzer("english");
		
		
		//
		// You can chain the methods above to define a field of a type in one go.
		//
		// For example, this defines a a field books.genre, which is text, but NOT analyzed 
		// (because we don't invoke setAnalyser() on it.
		//
		iDef.getTypeDef(typeName).getFieldDef("genre").setType(FieldDef.Types.text);
		
		// And this sets a field books.pages which is of type integer
		//
		iDef.getTypeDef(typeName).getFieldDef("pages").setType(FieldDef.Types.integer);
		
		
		// If we want this text field to be stemmed and to have stopwords removed, we have to do this
		fDef.setAnalyzer("english");
		
		// Once you have created an IndexDef, you can convert it to a Map<String,Object>, 
		// which can then be fed to ElasticSearch to define the index.
		//
		Map<String,Object> map = iDef.indexMappings();
	}
	
	/**************************
	 * DOCUMENTATION TESTS
	 **************************/

	@Test
	public void test__toMap__HappyPath() throws Exception {
		String name = "books";
		IndexDef iDef = new IndexDef(name);

		// Define a BOOKS type
		final String BOOKS = "books";
		TypeDef booksDef = iDef.getTypeDef(BOOKS);
		booksDef.getFieldDef("title").setType(FieldDef.Types.text);
		booksDef.getFieldDef("author").setType(FieldDef.Types.text).setAnalyzer("none");
		booksDef.getFieldDef("publication_date").setType(Types.date);
		
		// Define a MOVIES type
		final String MOVIES = "movies";
		TypeDef moviesDef = iDef.getTypeDef(MOVIES);
		moviesDef.getFieldDef("title").setType(FieldDef.Types.text);
		moviesDef.getFieldDef("director").setType(FieldDef.Types.text).setAnalyzer("none");
		moviesDef.getFieldDef("release_date").setType(Types.date);

		Map<String,Object> gotMap = iDef.indexMappings();
		String expMapJson = 
				  "{"
				+ "  \"mappings\": {\n"
				+ "    \"books\": {\n"
				+ "      \"properties\": {\n"
				+ "        \"author\": {\n"
				+ "          \"type\": \"text\"\n"
				+ "        },\n"
				+ "        \"publication_date\": {\n"
				+ "          \"type\": \"date\"\n"
				+ "        },\n"
				+ "        \"title\": {\n"
				+ "          \"type\": \"text\",\n"
				+ "          \"analyzer\": \"english\""
				+ "        }\n"
				+ "      }\n"
				+ "    },\n"
				+ "    \"movies\": {\n"
				+ "      \"properties\": {\n"
				+ "        \"director\": {\n"
				+ "          \"type\": \"text\"\n"
				+ "        },\n"
				+ "        \"release_date\": {\n"
				+ "          \"type\": \"date\"\n"
				+ "        },\n"
				+ "        \"title\": {\n"
				+ "          \"type\": \"text\",\n"
				+ "          \"analyzer\": \"english\""
				+ "        }\n"
				+ "      }\n"
				+ "    }\n"
				+ "  }\n"
				+ "}"
				;
		Map<String,Object> expMap = new HashMap<String,Object>();
		expMap = new ObjectMapper().readValue(expMapJson, expMap.getClass());
		AssertHelpers.assertDeepEquals("Index definition map was not as expected", 
				expMap, gotMap);
	}	

	@Test
	public void test__map2props__AND__props2map__HappyPath() throws Exception {
		Map<String,Object> tree = new HashMap<String,Object>();
		Map<String,Object> A = new HashMap<String,Object>();
		tree.put("A", A);
		{
			Map<String,Object> A_x = new HashMap<String,Object>();
			A.put("x", A_x);
			{
				A_x.put("hello", "world");
				A_x.put("num", 1000);
			}
			Map<String,Object> A_y = new HashMap<String,Object>();
			A.put("y", A_y);
			{
				A_y.put("greetings", "universe");
			}
		}
	
		Map<String,Object> gotProps = IndexDef.tree2props(tree);
		Map<String,Object> expProps = new HashMap<String,Object>();
		{
			expProps.put("A.y.greetings", "universe");
			expProps.put("A.x.num", 1000);
			expProps.put("A.x.hello", "world");
		}
		
		AssertHelpers.assertDeepEquals("Props converted from tree not as expected", 
				expProps, gotProps);
		
		Map<String,Object> gotTree = IndexDef.props2tree(gotProps);
		
		AssertHelpers.assertDeepEquals("Tree converted from props not as expected", 
				tree, gotTree);
	}
	
}
