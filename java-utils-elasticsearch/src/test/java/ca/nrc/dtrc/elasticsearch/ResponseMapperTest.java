package ca.nrc.dtrc.elasticsearch;

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
		final ResponseMapper strictMapper = new ResponseMapper();
		final ResponseMapper lenientMapper = new ResponseMapper(ResponseMapper.BadRecordPolicy.LOG_EXCEPTION);

		// The lenient configuraiton is designed to deal with corrupted ES records.
		// Indeed, for some unknown reason, ES records sometimes get corrupted to a state
		// where they don't fit the structure of the object they are supposed to represent
		//	(typically, they end up with unexpected fields like 'scroll' in the source).
		//
		// When a strict mapper encounters this kind of situation, it will raise
		// a CorruptedESRecordException.
		//
		// A lenient mapper on the other hand will just log the exception and
		// return some 'default' object.

		// You can use the mapper to map different types of ES responses.
		// For example, here is how you map an ES response that provides the
		// data for a single document
		{
			// Response for an non-corrupted document
			String index = "someIndex";
			String jsonResp =
				"{\"_index\":\"some-index\"," +
				"\"_type\":\"carton-chars\"," +
				"\"_id\":\"HomerSimpson\"," +
				"\"found\":true," +
				"\"_source\":{\"id\":\"HomerSimpson\",\"firstName\":\"Homer\",\"surname\":\"Simpson\",\"gender\":\"m\"}}";
			String mess = "Just testing";
			Person pers = strictMapper.mapSingleDocResponse(jsonResp, Person.class, mess, index);

			// Response for an non-corrupted document
			String jsonCorruptedDocResp =
				"{\"_index\":\"some-index\"," +
				"\"_type\":\"carton-chars\"," +
				"\"_id\":\"HomerSimpson\"," +
				"\"found\":true," +
				// This is a corrupted document record. Not sure how those happen.
				"\"_source\":{\"scroll\":\"1m\"}}";
			// In this case, the strict mapper raises an exception
			Assertions.assertThrows(CorruptedESRecordException.class, () -> {
				strictMapper.mapSingleDocResponse(jsonCorruptedDocResp, Person.class, mess, index);
			});
			// But the lenient mapper does NOT raise an exception and it
			// returns null
			//
			pers = lenientMapper.mapSingleDocResponse(jsonCorruptedDocResp, Person.class, mess, index);
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
		}

		public Person(String _first, String _surname, String _gender) {
			super();
			firstName = _first;
			surname = _surname;
			gender = _gender;
		}

		@Override
		public String getId() {
			return firstName+surname;
		}
 	}
}
