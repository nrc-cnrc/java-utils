package ca.nrc.testing;

import ca.nrc.json.PrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Allows a "deep equal" between two objects, no matter 
 * their structure.
 * 
 * Similar to:
 * - Hamcrest's isEqual
 * - Unittils' ReflectionAssert
 * - Mokito's ReflectionEquals
 * 
 * but with the following advantages.
 * 
 * Contrarily to Mockito and Hamcrest, it tells you exactly which parts of 
 * the two objects differ (the former only tells you wehther or 
 * not they differ).
 * 
 * Contrarily to Unittils, set equality is not sensitive to 
 * order in which the set internally stores the elements. 
 * All set elements are sorted before comparison. 
 * 
 * Contrarily to all the above, it allows you to compare 
 * objects which are "functionally" equivalent, without being of the same 
 * type. This is very convenient for specifying the expectations for 
 * a list. For example:
 * 
 *    List<String> gotResult = sayHello();
 *    String[] expResult = new String[] {"hello", "beautiful", "world"};
 *    assertDeepEquals(expResult, gotResult)
 *    
 * which is clearer and more concise than having to write the expecations as:
 * 
 *    List<String> expResult = new ArrayList<String>();
 *    expResult.add("hello); expResult.add("beautiful); 
 *    expResult.add("world")
 * 
 * There may be other advantages, but those are sufficient 
 * to warrant a "homegrown" solution.
 *  
 * @author desilets
 *
 */
public class AssertObject {
	
	public static void assertDeepEquals(
			String message, Object expObject, Object gotObject) throws IOException {
		Set<String> emptyIgnoreFields = new HashSet<String>();
		assertDeepEquals(message, expObject, gotObject, emptyIgnoreFields, null);
	}
	
	public static void assertDeepEquals(
			String message, Object expObject, Object gotObject, String... ignoreFields) throws IOException {
		Set<String> ignoreFieldsSet = new HashSet<String>();
		for (String aFieldName: ignoreFields) ignoreFieldsSet.add(aFieldName);
		assertDeepEquals(message, expObject, gotObject, ignoreFieldsSet, null);
	}

	public static void assertDeepEquals(
			String message, Object expObject, Object gotObject,
			Set<String> ignoreFields, Integer decimalsTolerance) throws IOException {
		String expObjectJsonString = new PrettyPrinter().pprint(expObject, ignoreFields, decimalsTolerance);
		assertEqualsJsonCompare(message, expObjectJsonString, gotObject, ignoreFields, true, decimalsTolerance);		
	}

	public static void assertDeepEquals(
			String message, Object expObject, Object gotObject,
			Integer decimalsTolerance) throws IOException {
		Set<String> ignoreFields = new HashSet<String>();
		String expObjectJsonString = new PrettyPrinter().pprint(expObject, ignoreFields, decimalsTolerance);
		assertEqualsJsonCompare(message, expObjectJsonString, gotObject, ignoreFields, true, decimalsTolerance);		
	}

	public static <T> void assertDeepEquals(
			String message, T[] expObjects, Iterator<T> gotObjectIterator) throws IOException {
		Set<String> emptyIgnoreFields = new HashSet<String>();
		Set<T> gotObjects = new HashSet<T>();
		while (gotObjectIterator.hasNext()) {
			gotObjects.add(gotObjectIterator.next());
		}
		assertDeepEquals(message, expObjects, gotObjects, emptyIgnoreFields, null);
	}

	
	public static void assertDeepNotEqual(String message, Object expObject, Object gotObject) {
		Boolean areEqual = false;
		try {
			// If the two objects are not equal, then the following 
			// assertion will raise an AssertionError.
			//
			assertDeepEquals("", expObject, gotObject);
			areEqual = true;
		} catch (AssertionError | IOException e) {
			areEqual = false;
		}
		
		if (areEqual) {
			Assertions.fail(
				message+"\n" +
				"The two objects should NOT have been equal but they were.\n"+
			    "Objects were both: \n"+PrettyPrinter.print(gotObject));
		}
	}

