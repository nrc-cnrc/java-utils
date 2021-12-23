package ca.nrc.dtrc.elasticsearch;

import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ResponseMapperTest {

	@Test
	public void test__ResponseMapper__Synopsis() throws Exception {
		// Use this class to map ElasticSearch responses to objects
		// of a specific class.
		//
		// The mapper can be set to be 'strict' or 'lenient'
		//
		// The two configurations differ in the way they behave when the mapper
		// encounters an ESFactory record that does not have the expected Document structure.
		//
		//	  LENIENT: The mapper will raise a BadESRecordException.
		//
		//	  STRITC: The mapper will og the exception and return a null or empty result
		//   (depending on the context).
		//
		// Note that there are two types of reasons why a mapper may encounter a
		// bad ESFactory record, namely:
		//
		//   Programming error: This happens if your code tries to map an ESFactory record
		//     that is based on a document class DC1, but it passes a different
		//     class DC2 to the mapping method.
		//
		//   Corrupted ESFactory record: For some unknown reason, ESFactory document records can
		//     become corrupted and end up with a structure that does not correspond
		//     to any Document class (typically, then end up with non-Document
		//     fields like 'scroll')
		//
		// The ResponseMapper class does not distinguish between those types of
		// circumstances and will log or raise the exception the same way.
		String indexName = "someindex";
		final ResponseMapper strictMapper = new ResponseMapper(indexName);
		final ResponseMapper lenientMapper = new ResponseMapper(indexName, ErrorHandlingPolicy.LENIENT);

		// You can use the mapper to map different types of ESFactory responses.
		// For example, here is how you map an ESFactory response that provides the
		// data for a single document
		{
			// Response for an non-corrupted document
			String index = "someIndex";
			JSONObject jsonResp = new JSONObject()
				.put("_index", "some-index")
				.put("_type", "cartoon-chars")
				.put("_id", "HomerSimpson")
				.put("found", true)
				.put("_source",
					new JSONObject()
						.put("id", "HomerSimpson")
						.put("firstName", "Homer")
						.put("surname", "Simpson")
						.put("gender", "m")
					)
				;
			String mess = "Just testing";
			Person pers;
			pers = strictMapper.response2doc(jsonResp, Person.class, mess);

			// Response for an non-corrupted document
			JSONObject jsonCorruptedDocResp = new JSONObject()
				.put("_index", "some-index")
				.put("_type", "cartoon-chars")
				.put("_id", "HomerSimpson")
				.put("found", true)
				// This is a corrupted document record. Not sure how those happen but
				// they sometimes do.
				.put("_source", new JSONObject().put("scroll", "1m"));
				;
			// In this case, the strict mapper raises an exception
			Assertions.assertThrows(BadESRecordException.class, () -> {
				strictMapper.response2doc(jsonCorruptedDocResp, Person.class, mess);
			});
			// But the lenient mapper does NOT raise an exception and it
			// returns null
			//
			pers = lenientMapper.response2doc(jsonCorruptedDocResp, Person.class, mess);
			Assertions.assertTrue(pers == null);
		}
	}

	public static class Person extends Document {
		public String firstName = null;
		public String surname = null;
		public String gender = null;
		public Integer age = 0;

		public Person() {
			super();
			type = "person";
		}

		public Person(String _first, String _surname, String _gender) {
			super();
			firstName = _first;
			surname = _surname;
			gender = _gender;
			type = "person";
		}

		@Override
		public String getId() {
			return firstName+surname;
		}
 	}
}
