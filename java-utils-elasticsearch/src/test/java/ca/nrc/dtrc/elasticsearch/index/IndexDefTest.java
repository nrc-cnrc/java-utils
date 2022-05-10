package ca.nrc.dtrc.elasticsearch.index;

import java.util.HashMap;
import java.util.Map;

import ca.nrc.dtrc.elasticsearch.ESFactory;
import ca.nrc.dtrc.elasticsearch.ElasticSearchException;
import ca.nrc.dtrc.elasticsearch.FieldDef;
import ca.nrc.dtrc.elasticsearch.TypeDef;
import ca.nrc.testing.AssertObject;
import com.fasterxml.jackson.databind.ObjectMapper;

import ca.nrc.dtrc.elasticsearch.FieldDef.Types;
import ca.nrc.testing.AssertHelpers;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public abstract class IndexDefTest {

	protected abstract ESFactory makeESFactory(String indexName) throws ElasticSearchException;

	protected ESFactory esFactory = null;
	protected IndexDef iDef = null;


	@BeforeEach
	public void setUp() throws Exception {
		esFactory = makeESFactory("es-test");
		iDef = new IndexDef();
	}

	/**************************
	 * DOCUMENTATION TESTS
	 **************************/

	@Test
	public void test__IndexDef__Synopsis() throws Exception {
		IndexDef iDef =  new IndexDef();

		// You can use the IndexDef to provide some generic settings
		// for the index. These settings will apply to all fields.
		//
		{
			// You can configure index settings "by name"
			iDef.setTotalFieldsLimit(1000);
		}

		// You can also use the IndexDef to provide some information about
		// fields and how to process them
		{
			//
			// Here is how you get the definition of a particular type in that index.
			//
			String typeName = "books";
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

			// Here is how you get the mappings as a JSON-ready Map
			//
			JSONObject mappings = iDef.jsonMappings();
		}
	}
	
	/**************************
	 * VERIFICATION TESTS
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

		JSONObject gotMappings = iDef.jsonMappings();
		JSONObject expMappings = new JSONObject()
			.put("books", new JSONObject()
				.put("properties", new JSONObject()
					.put("author", new JSONObject()
						.put("type", "text")
					)
					.put("publication_date", new JSONObject()
						.put("type", "date")
					)
					.put("title", new JSONObject()
						.put("type", "date")
						.put("analyzer", "english")
					)
				)
			);

		expMappings
			.put("movies", new JSONObject()
				.put("properties", new JSONObject()
					.put("director", new JSONObject()
						.put("type", "text")
					)
					.put("release_date", new JSONObject()
						.put("type", "date")
					)
					.put("title", new JSONObject()
						.put("type", "date")
						.put("analyzer", "english")
					)
				)
			);
		AssertObject.assertDeepEquals(
			"IndexDef mappings was not as expected",
			expMappings, gotMappings);
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
