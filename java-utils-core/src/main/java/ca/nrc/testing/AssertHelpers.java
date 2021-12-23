package ca.nrc.testing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.*;

import ca.nrc.json.PrettyPrinter;

/*
 * Additional useful assertions. For example, for easily comparing complex data structures.
 * 
 * For more information on how to use this class, see the 
 * DOCUMENTATION TESTS section of test case
 * AssertTest.
 */ 

public class AssertHelpers {
	
	public  enum Comp  {AT_LEAST, AT_MOST, EQUAL_TO, DIFFREENT_FROM};
	
	/**
	 * @deprecated  Use AssertObject method instead
	 */
	@Deprecated
	public static void assertDeepEquals(
			String message, Object expObject, Object gotObject) throws IOException {
		AssertObject.assertDeepEquals(message, expObject, gotObject);
	}
	
	/**
	 * @deprecated  Use AssertObject method instead
	 */
	@Deprecated
	public static void assertDeepEquals(
			String message, Object expObject, Object gotObject, String[] ignoreFields) throws IOException {
		AssertObject.assertDeepEquals(message, expObject, gotObject, ignoreFields);
	}
	
	/**
	 * @deprecated  Use AssertObject method instead
	 */
	@Deprecated
	public static void assertDeepEquals(
			String message, Object expObject, Object gotObject,
			Set<String> ignoreFields, Integer decimalsTolerance) throws IOException {
		AssertObject.assertDeepEquals(message, expObject, gotObject, ignoreFields, decimalsTolerance);		
	}

	/**
	 * @deprecated  Use AssertObject method instead
	 */
	@Deprecated
	public static void assertDeepEquals(
			String message, Object expObject, Object gotObject,
			Integer decimalsTolerance) throws IOException {
		AssertObject.assertDeepEquals(message, expObject, gotObject, decimalsTolerance);				
	}
	
	/**
	 * @throws IOException 
	 * @deprecated  Use AssertObject method instead
	 */
	@Deprecated
	public static void assertDeepNotEqual(String message, Object expObject, Object gotObject) throws IOException {
		AssertObject.assertDeepNotEqual(message, expObject, gotObject);		
	}

	/**
	 * @deprecated  Use AssertObject method instead
	 */
	@Deprecated
	public static void assertEqualsJsonCompare(String expJsonString, Object gotObject) throws  IOException {
		AssertObject.assertEqualsJsonCompare(expJsonString, gotObject);		
	}
	
	/**
	 * @deprecated  Use AssertObject method instead
	 */
	@Deprecated
	public static void assertEqualsJsonCompare(
			String message, String expJsonString, Object gotObject) throws  IOException {
		AssertObject.assertEqualsJsonCompare(message, expJsonString, gotObject);		
	}

	/**
	 * @deprecated  Use AssertObject method instead
	 */
	@Deprecated
	public static void assertEqualsJsonCompare(String expJsonString, Object gotObject,
			HashSet<String> ignoreFields) throws  IOException {
		AssertObject.assertEqualsJsonCompare(expJsonString, gotObject, ignoreFields);		
	}

	/**
	 * @deprecated  Use AssertObject method instead
	 */
	@Deprecated
	public static void assertEqualsJsonCompare( 
			String expJsonString, Object gotObject,
			String[] ignoreFields) throws  IOException {
		AssertObject.assertEqualsJsonCompare("", expJsonString, gotObject, ignoreFields);
	}

	/**
	 * @deprecated  Use AssertObject method instead
	 */
	@Deprecated	
	public static void assertEqualsJsonCompare(String message, 
			String expJsonString, Object gotObject,
			String[] ignoreFields) throws  IOException {
		AssertObject.assertEqualsJsonCompare(message, expJsonString, gotObject, ignoreFields);
	}
	
	/**
	 * @deprecated  Use AssertObject method instead
	 */
	@Deprecated	
	public static void assertEqualsJsonCompare(String message, 
			String expJsonString, Object gotObject,
			Set<String> ignoreFields) throws  IOException {
		AssertObject.assertEqualsJsonCompare(message, expJsonString, gotObject, ignoreFields);		
	}

