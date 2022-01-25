package ca.nrc.dtrc.elasticsearch;

import java.time.LocalDate;

import ca.nrc.testing.AssertObject;
import ca.nrc.testing.AssertString;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;

import ca.nrc.testing.AssertHelpers;

public class DocumentTest {

	protected static class Person extends Document {
		
		public String first = null;
		public String last = null;
		
		public Person(String _first, String _last) {
			super();
			this.first = _first;
			this.last = _last;
		}
	}

	@Test
	public void test__getFieldByName__HappyPath() throws Exception {
		Person homer = new Person("homer", "simpson");
		String first = (String) homer.getField("first");
		Assert.assertEquals("homer", first);
	}
	
	@Test
	public void test_getCreationLocalDate__HappyPath() throws Exception {
		Document doc = new Document();
		
		LocalDate gotDate = doc.getCreationLocalDate();
		AssertHelpers.assertDeepEquals("", null, gotDate);
		
		String dateStr = "2018-01-13";
		doc.setCreationDate(dateStr);
		gotDate = doc.getCreationLocalDate();
		AssertHelpers.assertDeepEquals("", LocalDate.parse(dateStr), gotDate);
	}
	
	@Test
	public void test__longDescription_and_content_are_synonyms() {
		Document doc = new Document();
		String content = "hello world";
		
		doc.setLongDescription(content);
		AssertString.assertStringEquals(doc.getContent(), content);
		
		content = "greetings earthlings";
		doc.setContent(content);
		AssertString.assertStringEquals(doc.getLongDescription(), content);
	}

	@Test
	public void test__toJson__DoesNotInclude_longDescription_Field() throws Exception {
		ESTestHelpers.SimpleDoc doc = new ESTestHelpers.SimpleDoc();
		doc.setContent("hello world");
		String gotJson = doc.toJson();
		String expJson = 
				"{\n" +
				"  \"_detect_language\":\n" +
				"    true,\n" +
				"  \"additionalFields\":\n" +
				"    {\n" +
				"    },\n" +
				"  \"category\":\n" +
				"    null,\n" +
				"  \"content\":\n" +
				"    \"hello world\",\n" +
				"  \"creationDate\":\n" +
				"    null,\n" +
				"  \"idWithoutType\":\n" +
				"    null,\n" +
				"  \"lang\":\n" +
				"    \"en\",\n" +
				"  \"shortDescription\":\n" +
				"    null,\n" +
				"  \"type\":\n" +
				"    \"simpledoc\"\n" +
				"}"
				;
		AssertObject.assertEqualsJsonCompare(expJson, doc);
	}
	
	@Test
	public void test__truncateID__IDWithinElasticSearchLimit__DoesNOTGetTruncated() throws Exception {
		String rawID = "this is a short ID";
		Document doc = new Document(rawID, "doc");
		String gotID = doc.truncateID(rawID);
		
		AssertString.assertStringEquals(rawID, gotID);
	}

	@Test
	public void test__truncateID__IDOverElasticSearchLimit__GetsTruncated() throws Exception {
		String rawID =
				"This is a very long ID xxxxxxxxxxxxxxxxxxxxxxxxxxx\n" +				
				"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\n" +
				"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\n" +
				"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\n" +
				"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\n" +
				"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\n" +
				"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\n" +
				"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\n" +
				"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\n" +
				"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\n" +
				"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\n" +
				"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\n" +
				"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\n" +
				"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\n";
		
		Document doc = new Document();
		String gotID = doc.truncateID(rawID);

		String expID = 		
			"This is a very long ID xxxxxxxxxxxxxxxxxxxxxxxxxxx\n" + 
			"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\n" + 
			"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\n" + 
			"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\n" + 
			"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\n" + 
			"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\n" + 
			"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\n" + 
			"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\n" + 
			"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\n" + 
			"xxxxxxxxxxxxxxxxxxxx DBDAC6CA1926666BDB70FFB33BD344C0";
		
		AssertString.assertStringEquals(expID, gotID);
	}

	@Test
	public void test__parseID__VariousCases() throws Exception {
		String id = "Homer";
		Pair<String,String> gotParsed = Document.parseID(id);
		AssertObject.assertDeepEquals(
			"ID "+id+" not parsed properly",
			Pair.of(null, "Homer"), gotParsed
		);
		id = "person:Homer";
		gotParsed = Document.parseID(id);
		AssertObject.assertDeepEquals(
			"ID "+id+" not parsed properly",
			Pair.of("person", "Homer"), gotParsed
		);
		id = null;
		gotParsed = Document.parseID(id);
		AssertObject.assertDeepEquals(
			"ID "+id+" not parsed properly",
			Pair.of(null, null), gotParsed
		);

	}
}
