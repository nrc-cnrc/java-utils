package ca.nrc.testing;

import static ca.nrc.testing.AssertHelpers.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

public class AssertHelpersTest {

	/*********************************************************
	 *  DOCUMENTATION TESTS
	 *********************************************************/
	@Test
	public void test__assertEqualsJsonCompare__Synopsis() throws JsonProcessingException, IOException {
		//
		// Say you are testing a method that returns a complex data
		// structure that looks like this: 
		//
		ArrayList<HashMap<String,String>> people = new ArrayList<HashMap<String,String>>();
		
		HashMap<String,String> person = new HashMap<String,String>();
		person.put("First", "Joe");
		person.put("Last", "Bloe");
		person.put("Age",  "32");
		people.add(person);
		
		person = new HashMap<String,String>();
		person.put("First", "Jane");
		person.put("Last", "Doe");
		person.put("Age",  "54");
		people.add(person);
		
		person = new HashMap<String,String>();
		person.put("First", "Homer");
		person.put("Last", "Simpson");
		person.put("Age",  "51");
		people.add(person);
		
		//
		// Now, you want to check the value of people against some hard-coded
		// expectations.
		// 
		// You could of course build the expectation as above. But it has
		// two disadvantages:
		// - It's long
		// - It's hard to see at a glance what the full expectations are
		//
		// But with assertEqualsJsonCompare(), you can instead create a string
		// that provides the expectations in JSON notation, and this tends to make
		// the expectation easier to write, and easier to read quickly.
		// 
		// For example:
		//
		String expJsonString =
				"["+
				"  {\"Age\": \"32\", \"First\": \"Joe\", \"Last\": \"Bloe\"},"+
				"  {\"Age\": \"54\", \"First\": \"Jane\",\"Last\": \"Doe\"},"+
				"  { \"Age\": \"51\", \"First\": \"Homer\", \"Last\": \"Simpson\"}"+
				"]"+
				"";
		assertEqualsJsonCompare(expJsonString, people);				
	}
	
	@Test(expected=AssertionError.class)
	public void test__assertStringEquals__Synopsis() {
		
		// Say you want to compare two very long strings that contain a lot of brackets...
		
		String expected = "[[hello,world], [], etc... [greetings,universe]]";
		String got = "[[hello,world], [], etc... [greetings,earth]]";
		
		// If you commpare them with assertEquals, it may be hard to see where the difference is
		// for two reasons:
		//
		// - The location of the first difference is highlighted with [], which can be hard
		//    to differentiate with the other brackets that are in the string.
		// - The diff will only show a few characters before and after the first diff, which
		//    makes it hard to tell where exactly that diff is inside the overall string
		//
		// So, instead of using assertEquals(), you should use assertStringEquals()
		//
		assertStringEquals(expected, got);
		
	
	}
	
	@Test
	public void test__assertUnOrderedSameElements_Synopsis(){
		//With two lists containing the same elements, in a different order
		List<String> a = new ArrayList<String>();
		a.add("one");
		a.add("two");
		List<String> b = new ArrayList<String>();
		b.add("two");
		b.add("one");
		
		//Determine if the two lists contain the same elements
		assertUnOrderedSameElements("Part of the assert error message occuring before the message generated by this function",
				a, b);
		
		String[] arrA = new String[]{"one", "two"};
		String[] arrB = new String[]{"two", "one"};
		assertUnOrderedSameElements("Part of the assert error message occuring before the message generated by this function",
				arrA, arrB);
	}
	
	@Test
	public void test__assertHashMapsEqualUnOrdered_Synopsis() throws Exception {
		//Two HashMaps containing the same content
		HashMap<String, String> a = new HashMap<>();
		a.put("one", "1");
		a.put("two", "2");
		HashMap<String, String> b = new HashMap<>();
		b.put("one", "1");
		b.put("two", "2");
		
		//Determine if the two HashMaps contain the same keys and values
		assertHashMapsEqualUnOrdered("Part of the assert error message occuring before the message generated by this function", 
				a,b);
	}
	
	/*********************************************************
	 *  VERIFICATION TESTS
	 *********************************************************/
	
	
	@Test
	public void test__assertStringEquals__IdenticalString__ShouldNotRaiseException() {
		String expected = "Hello world";
		String got = expected;
		
		assertStringEquals(expected, got);
	}
	
	@Test(expected=AssertionError.class)
	public void test__assertStringEquals__StringsDifferInMiddle__ShouldRaiseException() {
		String expected = "Hello world";
		String got = "Hello BEAUTIFULE world";
		
		assertStringEquals(expected, got);
	}
	
	@Test(expected=AssertionError.class)
	public void test__assertStringEquals__StringsDifferInBeginning__ShouldRaiseException() {
		String expected = "Hello world";
		String got = "I say: Hello world";
		
		assertStringEquals(expected, got);
	}
	