	/**
	 * @deprecated  Use AssertObject method instead
	 */
	@Deprecated
	public static void assertEqualsJsonCompare(String message, 
			String expJsonString, Object gotObject,
			Set<String> ignoreFields, boolean expJsonIsAlreadyPretty, Integer decimalsTolerance) throws  IOException {
		AssertObject.assertEqualsJsonCompare(message, expJsonString, gotObject, ignoreFields, expJsonIsAlreadyPretty, decimalsTolerance);		
	}

	/**
	 * @deprecated  Use AssertString method instead
	 */
	@Deprecated	
	public static void assertStringEquals(String message, String expString, String gotString) {
		AssertString.assertStringEquals(message, expString, gotString);
	}

	/**
	 * @deprecated  Use AssertString method instead
	 */
	@Deprecated
	public static void assertStringEquals(String expString, String gotString) {
		AssertString.assertStringEquals(expString, gotString);
	}

	/**
	 * @deprecated  Use AssertString method instead
	 */
	@Deprecated	
	public static void assertStringContains(String gotString, String expSubstring) {
		AssertString.assertStringContains(gotString, expSubstring);
	}

	/**
	 * @deprecated  Use AssertString method instead
	 */
	@Deprecated
	public static void assertStringContains(String message,
			String gotString, String expSubstring) {
		AssertString.assertStringContains(message, gotString, expSubstring);
	}

	/**
	 * @deprecated  Use AssertString method instead
	 */
	@Deprecated
	public static void assertStringContains(String message,
			String gotString, String pattern, Boolean caseSensitive) {
		AssertString.assertStringContains(message, gotString, pattern, caseSensitive);
	}
	
	/**
	 * @deprecated  Use AssertString method instead
	 */
	@Deprecated
	public static void assertStringContains(String message, String gotString, 
			String pattern, Boolean caseSensitive, Boolean isRegexp) {
		AssertString.assertStringContains(message, gotString, pattern, caseSensitive, isRegexp);
	}	
	
	/**
	 * @deprecated  Use AssertString method instead
	 */
	@Deprecated
	public static void assertStringDoesNotContain(String gotString, String unexpSubstring) {
		AssertString.assertStringDoesNotContain(gotString, unexpSubstring);
	}
	
	/**
	 * @deprecated  Use AssertString method instead
	 */
	@Deprecated
	public static void assertStringDoesNotContain(String message,
			String gotString, String unexpSubstring) {
		AssertString.assertStringDoesNotContain(message, gotString, unexpSubstring);
	}

	/**
	 * @deprecated  Use AssertString method instead
	 */
	@Deprecated
	public static void assertStringDoesNotContain(String message, String gotString, 
			String unexpSubstring, Boolean caseSensitive) {
		AssertString.assertStringDoesNotContain(message, gotString, unexpSubstring, caseSensitive);
	}
	
	/**
	 * @deprecated  Use AssertString method instead
	 */
	@Deprecated
	public static void assertStringDoesNotContain(String message, String gotString, 
			String unexpSubstring, Boolean caseSensitive, Boolean isRegexp) {
		AssertString.assertStringDoesNotContain(message, gotString, unexpSubstring, caseSensitive, isRegexp);
	}	
	
	/**
	 * @deprecated  Use AssertFile method instead
	 */
	@Deprecated
	public static void assertFileContains(String mess, File fPath, String pattern, 
			Boolean isCaseSensitive, Boolean isRegexp) throws IOException {
		AssertFile.assertFileContains(mess, fPath, pattern, isCaseSensitive, isRegexp);
	}

	/**
	 * @deprecated  Use AssertFile method instead
	 */
	public static void assertFileDoesNotContain(String mess, String fPath, String pattern, Boolean isRegexp) throws IOException {
		AssertFile.assertFileDoesNotContain(mess, fPath, pattern, isRegexp);	
	}
	
	/**
	 * @deprecated  Use AssertFile method instead
	 */
	public static void assertFileContentEquals(String mess, File file, String expFileContent) throws IOException {
		AssertFile.assertFileContentEquals(mess, file, expFileContent);
	}
	
