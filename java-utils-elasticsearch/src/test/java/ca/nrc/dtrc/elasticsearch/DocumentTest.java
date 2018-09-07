package ca.nrc.dtrc.elasticsearch;

import static org.junit.Assert.*;

import java.time.LocalDate;

import org.junit.Assert;
import org.junit.Test;

import ca.nrc.dtrc.elasticsearch.Document;
import ca.nrc.testing.AssertHelpers;

public class DocumentTest {

	protected static class Person extends Document {
		
		public String first = null;
		public String last = null;
		
		public Person(String _first, String _last) {
			this.first = _first;
			this.last = _last;
		}

		@Override
		public String keyFieldName() {
			return "first";
		}

		@Override
		public String getId() {
			return first;
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
		Document doc = new Document("doc1");
		
		LocalDate gotDate = doc.getCreationLocalDate();
		AssertHelpers.assertDeepEquals("", null, gotDate);
		
		String dateStr = "2018-01-13";
		doc.setCreationDate(dateStr);
		gotDate = doc.getCreationLocalDate();
		AssertHelpers.assertDeepEquals("", LocalDate.parse(dateStr), gotDate);
	}

}
