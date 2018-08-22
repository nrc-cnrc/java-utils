package ca.nrc.json;

//import static ca.nrc.testing.Assert.assertEquals;
import static org.junit.Assert.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ca.nrc.json.PrettyPrinter;

//import ca.nrc.testing.AssertHelpers;
//import ca.nrc.file.Tracer;

public class PrettyPrinterTest {
	
	private static class NonComparable extends Object{
		public NonComparable() {}
	}
	
	private static class Hello extends Object{
		public String greeting;
		public Hello(String _greeting) {this.greeting = _greeting;}
	}
	
	private static class Person extends Object {
		public String name;
		public Person(String _name) {
			this.name = _name;
		}
	}	

	private static class MarriedPerson extends Person {
		public MarriedPerson spouse;
		public Person[] children;
		public Person[] dependantChildren;
		public MarriedPerson(String _name) {
			super(_name);
		}
	}
	
	private static class ClassWithStaticAttr extends Object {
		public static String staticAttr = "Static Value";
		public String nonStaticAttr;
		public ClassWithStaticAttr(String _nsAttr) {
			this.nonStaticAttr = _nsAttr;
		}
	}


	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}
	
	/*************************************************************
	 * DOCUMENTATION TESTS
	 *************************************************************/

	@Test
	public void test__PrettyPrinter__Synopsis() {
		//
		// Use the PrettyPrinter to create json strings for diffferent
		// data structures and objects. The string will be formatted
		// to be easily read and parsed by a human.
		//
		// For example, you can pretty print complex data structures
		// like this.
		//
		ArrayList<HashMap<String,Integer>> complexStructure = new ArrayList<HashMap<String,Integer>>();
		String jsonString = PrettyPrinter.print(complexStructure);
		
		//
		// You can alsos pretty print objects.
		//
		class SomeClass {
			public String someField;
			public Integer someOtherFfield;
		}
		SomeClass anObject = new SomeClass();
		jsonString = PrettyPrinter.print(anObject);
		
		//
		// In some cases, you may not want the pretty printer to 
		// print certain fields of an object.
		//
		String[] fieldsToIgnore = new String[] {"someField"};
		jsonString = PrettyPrinter.print(anObject, fieldsToIgnore);
	}
	

	/*************************************************************
	 * VERIFICATION TESTS
	 *************************************************************/

	@Test
	public void test__print__String() {
		String str = "hello world";
		String gotJson = PrettyPrinter.print(str);
		String expJson = "\"hello world\"";
		Assert.assertEquals("", expJson, gotJson);
	}
	
	@Test
	public void test__print__Object() {
		class SomeClass {
			public String someField;
			public Integer someOtherField;
			public SomeClass(String _str, Integer _int) {
				someField = _str;
				someOtherField = _int;
			}
		}
		SomeClass anObject = new SomeClass("hello", 13);
		
		String gotJson = PrettyPrinter.print(anObject);
		String expJson = 
				"{\n"
			+ "  \"someField\":\n"
			+ "    \"hello\",\n"
			+ "  \"someOtherField\":\n"
			+ "    13\n"
			+ "}"
				;
		
		Assert.assertEquals(expJson, gotJson);	
	}
	
	@Test
	public void test__print__StringWithADoubleQuoteInIt__MustPutBackslashInFrontOfQuotes() {
		String someString = "He said: \"Hello World\".";
		
		String gotJson = PrettyPrinter.print(someString);
		String expJson = "\"He said: \\\"Hello World\\\".\"";
		
		Assert.assertEquals(expJson, gotJson);
	}	
	
	@Test
	public void test__print__NullValue() {
		String gotJson = PrettyPrinter.print(null);
		String expJson = "null";
		
		Assert.assertEquals(expJson, gotJson);
		
	}	
	
	@Test
	public void test__print__ListOfStrings() {
		List<String> list = new ArrayList<String>();
		list.add("hello");
		list.add("world");
		String gotJson = PrettyPrinter.print(list);
		String expJson = "[\n  \"hello\",\n  \"world\"\n]";
		Assert.assertEquals("", expJson, gotJson);
	}

	@Test
	public void test__print__SetOfStrings() {
		Set<String> set = new HashSet<String>();
		set.add("homer");
		set.add("marge");
		set.add("bart");
		set.add("lisa");
		set.add("moe");
		String gotJson = PrettyPrinter.print(set);
		
		String expJson = 
				"[\n" +
				"  \"bart\",\n"+
				"  \"homer\",\n"+
				"  \"lisa\",\n"+
				"  \"marge\",\n"+
				"  \"moe\"\n"+
				"]";

		Assert.assertEquals("", expJson, gotJson);
	}
	
	@Test
	public void test__print__MapOfStringsToInt() {
		Map<String,Integer> map = new HashMap<String,Integer>();
		map.put("xyz", -123);
		map.put("abc", 456);
		String gotJson = PrettyPrinter.print(map);
		
		String expJson = 
				"{\n" +
				"  \"abc\":\n"+
				"    456,\n" +
				"  \"xyz\":\n"+
				"    -123\n"+
				"}";

		Assert.assertEquals("", expJson, gotJson);
	}	

	@Test
	public void test__print__EmptyMap() {
		Map<String,Integer> map = new HashMap<String,Integer>();
		String gotJson = PrettyPrinter.print(map);
		
		String expJson = 
				"{\n" +
				"}";

		Assert.assertEquals("", expJson, gotJson);
	}	
	@Test
	public void test__print__MapOfNonComparablesToStrings() {
		Map<Hello,String> map = new HashMap<Hello,String>();
		map.put(new Hello("hi"), "xyz");
		map.put(new Hello("greetings"), "abc");
		String gotJson = PrettyPrinter.print(map);

		// We can't check the expectations, because we have no idea how
		// the keys will have been sorted.
	}	
	
	@Test
	public void test__print__ComplexDataStructure() {
		ArrayList<HashMap<String,Integer>> complexStructure = new ArrayList<HashMap<String,Integer>>();
		
		HashMap<String,Integer> map1 = new HashMap<String,Integer>();
		map1.put("height", 102);
		map1.put("width", 50);
		complexStructure.add(map1);
		
		HashMap<String,Integer> map2 = new HashMap<String,Integer>();
		map2.put("height", 143);
		map2.put("width", 67);
		complexStructure.add(map2);
		
		String gotJson = PrettyPrinter.print(complexStructure);
		String expJson = 
				"[\n"
			+ "  {\n"
			+ "    \"height\":\n"
			+ "      102,\n"
			+ "    \"width\":\n"
			+ "      50\n"
			+ "  },\n"
			+ "  {\n"
			+ "    \"height\":\n"
			+ "      143,\n"
			+ "    \"width\":\n"
			+ "      67\n"
			+ "  }\n"

			+ "]"
				;
		
		Assert.assertEquals(expJson, gotJson);
	}
	

	@Test
	public void test__print__Object__IgnoreSomeFields() {
		class SomeClass {
			public String someField;
			public Integer someOtherField;
		}
		String[] ignoreFields = new String[] {"someField"};
		SomeClass anObject = new SomeClass();
		
		String gotJson = PrettyPrinter.print(anObject, ignoreFields);
		String expJson = 
				"{\n"
			+ "  \"someOtherField\":\n"
			+ "    null\n"
			+ "}"
				;
		
		Assert.assertEquals(expJson, gotJson);
		
	}

	@Test
	public void test__print__ArrayOfArrays() {
		ArrayList<ArrayList<String>> array = new ArrayList<ArrayList<String>>();
		
		ArrayList<String> subArr1 = new ArrayList<String>();
		subArr1.add("hello");
		subArr1.add("world");
		array.add(subArr1);

		ArrayList<String> subArr2 = new ArrayList<String>();
		subArr2.add("greetings");
		subArr2.add("universe");
		array.add(subArr2);

		String gotJson = PrettyPrinter.print(array);
		
		String expJson = 
				"[\n" +
				"  [\n" +
				"    \"hello\",\n" +
				"    \"world\"\n" +
				"  ],\n" +
				"  [\n" +
				"    \"greetings\",\n" +
				"    \"universe\"\n" +
				"  ]\n" +
				"]";
		Assert.assertEquals(expJson, gotJson);
	}

	@Test
	public void test__print__SetOfComparables() {
		Set<String> set = new HashSet<String>();
		set.add("xyz");
		set.add("abc");
		String gotJson = new PrettyPrinter().print(set);
		String expJson = "[\n  \"abc\",\n  \"xyz\"\n]";
		Assert.assertEquals("", expJson, gotJson);
	}
	
	@Test
	public void test__print__DataStructureWithLoops__ShouldNotCauseAStackOverflow() {
		MarriedPerson homer = new MarriedPerson("homer");
		MarriedPerson marge = new MarriedPerson("marge");
		homer.spouse = marge;
		marge.spouse = homer;
		String gotJson = new PrettyPrinter().print(homer);
		String expJson = 
				"{\n"+
				"  \"children\":\n" +
				"    null,\n" +
				"  \"dependantChildren\":\n" +
				"    null,\n" +
		        "  \"name\":\n" +
				"    \"homer\",\n" +
				"  \"spouse\":\n" +
				"    {\n" +
				"      \"children\":\n" +
				"        null,\n" +
				"      \"dependantChildren\":\n" +
				"        null,\n" +
		        "      \"name\":\n" +
				"        \"marge\",\n" +
		        "      \"spouse\":\n" +
				"        <OBJECT ALREADY SEEN. Not printing again to avoid infinite recursion>\n" +
				"    }\n" +
		        "}"
				;
		Assert.assertEquals(expJson, gotJson);
	}
	
	@Test
	public void test__print__DataStructureWithRepeatedObjectsButNoLoops__ShoulPrintRepeatedObjects() {
		MarriedPerson joe = new MarriedPerson("joe");
		Person jane = new Person("jane");
		Person john = new Person("john");
		
		joe.children = new Person[] {jane, john};
		joe.dependantChildren = new Person[] {john};
		
		String gotJson = new PrettyPrinter().print(joe);
		String expJson = 
				"{\n"+
		        "  \"children\":\n" +
				"    [\n" +
		        "      {\n" +
				"        \"name\":\n" +
		        "          \"jane\"\n" +
				"      },\n" +
		        "      {\n" +
				"        \"name\":\n" +
		        "          \"john\"\n" +
				"      }\n" +
		        "    ],\n" +
		        "  \"dependantChildren\":\n" +
				"    [\n" +
		        "      {\n" +
				"        \"name\":\n" +
		        "          \"john\"\n" +
				"      }\n" +
		        "    ],\n" +
		        "  \"name\":\n" +
				"    \"joe\",\n" +				
		        "  \"spouse\":\n" +
				"    null\n" +				
		        "}"
				;
		Assert.assertEquals(expJson, gotJson);
	}	
	
	@Test
	public void test__print__ClassWithStaticAttribute__DoesNotPrintTheStaticAtttribute() throws IOException {
		ClassWithStaticAttr obj = new ClassWithStaticAttr("non-static value");
		String gotJson = new PrettyPrinter().print(obj);
		String expJson = 
				"{\n"+
				"  \"nonStaticAttr\":\n" +
				"    \"non-static value\"\n" +
		        "}"
				;
		Assert.assertEquals(expJson, gotJson);		
	}
	
	@Test
	public void test_print__int() {
		int num = 10;
		String gotJson = new PrettyPrinter().print(num);
		String expJson =  "10";
		Assert.assertEquals(expJson, gotJson);		
	}	
	
	@Test
	public void test_print__double() {
		double num = 0.17437343;
		
		String gotJson = PrettyPrinter.print(num);
		String expJson =  "0.17437343";
		Assert.assertEquals(expJson, gotJson);		

		// Now we ask for 4 decimals;
		int decimals = 4;
		gotJson = PrettyPrinter.print(num, decimals);
		// In th to 4 decimals
		expJson =  "0.1744";
		Assert.assertEquals(expJson, gotJson);				
	}	
	
	@Test
	public void test_print__Double() {
		Double num = 0.17437343;
		
		String gotJson = PrettyPrinter.print(num);
		String expJson =  "0.17437343";
		Assert.assertEquals(expJson, gotJson);		

		// Now we ask for 4 decimals;
		int decimals = 4;
		gotJson = PrettyPrinter.print(num, decimals);
		// In th to 4 decimals
		expJson =  "0.1744";
		Assert.assertEquals(expJson, gotJson);				
	}	

	@Test
	public void test_print__DoubleArray() {
		Double[] arr = new Double[] {0.1744, 12.3427};
		
		String gotJson = PrettyPrinter.print(arr);
		String expJson =  
				  "[\n"
				+ "  0.1744,\n"
				+ "  12.3427\n"
				+ "]"
				  ;
		Assert.assertEquals(expJson, gotJson);		

		// Now we ask for 2 decimals;
		int decimals = 2;
		gotJson = PrettyPrinter.print(arr, decimals);
		// In th to 4 decimals
		expJson = 
				  "[\n"
				+ "  0.17,\n"
				+ "  12.34\n"
				+ "]"
				  ;
				
		Assert.assertEquals(expJson, gotJson);				
	}	
	
	@Test
	public void test_print__doubleArray() {
		double[] arr = new double[] {0.1744, 12.3427};
		
		String gotJson = PrettyPrinter.print(arr);
		String expJson =  
				  "[\n"
				+ "  0.1744,\n"
				+ "  12.3427\n"
				+ "]"
				  ;
		Assert.assertEquals(expJson, gotJson);		

		// Now we ask for 2 decimals;
		int decimals = 2;
		gotJson = PrettyPrinter.print(arr, decimals);
		// In th to 4 decimals
		expJson = 
				  "[\n"
				+ "  0.17,\n"
				+ "  12.34\n"
				+ "]"
				  ;
				
		Assert.assertEquals(expJson, gotJson);				
	}	
	
	@Test
	public void test_print__floatArray() {
		float[] arr = new float[] {0.1744f, 12.3427f};
		
		String gotJson = PrettyPrinter.print(arr);
		String expJson =  
				  "[\n"
				+ "  0.1744,\n"
				+ "  12.3427\n"
				+ "]"
				  ;
		Assert.assertEquals(expJson, gotJson);		

		// Now we ask for 2 decimals;
		int decimals = 2;
		gotJson = PrettyPrinter.print(arr, decimals);
		// In th to 4 decimals
		expJson = 
				  "[\n"
				+ "  0.17,\n"
				+ "  12.34\n"
				+ "]"
				  ;
				
		Assert.assertEquals(expJson, gotJson);				
	}		
	
	@Test
	public void test_print__intArray() {
		int[] arr = new int[] {1, 2, 100};
		
		String gotJson = PrettyPrinter.print(arr);
		String expJson =  
				  "[\n"
				+ "  1,\n"
				+ "  2,\n"
				+ "  100\n"
				+ "]"
				  ;
		Assert.assertEquals(expJson, gotJson);		
	}			

	@Test
	public void test_print__longArray() {
		long[] arr = new long[] {1, 2, 100};
		
		String gotJson = PrettyPrinter.print(arr);
		String expJson =  
				  "[\n"
				+ "  1,\n"
				+ "  2,\n"
				+ "  100\n"
				+ "]"
				  ;
		Assert.assertEquals(expJson, gotJson);		
	}				
	@Test
	public void test__checkForLoops__SequenceThatDoesNOTIntroduceALoop() {
		Hello obj1 = new Hello("hi");
		Hello obj2 = new Hello("greetings");
		
		PrettyPrinter printer = new PrettyPrinter();
		
		boolean gotAnswer = printer.checkForLoops(obj1);
		assertFalse(gotAnswer);
		
		gotAnswer = printer.checkForLoops(obj2);
		assertFalse(gotAnswer);
		
		// Assume we are done with printing obj2
		printer.removeFromAlreadySeen(obj2);
		
		// At this point, we shouldn't have a loop, even
		// if we are re-printing obj2
		gotAnswer = printer.checkForLoops(obj2);
		assertFalse(gotAnswer);
		
	}	
	
	@Test
	public void test__checkForLoops__SequenceThatIntroducesALoop() {
		Hello obj1 = new Hello("hi");
		Hello obj2 = new Hello("greetings");
		
		PrettyPrinter printer = new PrettyPrinter();
		
		boolean gotAnswer = printer.checkForLoops(obj1);
		assertFalse(gotAnswer);
		
		gotAnswer = printer.checkForLoops(obj2);
		assertFalse(gotAnswer);

		// This one should find a loop, because obj1 is repeated
		gotAnswer = printer.checkForLoops(obj1);
		assertTrue(gotAnswer);
	}
	
	@Test
	public void test__getAllFields__ClassWithAParentClass() throws IOException {
		MarriedPerson homer = new MarriedPerson("homer");
		List<Field> gotFields = PrettyPrinter.getAllFields(homer);
		
		String[] expFieldNames = new String[] {"spouse", "children", "dependantChildren", "name"};
		assertFieldNamesAre(expFieldNames, gotFields);
		
	}

	@Test
	public void test__getAllFields__ClassWithoutAParentClass() throws IOException {
		Hello hello = new Hello("hi");
		List<Field> gotFields = PrettyPrinter.getAllFields(hello);
		
		String[] expFieldNames = new String[] {"greeting"};
		assertFieldNamesAre(expFieldNames, gotFields);		
	}
	
	/*************************************************************
	 * TEST HELPERS
	 * @throws IOException 
	 *************************************************************/
	
	private void assertFieldNamesAre(String[] expFieldNames, List<Field> gotFields) throws IOException {
		
		// Note: We can't use AssertHelpers.assertDeepEquals here, because it depends on the PrettyPrinter
		//   class that we are testing here.
		List<String> gotFieldNames = new ArrayList<String>();
		for (Field aField: gotFields) {
			gotFieldNames.add(aField.getName());
		}
		
		String errMess = "\nExp : "+String.join(", ", expFieldNames)+"\nGot : "+String.join(", ", gotFieldNames);
		if (expFieldNames.length != gotFieldNames.size()) {
			fail("The two lists of field names did not have the same length."+errMess);
		}
		for (int ii=0; ii < expFieldNames.length; ii++) {
			assertEquals("Field name no "+ii+" differed."+errMess, expFieldNames[ii], gotFieldNames.get(ii));
		}
	}
	
	
}