	public static void assertEqualsJsonCompare(String expJsonString, Object gotObject) throws  IOException {
		assertEqualsJsonCompare("", expJsonString, gotObject);
	}

	public static void assertEqualsJsonCompare(
			String message, String expJsonString, Object gotObject) throws  IOException {
		HashSet<String> emptySetOfFieldsToIgnore = new HashSet<String>();
		assertEqualsJsonCompare(message, expJsonString, gotObject, emptySetOfFieldsToIgnore);
	}

	public static void assertEqualsJsonCompare(String expJsonString, Object gotObject,
			HashSet<String> ignoreFields) throws  IOException {
		assertEqualsJsonCompare("", expJsonString, gotObject, ignoreFields);
	}

	public static void assertEqualsJsonCompare( 
			String expJsonString, Object gotObject,
			String[] ignoreFields) throws  IOException {
		assertEqualsJsonCompare("", expJsonString, gotObject, ignoreFields);
	}

		public static void assertEqualsJsonCompare(String message, 
			String expJsonString, Object gotObject,
			String[] ignoreFields) throws  IOException {
		Set<String> ignoreFieldsSet = new HashSet<String>();
		for (String aField: ignoreFields) {
			ignoreFieldsSet.add(aField);
		}
		assertEqualsJsonCompare(message, expJsonString, gotObject, ignoreFieldsSet);
	}
	
	
	public static void assertEqualsJsonCompare(String message, 
			String expJsonString, Object gotObject,
			Set<String> ignoreFields) throws  IOException {
		assertEqualsJsonCompare(message, expJsonString, gotObject, ignoreFields, false, null);
	}
		
	public static void assertEqualsJsonCompare(String message, 
			String expJsonString, Object gotObject,
			Set<String> ignoreFields, boolean expJsonIsAlreadyPretty, Integer decimalsTolerance) throws  IOException {
		/*
		 * Algorithm is as follows:
		 * 
		 * - Transform the gotObject into a json string where the keys of 
		 *    all dictionaries are guaranteed to be sorted alphabetically
		 *    
		 *  - if expJsonIsAlreadyPretty is false, then "prettify" it by
		 *    - Deserializing it into an Object
		 *    - PrettyPrinting it to a json string.
		 *    
		 *  - Compare the two strings using our own string comparison 
		 *    which gives a better view of the difference
		 *    
		 * Note: For generating a json string with all dictionary keys
		 *   sorted, we use our own serialization method jsonNodeToString().
		 *   We could have used the writeValueAsString with 
		 *   ORDER_MAP_ENTRIES_BY_KEYS set to true, but this does not
		 *   seem to work for JsonNode objects that have been read from string.
		 */
		
		/*
		 * Transform the gotObject into a json string where the keys of 
		 *  all dictionaries are garanteed to be sorted alphabetically.
		 *  - First we transform the object into a JsonNode
		 *  - Then we print that JsonNode into a string with keys sorted 
		 *     alphabetically  
		*/
		String gotJsonStrKeysSorted = new PrettyPrinter().pprint(gotObject, ignoreFields, decimalsTolerance);
		
		/*
		 *  Possibly "prettify" the expected json string
		 */
		String expJsonPrettyPrint = expJsonString;
		if (! expJsonIsAlreadyPretty) {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode expJsonObj = mapper.readTree(expJsonString);
			expJsonPrettyPrint = PrettyPrinter.print(expJsonObj);
		}

		// Ignore differences in \n versus \r\n
		//  TODO: This should probably be an option
		expJsonPrettyPrint = expJsonPrettyPrint.replaceAll("\r\n", "\n");
		gotJsonStrKeysSorted = gotJsonStrKeysSorted.replaceAll("\r\n", "\n");
		
		AssertString.assertStringEquals(
				message+"\nThe objects was not as expected.\nBelow is a diff of a JSON serialization of the gotten and expected objects.\n",
				expJsonPrettyPrint, gotJsonStrKeysSorted);		
	}	

}
