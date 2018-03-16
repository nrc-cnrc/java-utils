package ca.nrc.testing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.junit.Assert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ca.nrc.json.PrettyPrinter;



/*
 * Additional useful assertions. For example, for easily comparing complex data structures.
 * 
 * For more information on how to use this class, see the 
 * DOCUMENTATION TESTS section of test case
 * AssertTest.
 */ 

public class AssertHelpers {

	public static void assertDeepEquals(
			String message, Object expObject, Object gotObject) throws IOException {
		Set<String> emptyIgnoreFields = new HashSet<String>();
		assertDeepEquals(message, expObject, gotObject, emptyIgnoreFields);
	}
	
	public static void assertDeepEquals(
			String message, Object expObject, Object gotObject, String[] ignoreFields) throws IOException {
		Set<String> ignoreFieldsSet = new HashSet<String>();
		for (String aFieldName: ignoreFields) ignoreFieldsSet.add(aFieldName);
		assertDeepEquals(message, expObject, gotObject, ignoreFieldsSet);
	}

	public static void assertDeepEquals(
			String message, Object expObject, Object gotObject,
			Set<String> ignoreFields) throws IOException {
		String expObjectJsonString = PrettyPrinter.print(expObject, ignoreFields);
		assertEqualsJsonCompare(message, expObjectJsonString, gotObject, ignoreFields, true);		
	}
	
