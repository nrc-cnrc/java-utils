package ca.nrc.json;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ca.nrc.introspection.IntrospectionTest;
import ca.nrc.testing.AssertObject;
import ca.nrc.testing.AssertString;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;

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

	public static class SomeComparableClass
		implements Comparable<SomeComparableClass> {

		public String keyField = null;
		public String otherField = null;

		public SomeComparableClass(String _key, String _other) {
			this.keyField = _key;
			this.otherField = _other;
		}

		@Override
		public int compareTo(SomeComparableClass o) {
			return this.keyField.compareTo(o.keyField);
		}
	}

	@BeforeEach
	public void setUp() throws Exception {
	}

	@AfterEach
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
		Assertions.assertEquals(expJson, gotJson, "");
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
		
		Assertions.assertEquals(expJson, gotJson);	
	}

	@Test
	public void test__print__Class() {
		class SomeClass {
		}

		String gotJson = PrettyPrinter.print(SomeClass.class);
		String expJson ="\"Class<ca.nrc.json.PrettyPrinterTest$3SomeClass>\"";

		Assertions.assertEquals(expJson, gotJson);
	}

	@Test
	public void test__print__StringWithADoubleQuoteInIt__MustPutBackslashInFrontOfQuotes() {
		String someString = "He said: \"Hello World\".";
		
		String gotJson = PrettyPrinter.print(someString);
		String expJson = "\"He said: \\\"Hello World\\\".\"";
		
		Assertions.assertEquals(expJson, gotJson);
	}	
	
	@Test
	public void test__print__NullValue() {
		String gotJson = PrettyPrinter.print(null);
		String expJson = "null";
		
		Assertions.assertEquals(expJson, gotJson);
		
	}	
	
	@Test
	public void test__print__ListOfStrings() {
		List<String> list = new ArrayList<String>();
		list.add("hello");
		list.add("world");
		String gotJson = PrettyPrinter.print(list);
		String expJson = "[\n  \"hello\",\n  \"world\"\n]";
		Assertions.assertEquals(expJson, gotJson);
	}

	@Test
	public void test__print__StringThatContainsDoubleQuotes() {
		String hello = "I said \"hello\"";
		String gotJson = PrettyPrinter.print(hello);
		String expJson = "\"I said \\\"hello\\\"\"";
		AssertString.assertStringEquals(expJson, gotJson);
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

		Assertions.assertEquals(expJson, gotJson);
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

		Assertions.assertEquals(expJson, gotJson);
	}	

	@Test
	public void test__print__EmptyMap() {
		Map<String,Integer> map = new HashMap<String,Integer>();
		String gotJson = PrettyPrinter.print(map);
		
		String expJson = 
				"{\n" +
				"}";

		Assertions.assertEquals(expJson, gotJson);
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
		
		Assertions.assertEquals(expJson, gotJson);
	}
	

	@Test
	public void test__print__ObjectOrMap__IgnoreSomeFields() throws Exception {
		class SomeClass {
			public String stringField;
			public Integer integerField;
			public Long longField;
			public Double doubleField;
			public Boolean booleanField;
			public List<String> listField;
			public Set<String> hashsetField;
			public String[] arrrField;
			public Map<String,String> mapField;
		}

		SomeClass anObject = new SomeClass();

		Map<String,Object> expEntries = new HashMap<String,Object>();
		String[] fieldNames = new String[] {
			"stringField", "integerField", "longField", "doubleField",
			"booleanField", "listField", "hashsetField", "arrrField", "mapField"
		};
		for (String fieldName: fieldNames) {
			expEntries.put(fieldName, null);
		}
		for (String fieldToIgnore: fieldNames) {
			HashSet<String> ignoreFields = new HashSet<String>();
			ignoreFields.add(fieldToIgnore);
			String gotJson = PrettyPrinter.print(anObject, ignoreFields);
			assertJsonFieldsAre(
				"Field "+fieldToIgnore+" not properly filtered",
				gotJson, fieldNames, fieldToIgnore);
		}
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
		Assertions.assertEquals(expJson, gotJson);
	}

	@Test
	public void test__print__SetOfPrimitiveComparables() {
		Set<String> set = new HashSet<String>();
		set.add("xyz");
		set.add("abc");
		String gotJson = new PrettyPrinter().print(set);
		String expJson = "[\n  \"abc\",\n  \"xyz\"\n]";
		Assertions.assertEquals(expJson, gotJson);
	}

	@Test
	public void test__print__SetOfNonPrimitiveComparables() {
		Set<SomeComparableClass> set = new HashSet<SomeComparableClass>();
		{
			set.add(new SomeComparableClass("salutations", "universe"));
			set.add(new SomeComparableClass("hello", "world"));
			set.add(new SomeComparableClass("greetings", "earth"));
		}
		String gotPrint = PrettyPrinter.print(set);
		String expPrint =
			"[\n" +
			"  {\n" +
			"    \"keyField\":\n" +
			"      \"greetings\",\n" +
			"    \"otherField\":\n" +
			"      \"earth\"\n" +
			"  },\n" +
			"  {\n" +
			"    \"keyField\":\n" +
			"      \"hello\",\n" +
			"    \"otherField\":\n" +
			"      \"world\"\n" +
			"  },\n" +
			"  {\n" +
			"    \"keyField\":\n" +
			"      \"salutations\",\n" +
			"    \"otherField\":\n" +
			"      \"universe\"\n" +
			"  }\n" +
			"]";
		AssertString.assertStringEquals(expPrint, gotPrint);
		return;
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
		Assertions.assertEquals(expJson, gotJson);
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
		Assertions.assertEquals(expJson, gotJson);
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
		Assertions.assertEquals(expJson, gotJson);		
	}
	
	@Test
	public void test_print__int() {
		int num = 10;
		String gotJson = new PrettyPrinter().print(num);
		String expJson =  "10";
		Assertions.assertEquals(expJson, gotJson);		
	}	
	
	@Test
	public void test_print__double() {
		double num = 0.17437343;
		
		String gotJson = PrettyPrinter.print(num);
		String expJson =  "0.17437343";
		Assertions.assertEquals(expJson, gotJson);		

		// Now we ask for 4 decimals;
		int decimals = 4;
		gotJson = PrettyPrinter.print(num, decimals);
		// In th to 4 decimals
		expJson =  "0.1744";
		Assertions.assertEquals(expJson, gotJson);				
	}	
	
	@Test
	public void test_print__Double() {
		Double num = 0.17437343;
		
		String gotJson = PrettyPrinter.print(num);
		String expJson =  "0.17437343";
		Assertions.assertEquals(expJson, gotJson);		

		// Now we ask for 4 decimals;
		int decimals = 4;
		gotJson = PrettyPrinter.print(num, decimals);
		// In th to 4 decimals
		expJson =  "0.1744";
		Assertions.assertEquals(expJson, gotJson);				
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
		Assertions.assertEquals(expJson, gotJson);		

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
				
		Assertions.assertEquals(expJson, gotJson);				
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
		Assertions.assertEquals(expJson, gotJson);		

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
				
		Assertions.assertEquals(expJson, gotJson);				
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
		Assertions.assertEquals(expJson, gotJson);		

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
				
		Assertions.assertEquals(expJson, gotJson);				
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
		Assertions.assertEquals(expJson, gotJson);		
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
		Assertions.assertEquals(expJson, gotJson);		
	}

	@Test
	public void test_print__StringWithSpecialChars() {
		String str = "quoted word: \"hello\" backslash char: \\";

		String gotJson = PrettyPrinter.print(str);
		String expJson = "\"quoted word: \\\"hello\\\" backslash char: \\\\\"";
		Assertions.assertEquals(expJson, gotJson);
	}

	@Test
	protected void test__print__Object__IgnorePrivateFields() {
		IntrospectionTest.SomeClass obj = new IntrospectionTest.SomeClass();
		PrettyPrinter pprinter =
			new PrettyPrinter(PrettyPrinter.OPTION.IGNORE_PRIVATE_PROPS);
		String gotJson = pprinter.pprint(obj);
		String expJson =
			"{\n" +
			"  \"privInstPropWithAccessor\":\n" +
			"    \"priv inst prop with accessor\",\n" +
			"  \"pubInstProp\":\n" +
			"    \"pub inst prop\"\n" +
			"}"
			;
		AssertString.assertStringEquals(
			"Pretty printed json was wrong for class with private props",
			expJson, gotJson
		);
	}


	@Test
	public void test__checkForLoops__SequenceThatDoesNOTIntroduceALoop() {
		Hello obj1 = new Hello("hi");
		Hello obj2 = new Hello("greetings");
		
		PrettyPrinter printer = new PrettyPrinter();
		
		boolean gotAnswer = printer.checkForLoops(obj1);
		Assertions.assertFalse(gotAnswer);
		
		gotAnswer = printer.checkForLoops(obj2);
		Assertions.assertFalse(gotAnswer);
		
		// Assume we are done with printing obj2
		printer.removeFromAlreadySeen(obj2);
		
		// At this point, we shouldn't have a loop, even
		// if we are re-printing obj2
		gotAnswer = printer.checkForLoops(obj2);
		Assertions.assertFalse(gotAnswer);
		
	}	
	
	@Test
	public void test__checkForLoops__SequenceThatIntroducesALoop() {
		Hello obj1 = new Hello("hi");
		Hello obj2 = new Hello("greetings");
		
		PrettyPrinter printer = new PrettyPrinter();
		
		boolean gotAnswer = printer.checkForLoops(obj1);
		Assertions.assertFalse(gotAnswer);
		
		gotAnswer = printer.checkForLoops(obj2);
		Assertions.assertFalse(gotAnswer);

		// This one should find a loop, because obj1 is repeated
		gotAnswer = printer.checkForLoops(obj1);
		Assertions.assertTrue(gotAnswer);
	}
	
	@Test
	public void test__getAllFields__ClassWithAParentClass() throws IOException {
		MarriedPerson homer = new MarriedPerson("homer");
		List<Field> gotFields = new PrettyPrinter().getAllFields(homer);
		
		String[] expFieldNames = new String[] {"spouse", "children", "dependantChildren", "name"};
		assertFieldNamesAre(expFieldNames, gotFields);
		
	}

	@Test
	public void test__getAllFields__ClassWithoutAParentClass() throws IOException {
		Hello hello = new Hello("hi");
		List<Field> gotFields = new PrettyPrinter().getAllFields(hello);
		
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
			Assertions.fail("The two lists of field names did not have the same length."+errMess);
		}
		for (int ii=0; ii < expFieldNames.length; ii++) {
			Assertions.assertEquals(
				expFieldNames[ii], gotFieldNames.get(ii),
				"Field name no "+ii+" differed."+errMess);
		}
	}

	private void assertJsonFieldsAre(
		String mess, String json, String[] fieldsSuperset,
		String excludedField) throws Exception {
		Set<String> expFields = new HashSet<String>();
		for (String aField: fieldsSuperset) {
			if (!aField.equals(excludedField)) {
				expFields.add(aField);
			}
		}

		Map<String,Object> gotObj = new HashMap<String,Object>();
		gotObj = new ObjectMapper().readValue(json, gotObj.getClass());
		Set<String> gotFields = gotObj.keySet();
		AssertObject.assertDeepEquals(
			mess+"\nFields were not as expected in JSON string:\n"+json,
			expFields, gotFields
		);
	}
}