	/**
	 * @deprecated  Use AssertFile method instead
	 */
	public static void assertFileContentStartsWith(String mess, File file, String expContentStart) throws IOException {
		AssertFile.assertFileContentStartsWith(mess, file, expContentStart);
	}
	
	/**
	 * @deprecated  Use AssertFile method instead
	 */
	public static void assertFileContentEndsWith(String mess, File file, String expContentEnd) throws IOException {
		AssertFile.assertFileContentEndsWith(mess, file, expContentEnd);
	}
	
	/**
	 * @deprecated  Use AssertFile method instead
	 */
	public static void assertFilesHaveSameContent(String mess, File file1, File file2) throws IOException {
		AssertFile.assertFilesHaveSameContent(mess, file1, file2);
	}
	
	/**
	 * @deprecated  Use AssertFile method instead
	 */
	public static void assertDirectoryHasFiles(String message, File dir, String[] expFiles) throws IOException {
		AssertFile.assertDirectoryHasFiles(message,  dir, expFiles);
	}

	/**
	 * @deprecated  Use AssertFile method instead
	 */
	public static void assertFilesEqual(String mess, String[] expFilesStr, File[] gotFiles, File rootDir) throws Exception {
		AssertFile.assertFilesEqual(mess, expFilesStr, gotFiles, rootDir);
	}

	public static <T> void assertContainsAll(String message, T[] supersetArr, T[] subsetArr) {
		Set<Object> superset = new HashSet<Object>();
		for (Object elt: supersetArr) superset.add(elt);
		assertContainsAll(message, superset, subsetArr);
	}

	public static <T> void assertContainsAll(String message, List<T> supersetList, T[] subsetArr) {
		Set<Object> superset = new HashSet<Object>();
		for (Object elt: supersetList) superset.add(elt);
		assertContainsAll(message, superset, subsetArr);
	}
	
	public  static <T> void  assertContainsAll(String message, Set<T> superset, T[] subsetArr) {
		
		int ii = 0;
		for (Object elt: subsetArr) {
			if (!superset.contains(elt)) {
				message = message + ("\nElement " + ii + "(" + elt + ") of the second collection is absent from the first collection\n"
				+ "First collection:\n" + PrettyPrinter.print(superset) + "\n"
				+ "\n"
				+ "Second collection:\n" + PrettyPrinter.print(subsetArr) + "\n")
						;
					Assertions.fail(message);

			}
			ii++;
		}
		
		Set<Object> subset = new HashSet<Object>();
		for (Object elt: subsetArr) subset.add(elt);
		
		if (!superset.containsAll(subset)) {
		}
	}
	