	public static void assertDeepNotEqual(String message, Object expObject, Object gotObject) {
		try {
			assertDeepEquals("", expObject, gotObject);
			
			// NOTE: If the two objects are not equal, then the above assertion should
			//  fail. Therefore, if we make it to here, it means that the two 
			//  objects are equal, and that we should raise an exception (since
			//  we are trying to assert that the two objects are NOT equal).
			
			Assert.assertTrue(message+"\nThe two objects should NOT have been equal. But they were both equal to:\n"+PrettyPrinter.print(expObject), 
					false);
			
		} catch (Exception e) {
			
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
		assertEqualsJsonCompare(message, expJsonString, gotObject, ignoreFields, false);
	}
		
	public static void assertEqualsJsonCompare(String message, 
			String expJsonString, Object gotObject,
			Set<String> ignoreFields, boolean expJsonIsAlreadyPretty) throws  IOException {
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
		String gotJsonStrKeysSorted = PrettyPrinter.print(gotObject, ignoreFields);
		
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
		
		assertStringEquals(
				message+"\nThe objects was not as expected.\nBelow is a diff of a JSON serialization of the gotten and expected objects.\n",
				expJsonPrettyPrint, gotJsonStrKeysSorted);		
	}

	public static void assertStringEquals(String message, String expString, String gotString) {
		message = message +
				"The two strings differred. Location of the first difference is highlighted below with tag <FIRST_DIFF>.\n";
		
		int firstDiffOffset = StringUtils.indexOfDifference(expString, gotString);
		
		if (expString == null || gotString == null) {
			Assert.assertEquals(message, expString, gotString);
		}

		if (firstDiffOffset >= 0) {
			String commonStart = expString.substring(0, firstDiffOffset);
			String expectedRest = expString.substring(firstDiffOffset);
			String gotRest = gotString.substring(firstDiffOffset);

			message = 
					message + 
					"== Expected:\n "+
					commonStart +
					"<<FIRST_DIFF>>" +
					expectedRest + "\n";
			message = 
					message + 
					"== Got         :\n "+
					commonStart +
					"<<FIRST_DIFF>>" +
					gotRest + "\n";
			
			Assert.fail(message);
		}
	}

	public static void assertStringEquals(String expString, String gotString) {
		assertStringEquals("", expString, gotString);
	}

	public static void assertStringContains(String gotString, String expSubstring) {
		assertStringContains(null, gotString, expSubstring);
	}

	public static void assertStringContains(String message,
			String gotString, String expSubstring) {
		boolean caseSensitive = true;
		assertStringContains(message, gotString, expSubstring, caseSensitive);
	}

	public static void assertStringContains(String message,
			String gotString, String expSubstring, boolean caseSensitive) {
		
		if (!caseSensitive) {
			gotString = gotString.toLowerCase();
			expSubstring = expSubstring.toLowerCase();
		}
		
		if (message == null) {
			message = "";
		} else {
			message = message + "\n";
		}
		message = message + 
				   "String did not contain expected substring.\n"
						  + "== Expected substring: \n"+expSubstring+"\n\n"
						  + "== Got string : \n"+gotString+"\n\n";

		Assert.assertTrue(message, gotString.contains(expSubstring));
	}
	
	public static void assertStringDoesNotContain(String gotString, String unexpSubstring) {
		boolean caseSensitive = true;
		assertStringDoesNotContain("", gotString, unexpSubstring, caseSensitive);
	}
	
	public static void assertStringDoesNotContain(String message,
			String gotString, String unexpSubstring) {
		boolean caseSensitive = true;
		assertStringDoesNotContain(message, gotString, unexpSubstring, caseSensitive);
	}
	
	public static void assertStringDoesNotContain(String message, String gotString, 
			String unexpSubstring, boolean caseSensitive) {
		if (!caseSensitive) {
			gotString = gotString.toLowerCase();
			unexpSubstring = unexpSubstring.toLowerCase();
		}
		
		if (message == null) {
			message = "";
		} else {
			message = message + "\n";
		}
		message = message + 
				   "String contained an UN-expected substring.\n"
						  + "== Un-expected substring: \n"+unexpSubstring+"\n\n"
						  + "== Got string : \n"+gotString+"\n\n";

		Assert.assertFalse(message, gotString.contains(unexpSubstring));
	}	

	public static void assertContainsAll(String message, Object[] supersetArr, Object[] subsetArr) {
		Set<Object> superset = new HashSet<Object>();
		for (Object elt: supersetArr) superset.add(elt);
		
		int ii = 0;
		for (Object elt: subsetArr) {
			if (!superset.contains(elt)) {
				message += 
						  "\nElement "+ii+"("+elt+") of the second collection is absent from the first collection\n"
						+ "First collection:\n"+PrettyPrinter.print(supersetArr)+"\n"
						+ "\n"
						+ "Second collection:\n"+PrettyPrinter.print(subsetArr)+"\n"
						;
					Assert.fail(message);

			}
			ii++;
		}
		
		Set<Object> subset = new HashSet<Object>();
		for (Object elt: subsetArr) subset.add(elt);
		
		if (!superset.containsAll(subset)) {
		}
	}
	
	/**
	 * Indicates if elements in two collections are the same, ignoring the order of elements
	 * Assumes that objects inside the collection can be tested for equality through their equals function.
	 * @param failMessageStart Beginning of the message displayed when the assert fails
	 * @param exp array containing the expected results
	 * @param got array containing the actual results
	 * @throws IOException 
	 */
	public static <T> void assertUnOrderedSameElements(String failMessageStart, T[] exp, T[] got) throws IOException{
		List<T> lstExp = new ArrayList<T>();
		for(T e: exp){
			lstExp.add(e);
		}
		List<T> lstGot = new ArrayList<>();
		for(T g: got){
			lstGot.add(g);
		}
		assertUnOrderedSameElements(failMessageStart, lstExp, lstGot);
	}
	
	/**
	 * Indicates if elements in two collections are the same, ignoring the order of elements
	 * Assumes that objects inside the collection can be tested for equality through their equals function.
	 * @param failMessageStart Beginning of the message displayed when the assert fails
	 * @param exp List containing the expected results
	 * @param got List containing the actual results
	 * @throws IOException 
	 */
	public static <T,U> void assertUnOrderedSameElements(String failMessageStart, List<T> exp, List<U> got) throws IOException{

		 if(exp.size() != got.size()){
			StringBuffer sizeMessage = new StringBuffer(failMessageStart);
			sizeMessage.append("Different number of elements: <");
			sizeMessage.append(exp.size());
			sizeMessage.append(">, <");
			sizeMessage.append(got.size());						
			sizeMessage.append(">\nExpected: \n");
			for(T expItem:exp){
				sizeMessage.append(expItem);
				sizeMessage.append("\n");
			}
			sizeMessage.append("\nGot: \n");
			for (U gotItem:got){
				sizeMessage.append(gotItem);
				sizeMessage.append("\n");
			}
			
			Assert.fail(sizeMessage.toString());
		}else{
			HashSet<T> expHashSet = new HashSet<>(exp);
			HashSet<U> gotHashSet = new HashSet<>(got);
			
			if(!expHashSet.equals(gotHashSet)){
				StringBuffer message = new StringBuffer(failMessageStart);
				message.append("\nElements are different in both collections. \nExpected:\n");
				Iterator<T> itExp = expHashSet.iterator();
				while(itExp.hasNext()){
					message.append(itExp.next());
					message.append("\n");
				}
				message.append("\nGot:\n");
				Iterator<U> itGot = gotHashSet.iterator();
				while(itGot.hasNext()){
					message.append(itGot.next());
					message.append("\n");
				}
				
				Assert.fail(message.toString());	
			}						
		}
	}
	
	/**
	 * Indicates if contents of two HashMaps are the same.
	 * When the value of the HashMap is a List, order is ignored
	 * Assumes keys and values can be tested for equality through the equals function, with the exception of Lists which is handled differently by this method.
	 * @param failMessageStart Beginning of the message displayed when the assert fails
	 * @param exp HashMap containing the expected results
	 * @param got HashMap containing the actual results
	 * @throws IOException 
	 */
//	public static <K,V> void assertHashMapsEqualUnOrdered(String failMessageStart, Map<K, V> exp, Map<K, V> got){
//		List<K> expKeys = new ArrayList<K>(exp.keySet());
//		List<K> gotKeys = new ArrayList<K>(got.keySet());
//		
//		//Verify that the keys are the same
//		assertUnOrderedSameElements(failMessageStart + "\nHashMap keys differ: ", expKeys, gotKeys);
//		
//		for(K expKey: expKeys){
//			V expVal = exp.get(expKey);
//			V gotVal = got.get(expKey);
//			
//			StringBuffer errorMessage = new StringBuffer(failMessageStart);
//			errorMessage.append("\n Key: ");
//			errorMessage.append(expKey);
//			
//			if(expVal instanceof List<?> && gotVal instanceof List<?>){
//				assertUnOrderedSameElements(errorMessage.toString(), (List<?>)expVal, (List<?>)gotVal);
//			}else if (!expVal.equals(gotVal)){
//				errorMessage.append("\n Values not equal ");
//				errorMessage.append(expVal);
//				errorMessage.append(", ");
//				errorMessage.append(gotVal);
//				Assert.fail(errorMessage.toString());
//			}
//		}
//	}
	
	public static <K,V> void assertHashMapsEqualUnOrdered(String failMessageStart, Map<K, V> exp, Map<K, V> got) throws IOException{
		
		for (K aKey: exp.keySet()) {
			V aVal = exp.get(aKey);
			if (aVal instanceof List<?>) {
				Collections.sort((List)aVal);
			}
		}
		
		for (K aKey: got.keySet()) {
			V aVal = got.get(aKey);
			if (aVal instanceof List<?>) {
				Collections.sort((List)aVal);
			}
		}
		
		AssertHelpers.assertDeepEquals("The two hashmaps differed in more than the order of the list values.", exp, got);		
	}	

	public static void assertValueInRange(String message, double minValue, double maxValue, double gotValue) {
		Assert.assertTrue(message+"\nValue: "+ gotValue + " was smaller than expexected min value: " + minValue, 
				gotValue >= minValue);
		Assert.assertTrue(message+"\nValue: "+ gotValue + " was greater than expexected max value: " + maxValue, 
				gotValue <= maxValue);
	}

}
