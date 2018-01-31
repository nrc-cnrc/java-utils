package ca.nrc.ui.web.testing;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class ComparisonUtilsTest {

	/*********************************************************
	 * DOCUMENTATION TESTS
	 *********************************************************/
	@Test
	public void test__generateExpectedResultsString() {
		// Specify the expected labels/columns.
		String[] expectedLabels = { "Flag", "Name", "Entity List" };

		// Specify the expected URLs in order. The model url is first, followed
		// by training URLs
		String[] expectedURLs = { "http://test.com/trainURL", "http://test.com/testURL" };

		// Specify the expected scraped values. Each index corresponds to the
		// expected value rows for the web page in the corresponding index in the URL array
		// array. Each sub-array corresponds to one relation. Each index in a relation array must correspond to a label in the labels array.
		String[][][] expectedValues = {{{ "China", "Boat1", "E1" }}, {{ "Bahrain", "Boat2", "E2" }}};

		// Generate a string in the format used to compare expected results with
		// actual results displayed on the page
		String results = ComparisonUtils.generateExpectedResultsString(expectedLabels, expectedURLs, expectedValues);

		String expectedResults = "[" + "  {"
				+ "    \"relations\": [[\"Flag\",\"Name\",\"Entity List\"],[\"China\",\"Boat1\",\"E1\"]],"
				+ "    \"url\": \"http://test.com/trainURL\"" + "  }," + "  {"
				+ "    \"relations\": [[\"Flag\",\"Name\",\"Entity List\"],[\"Bahrain\",\"Boat2\",\"E2\"]],"
				+ "    \"url\": \"http://test.com/testURL\"" + "  }" + "]" + "";

		assertEquals(expectedResults, results);
	}

	/*********************************************************
	 *  VERIFICATION TESTS
	 *********************************************************/
	
	@Test
	public void test_test__generateExpectedResultsString__MultipleTestURLs(){				
		String results = ComparisonUtils.generateExpectedResultsString(
				new String[]{"Flag", "Name", "Entity List"}, 
				new String[]{"http://test.com/trainURL", "http://test.com/testURL1", "http://test.com/testURL2"}, 
				new String[][][] {
						{{"China", "Boat1", ""}},
						{{"Bahrain", "Boat2", ""}},
						{{"UAE", "Boat3", ""}}						
				 }
		);
		
		String expectedResults =
				"["+
		        		"  {"+
		        		"    \"relations\": [[\"Flag\",\"Name\",\"Entity List\"],[\"China\",\"Boat1\",\"\"]],"+
		        		"    \"url\": \"http://test.com/trainURL\""+
		        		"  },"+
		        		"  {"+
		        		"    \"relations\": [[\"Flag\",\"Name\",\"Entity List\"],[\"Bahrain\",\"Boat2\",\"\"]],"+
		        		"    \"url\": \"http://test.com/testURL1\""+
		        		"  },"+
		        		"  {"+
		        		"    \"relations\": [[\"Flag\",\"Name\",\"Entity List\"],[\"UAE\",\"Boat3\",\"\"]],"+
		        		"    \"url\": \"http://test.com/testURL2\""+
		        		"  }"+
		        		"]"+
		        		"";
		       
		
		assertEquals(expectedResults, results);
	}
	

	@Test
	public void test_test__generateExpectedResultsString__MultipleRelations(){				
		String results = ComparisonUtils.generateExpectedResultsString(
				new String[]{"Flag", "Name", "Entity List"}, 
				new String[]{"http://test.com/trainURL", "http://test.com/testURL"}, 
				new String[][][] {
						{{"China", "Boat1", ""}},
						{{"Bahrain", "Boat2", ""},{"UAE", "Boat3", ""}}						
				 }
		);
		
		String expectedResults =
				"["+
		        		"  {"+
		        		"    \"relations\": [[\"Flag\",\"Name\",\"Entity List\"],[\"China\",\"Boat1\",\"\"]],"+
		        		"    \"url\": \"http://test.com/trainURL\""+
		        		"  },"+
		        		"  {"+
		        		"    \"relations\": [[\"Flag\",\"Name\",\"Entity List\"],[\"Bahrain\",\"Boat2\",\"\"],[\"UAE\",\"Boat3\",\"\"]],"+
		        		"    \"url\": \"http://test.com/testURL\""+
		        		"  }"+
		        		"]"+
		        		"";
		       
		
		assertEquals(expectedResults, results);
	}
	
	@Test
	public void test_test__noValues(){
		//Test return value when no values were found
		
		String results = ComparisonUtils.generateExpectedResultsString(
				new String[]{"Flag", "Name", "Entity List"}, 
				new String[]{"http://test.com/trainURL", "http://test.com/testURL"}, 
				new String[][][] {
						{{"China", "Boat1", ""}},
						{}						
				 }
		);
		
		String expectedResults =
				"["+
		        		"  {"+
		        		"    \"relations\": [[\"Flag\",\"Name\",\"Entity List\"],[\"China\",\"Boat1\",\"\"]],"+
		        		"    \"url\": \"http://test.com/trainURL\""+
		        		"  },"+
		        		"  {"+
		        		"    \"relations\": [[\"Flag\",\"Name\",\"Entity List\"]],"+
		        		"    \"url\": \"http://test.com/testURL\""+
		        		"  }"+
		        		"]"+
		        		"";
		       
		
		assertEquals(expectedResults, results);
	}
}