	@Test(expected=AssertionError.class)
	public void test__assertStringEquals__StringsDifferAtEnd__ShouldRaiseException() {
		String expected = "Hello world";
		String got = "Hello world, he said";
		
		assertStringEquals(expected, got);
	}
	
	@Test(expected=AssertionError.class)
	public void test__assertStringEquals__FirstStringEmptyButNotSecond__ShouldRaiseException() {
		String expected = "";
		String got = "Hello world";
		
		assertStringEquals(expected, got);
	}

	@Test(expected=AssertionError.class)
	public void test__assertStringEquals__FirstStringNonEmptyButSecondOneIs__ShouldRaiseException() {
		String expected = "Hello world";
		String got = "";
		
		assertStringEquals(expected, got);
	}

	@Test(expected=AssertionError.class)
	public void test__assertUnOrderedSameElements_SameElementsSomeRepeated_AndDifferentSize__ShouldRaiseException() {
		//Even though the two lists contain the same elements, some are duplicated so they do not contain the same elements
		//Also tests if different sized lists are considered equal
		List<String> first = new ArrayList<String>();
		first.add("one");
		first.add("one");
		first.add("two");
		List<String> second = new ArrayList<String>();
		second.add("one");
		second.add("two");
		assertUnOrderedSameElements("", first, second);
	}
	
	@Test(expected=AssertionError.class)
	public void test__assertUnOrderedSameElements_SameElementsSomeRepeated_AndDifferentSize_Array__ShouldRaiseException() {
		//Even though the two lists contain the same elements, some are duplicated so they do not contain the same elements
		//Also tests if different sized lists are considered equal
		String[] first = {"one","one", "two"};
		String[] second = {"one","two"};
		assertUnOrderedSameElements("", first, second);
	}
	
	@Test(expected=AssertionError.class)
	public void test__assertUnOrderedSameElements_DifferentElements__ShouldRaiseException() {
		//Two lists containing different elements
		List<String> first = new ArrayList<String>();
		first.add("one");
		first.add("two");
		List<String> second = new ArrayList<String>();
		second.add("one");
		second.add("three");
		assertUnOrderedSameElements("", first, second);
	}
	
	@Test(expected=AssertionError.class)
	public void test__assertUnOrderedSameElements_DifferentElements_Array__ShouldRaiseException() {
		//Two lists containing different elements
		String[] first = new String[]{"one", "two"};
		String[] second = new String[]{"one", "three"};
		assertUnOrderedSameElements("", first, second);
	}
	
	
	@Test(expected=AssertionError.class)
	public void test__assertHashMapsEqualUnOrdered__DifferentKeys() throws Exception {
		HashMap<String, String> a = new HashMap<>();
		a.put("one", "");
		a.put("two", "");
		HashMap<String, String> b = new HashMap<>();
		b.put("one", "");
		b.put("three", "");
		assertHashMapsEqualUnOrdered("",a,b);
	}
	
	@Test(expected=AssertionError.class)
	public void test__assertHashMapsEqualUnOrdered__SameKeysDifferentContent() throws IOException{
		HashMap<String, String> a = new HashMap<>();
		a.put("one", "1");
		a.put("two", "2");
		HashMap<String, String> b = new HashMap<>();
		b.put("one", "1");
		b.put("two", "different");
		assertHashMapsEqualUnOrdered("",a,b);
	}
	
	@Test
	public void test__assertHashMapsEqualUnOrdered__ListOrderIgnored() throws IOException{
		HashMap<String, List<String>> a = new HashMap<>();
		List<String> aList = new ArrayList<>();
		aList.add("one");
		aList.add("two");
		a.put("lst", aList);
		HashMap<String, List<String>> b = new HashMap<>();
		List<String> bList = new ArrayList<>();
		bList.add("two");
		bList.add("one");
		b.put("lst", bList);
		assertHashMapsEqualUnOrdered("",a,b);
	}
	
	@Test(expected=AssertionError.class)
	public void test__assertHashMapsEqualUnOrdered__ListsDifferent() throws IOException{
		HashMap<String, List<String>> a = new HashMap<>();
		List<String> aList = new ArrayList<>();
		aList.add("one");
		aList.add("two");
		a.put("lst", aList);
		HashMap<String, List<String>> b = new HashMap<>();
		List<String> bList = new ArrayList<>();
		bList.add("one");
		bList.add("three");
		b.put("lst", bList);
		assertHashMapsEqualUnOrdered("",a,b);		
	}
	
	@Test
	public void test__assertEqualsJsonCompare__NonEmptyArray() throws IOException {
		String expJson = "[\"hello\", \"world\"]";
		assertEqualsJsonCompare(expJson, new String[]{"hello", "world"});
	}	

	@Test
	public void test__assertEqualsJsonCompare__EmptyArray() throws IOException {
		assertEqualsJsonCompare("[]", new String[]{});
	}	
}