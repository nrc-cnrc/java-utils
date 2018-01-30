package ca.nrc.json;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ca.nrc.testing.AssertHelpers;
import ca.nrc.container.ResourceGetter;

public class ObjectStreamReaderTest {
	

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}
	
	/*************************************************
	 * DOCUMENTATION TESTS
	 *************************************************/

	@Test
	public void test__ObjectStreamReader__Synopsis() throws Exception {
		/*
		 * Use a ObjectStreamReader to read a stream of Json objects.
		 */
		
		/*
		 * You can create an ObjectStreamReader froma:
		 * - BufferedReader object
		 * - File object
		 * - File path
		 */

		String content = "class=ca.nrc.json.DummyClass1\n{}";
		ObjectStreamReader reader = new ObjectStreamReader(new StringReader(content));
		
		String fPath = ResourceGetter.getResourcePath("json_records/test_records.json");
		reader = new ObjectStreamReader(new File(fPath));
		
		reader = new ObjectStreamReader(fPath);
		
		/*
		 * The object stream must have the following structure
		 */
		content =
				  "// You can comment out any line with //.\n"
				+ "// \n"
				+ "// Blank lines are allowed anywhere EXCEPT inside the body of an object.\n"
				+ "//\n"
				+ "// Before the first object's body, you need to include a 'class=' line which\n"
				+ "// specifies the class of that object and all that follow.\n"
				+ "//\n"
				+ "class=ca.nrc.json.DummyClass1\n"
				+ "{\"greetings\": \"hello!\"}\n"
				+ "// You need to include at least one blank line between each object.\n"
				+ "\n"
				+ "{\"greetings\": \"ahoy!\"}\n"
				+ "\n"
				+ "// If the class of objects in the stream change, you simply insert\n"
				+ "// a different class= line before the body of the first object whose\n"
				+ "// type is different..\n"
				+ "class=ca.nrc.json.DummyClass2\n"
				+ "{\"name\": \"Homer Simpson\"}\n"
				+ "";

		reader = new ObjectStreamReader(new StringReader(content));
		
		// Once the reader has been instantiated with a stream, you 
		// can read its object one by one as follows...
		while (true) {
			Object obj = reader.readObject();
			if (obj == null) break;
			// do something with the object
		}
	}
	
	/*************************************************
	 * VERIFICATION TESTS
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 *************************************************/	

	@Test
	public void test__read__HappyPath() throws Exception {
		String json = 
				"class=ca.nrc.json.DummyClass1\n"
						+ "{\n"
						+ "  \"greetings\": \"hello world\"\n"
						+ "}\n"
						+ "\n"
						+ "class=ca.nrc.json.DummyClass2\n"
						+ "{\"name\": \"Homer Simpson\"}\n"
						;
		StringReader strReader = new StringReader(json);
		ObjectStreamReader reader = new ObjectStreamReader(strReader);
		
		Object obj =  reader.readObject();		
		assertTrue("", obj instanceof DummyClass1);
		String expJson = "{\"greetings\": \"hello world\"}";
		AssertHelpers.assertEqualsJsonCompare(expJson, obj);
		
		obj =  reader.readObject();		
		assertTrue("", obj instanceof DummyClass2);
		expJson = "{\"name\": \"Homer Simpson\"}";
		AssertHelpers.assertEqualsJsonCompare(expJson, obj);

		obj =  reader.readObject();		
		assertEquals(null, obj);
	}

	@Test
	public void test__read__SingleLineObjectAtEndOfStream() throws Exception {
		StringReader strReader = 
				new StringReader(
						  "class=ca.nrc.json.DummyClass2\n"
						+ "{\"name\": \"Homer Simpson\"}\n"
						);
		ObjectStreamReader reader = new ObjectStreamReader(strReader);
		
		Object obj =  reader.readObject();		
		assertTrue("", obj instanceof DummyClass2);
		String expJson = "{\"name\": \"Homer Simpson\"}";
		AssertHelpers.assertEqualsJsonCompare(expJson, obj);
	}
	
	@Test
	public void test__read__CommentsFollowedByBLankLine__MustNotBeDeemdANullObject() throws Exception {
		StringReader strReader = 
				new StringReader(
						  "// Some Comments\n"
						+ "\n"
						+ "class=ca.nrc.json.DummyClass1\n"
						+ "{\"greetings\": \"hello world\"}\n"
						);
		ObjectStreamReader reader = new ObjectStreamReader(strReader);
		
		Object obj =  reader.readObject();
		assertObjectHasClass(obj, DummyClass1.class);
		
		String expJson = "{\"greetings\": \"hello world\"}";
		AssertHelpers.assertEqualsJsonCompare(expJson, obj);
		
		obj =  reader.readObject();		
		assertEquals(null, obj);
	}
	
	
	@Test
	public void test__read__OneObjectPerLine() throws Exception {
		StringReader strReader = 
				new StringReader(
						  "bodyEndMarker=NEW_LINE\n"
						+ "class=ca.nrc.json.DummyClass1\n"
						+ "{\"greetings\": \"hello world\"}\n"
						+ "{\"greetings\": \"yo man!\"}"
						);
		ObjectStreamReader reader = new ObjectStreamReader(strReader);
		
		Object obj =  reader.readObject();
		assertObjectHasClass(obj, DummyClass1.class);
		
		String expJson = "{\"greetings\": \"hello world\"}";
		AssertHelpers.assertEqualsJsonCompare(expJson, obj);
		
		obj =  reader.readObject();	
		expJson = "{\"greetings\": \"yo man!\"}";
		AssertHelpers.assertEqualsJsonCompare(expJson, obj);
	}
	private void assertObjectHasClass(Object obj, Class type) {
		if (obj == null) {
			fail("Object was null. Should have been non-null instance of "+type);
		} else {
			Class gotClass = obj.getClass();
			assertEquals("Read object was of the wrong class", DummyClass1.class, gotClass);
		}
	}

}