	public static void intersectionNotEmpty(String mess, 
			Set<Object> set1,
			Set<Object> set2) throws IOException {
		Set<Object> gotIntersection = new HashSet<Object>();
		for (Object obj: set2) {
			if (set1.contains(obj)) {
				gotIntersection.add(obj);
			}
		}
		
		Set<Object> emptySet = new HashSet<Object>();
		AssertObject.assertDeepNotEqual(
				mess+"\nIntersection of sets was empty.", 
				emptySet, gotIntersection);
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
	
	public static <T> void assertUnOrderedSameElements(String failMessageStart, T[] exp, List<T> got) throws IOException {
		List<T> expList = new ArrayList<T>();
		for (T elt: exp) {
			expList.add(elt);
		}
		assertUnOrderedSameElements(failMessageStart, expList, got);
	}
	
	public static <T> void assertUnOrderedSameElements(String failMessageStart, List<T> exp, T[] got) throws IOException {
		List<T> gotList = new ArrayList<T>();
		for (T elt: exp) {
			gotList.add(elt);
		}
		assertUnOrderedSameElements(failMessageStart, exp, gotList);
	}

	public static <T> void assertUnOrderedSameElements(String failMessageStart, T[] exp, Set<T> got) throws IOException {
		List<T> expList = new ArrayList<T>();
		for (T elt: exp) {
			expList.add(elt);
		}
		List<T> gotList = new ArrayList<T>();
		for (T elt: exp) {
			gotList.add(elt);
		}
		assertUnOrderedSameElements(failMessageStart, expList, gotList);
	}
	
	public static <T> void assertUnOrderedSameElements(String failMessageStart, Set<T> exp, T[] got) throws IOException {
		List<T> expList = new ArrayList<T>();
		for (T elt: exp) {
			expList.add(elt);
		}
		List<T> gotList = new ArrayList<T>();
		for (T elt: exp) {
			gotList.add(elt);
		}
		assertUnOrderedSameElements(failMessageStart, expList, gotList);
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
			
			Assertions.fail(sizeMessage.toString());
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
				
				Assertions.fail(message.toString());
			}						
		}
	}
	
	public static <T> void assertIsSubsetOf(String mess, T[] elts1, T[] elts2) {
		assertBothNullOrNone(mess, elts1, elts2);
		
		Set<T> eltsSet1 = new HashSet<T>();
		if (elts1 != null) {
			eltsSet1.addAll(Arrays.asList(elts1));
		}
		
		Set<T> eltsSet2 = new HashSet<T>();
		if (elts2 != null) {
			eltsSet2.addAll(Arrays.asList(elts2));
		}
		
		assertIsSubsetOf(mess, eltsSet1, eltsSet2);
	}
	
	private static void assertBothNullOrNone(String mess, Object obj1, Object obj2) {
		if (obj1 == null && obj2 != null) {
			Assertions.fail(mess+"\n\nFirst object was null, but second was not");
		}
		if (obj1 != null && obj2 == null) {
			Assertions.fail(mess+"\n\nSecond object was null, but first was not");
		}
	}

	public static <T> void assertIsSubsetOf(String mess, Set<T> set1, Set<T> set2) {
		if (set1 == null && set1 != null) {
			Assertions.fail("First set was null, but not the second one.");
		}
		
		for (T elt: set1) {
			if (!set2.contains(elt)) {
				Assertions.fail(mess+"\n\nElement "+elt+" was present in the first set but not in the second one");
			}
		}
	}

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
		Assertions.assertTrue(
			gotValue >= minValue,
			message+"\nValue: "+ gotValue + " was smaller than expexected min value: " + minValue);
		Assertions.assertTrue(
			gotValue <= maxValue,
			message+"\nValue: "+ gotValue + " was greater than expexected max value: " + maxValue);
	}

	public static void assertFileContentIs(String message, String expContent, File filePath) throws IOException {
		assertFileContentIs(message, expContent, filePath.toString());
	}

	
	public static void assertFileContentIs(String message, String expContent, String filePath) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(new File(filePath)));
		
		String gotContent = "";
		while(true) {
			String line = reader.readLine();
			if (line == null) break;
			if (!gotContent.isEmpty()) gotContent += "\n";
			gotContent += line;
		}
		reader.close();
		
		assertStringEquals(message+"\nContent of file '"+filePath+"' was not as expected.", 
				expContent, gotContent);
	}

	public static void compareNumbers(String mess, Number gotN, Comp comparison, Number nBeforeDate) {
		if (comparison == Comp.AT_LEAST) {
			assertAtLeast(mess, gotN, nBeforeDate);
		} else if (comparison == Comp.AT_MOST) {
			assertAtMost(mess, gotN, nBeforeDate);	
		}
		
	}

	private static void assertAtLeast(String mess, Number gotN, Number minN) {
		mess += "\nNumber "+gotN+" should have been at least "+minN;
		if (gotN instanceof Integer) {
			Assertions.assertTrue(gotN.intValue() >= minN.intValue(), mess);
		}
	}

	private static void assertAtMost(String mess, Number gotN, Number maxN) {
		mess += "\nNumber "+gotN+" should have been at most "+maxN;
		if (gotN instanceof Integer) {
			Assertions.assertTrue(gotN.intValue() <= maxN.intValue(), mess);
		}
	}

	public static void assertElapsedLessThan(String mess, long start, long maxSecs) {
		long elapsed = System.currentTimeMillis() - start;
		if (elapsed > maxSecs) {
			Assertions.fail(mess+"\nElapsed time of "+elapsed+"secs was longer than expected (max="+maxSecs+"secs).");
		}
	}
}
