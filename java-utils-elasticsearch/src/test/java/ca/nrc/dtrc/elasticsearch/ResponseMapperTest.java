package ca.nrc.dtrc.elasticsearch;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ResponseMapperTest {

	@Test
	public void test__ResponseMapper__Synopsis() throws Exception {
		// Use this class to map ElasticSearch responses to objects.
		final ResponseMapper strictMapper = new ResponseMapper();

		// You can map a response to a single Document
		String index = "someIndex";
		String jsonResp =
			"{\"_index\":\"some-index\","+
			"\"_type\":\"carton-chars\","+
			"\"_id\":\"HomerSimpson\","+
			"\"found\":true,"+
			"\"_source\":{\"id\":\"HomerSimpson\",\"firstName\":\"Homer\",\"surname\":\"Simpson\",\"gender\":\"m\"}}"
		;
		String mess = "Just testing";
		Person pers = strictMapper.mapSingleDocResponse(jsonResp, Person.class, mess, index);

	// For some unknown reason, ES records sometimes get corrupted to a state
	// where they don't fit the structure of the object they are supposed to represent
	//	(typically, they end up with unexpected fields like 'scroll' in the source).
	//
	String jsonCorruptedDocResp =
		"{\"_index\":\"some-index\","+
		"\"_type\":\"carton-chars\","+
		"\"_id\":\"HomerSimpson\","+
		"\"found\":true,"+
		// This is a corrupted document record. Not sure how those happen.
		"\"_source\":{\"scroll\":\"1m\"}}"
		;

		// By default, the defaultMapper will raise an exception when it encounters such
		// a corrupted record response
		Assertions.assertThrows(CorruptedESRecordException.class, () -> {
			strictMapper.mapSingleDocResponse(jsonCorruptedDocResp, Person.class, mess, index);
		});

		// However, you can configure the defaultMapper so it just ignores those kinds
		// of problems (note however that it will still LOG the exceptions)
		// In that situation, the defaultMapper will return a null document
		//
		ResponseMapper lenientMapper = new ResponseMapper(ResponseMapper.BadRecordPolicy.LOG_EXCEPTION);
		pers = lenientMapper.mapSingleDocResponse(jsonCorruptedDocResp, Person.class, mess, index);
		Assertions.assertTrue(pers == null);
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
