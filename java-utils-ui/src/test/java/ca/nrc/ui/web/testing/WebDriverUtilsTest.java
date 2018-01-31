package ca.nrc.ui.web.testing;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;

import com.fasterxml.jackson.databind.node.ArrayNode;

import ca.nrc.file.ResourceGetter;
import ca.nrc.testing.AssertHelpers;


public class WebDriverUtilsTest {

	private static WebDriver theDriver;
	private WebDriverUtils wdUtils;
	
	@BeforeClass 
	public static void setUpBeforeClass() {	
		theDriver = WebDriverFactory.getDriver();
	}
	
	@Before
	public void setUp() throws Exception {
		int resetWaitMSecs = 3*1000;
		wdUtils = new WebDriverUtils(WebDriverUtilsTest.theDriver, resetWaitMSecs);
	}

	@After
	public void tearDown() throws Exception {
	}
	
	
	/*****************************************************************************
	 * DOCUMENTATION TESTS
	 *****************************************************************************/

	@Test
	public void test__ThisIsHowYouCreateA_WebDriverUtils_Instance() {
		
		//
		// Some methods in WebDriverUtils may need to temporarily
		// set the driver's implicit wait time to a very short time.
		// Strangely enough, the WebDriver API does not include
		// a method for reading the value of the implicit wait, so the
		// method has no way of knowing what value it was set to
		// originally.
		//
		// For that reason, you need to tell the WebDriverUtil at what
		// level you wnat the implicit wait to be reset.
		//
		int resetWaitInMSecs = 10*1000;
		
		// You can feed the WebDriver at construciton time
		WebDriverUtils wdUtils = new WebDriverUtils(theDriver, resetWaitInMSecs);
		
		// Or you can feed it later
		wdUtils = new WebDriverUtils(resetWaitInMSecs);
		wdUtils.setDriver(theDriver);
	
	}

	@Test  @SuppressWarnings("unused")
	public void test__ThisIsHowYouExtractTextOfATable() throws IOException {
		loadURLIntoDriver("small_table.html");
		
		//
		// You can extract the text content of a table as  two-dimensional
		// ArrayList of Strings
		//
		By tableLocator = By.id("smallTable");
		ArrayList<ArrayList<String>> tableText = wdUtils.extractTableTextAsArray(tableLocator);
		
		// 
		// You can also extract a table that resides inside a given WebElement
		//
		WebElement withinElement = wdUtils.driver.findElement(By.id("tableDiv"));
		tableText = wdUtils.extractTableTextAsArray(tableLocator, withinElement);
		
		//
		// Or, you can extract the table text as an ArrayNode object
		//
		ArrayNode tableTextAsJson = wdUtils.extractTableTextAsJson(tableLocator);
		// Or...
		tableTextAsJson = wdUtils.extractTableTextAsJson(tableLocator, withinElement);

	}

	@Test
	public void test__YouCanCheckIfAnElementExists() throws IOException {
		//
		// Oddly enough, WebDriver does not have a method for 
		// doing that. Of course, you can always use findElement() and 
		// check if the return value is null. But findElement() will wait for 
		// the default timeout period (or whaterver timeout you have
		// set for the driver), before it concludes that the element does
		// not exist.
		//
		// Our method elementExists() returns true or false immediatly
		// without waiting.
		//

		loadURLIntoDriver("small_table.html");

		@SuppressWarnings("unused")
		boolean exists = wdUtils.elementExists(By.id("nonexistant_id"));
	}
	
	/*****************************************************************************
	 * VERIFICATION  TESTS
	 *****************************************************************************/

	@Test
	public void test__extractTableTextAsArray__FromPage() throws IOException {
		
		loadURLIntoDriver("small_table.html");
		ArrayList<ArrayList<String>> gotTableText = wdUtils.extractTableTextAsArray(By.id("tableDiv"));

		String expTableText =
				"["+
				"  [\"Name\", \"Age\"],"+
				"  [\"Alain\", \"51\"],"+
				"  [\"Mathieu\", \"23\"]"+
				"]"+
				"";
		ca.nrc.testing.AssertHelpers.assertEqualsJsonCompare(expTableText, gotTableText);
	}

	@Test
	public void test__extractTableTextAsArray__FirstTableFromRootOfPage() throws IOException {
		
		loadURLIntoDriver("small_table.html");
		WebElement withinElement = wdUtils.driver.findElement(By.id("tableDiv"));
		ArrayList<ArrayList<String>> gotTableText = wdUtils.extractTableTextAsArray(By.id("smallTable"), withinElement);

		String expTableText =
				"["+
				"  [\"Name\", \"Age\"],"+
				"  [\"Alain\", \"51\"],"+
				"  [\"Mathieu\", \"23\"]"+
				"]"+
				"";
		AssertHelpers.assertEqualsJsonCompare(expTableText, gotTableText);
	}
		
	@Test
	public void test__extractTableTextAsJson__FromPage() throws IOException {
		
		loadURLIntoDriver("small_table.html");
		ArrayNode gotTableText = wdUtils.extractTableTextAsJson(By.id("tableDiv"));

		String expTableText =
				"["+
				"  [\"Name\", \"Age\"],"+
				"  [\"Alain\", \"51\"],"+
				"  [\"Mathieu\", \"23\"]"+
				"]"+
				"";
		AssertHelpers.assertEqualsJsonCompare(expTableText, gotTableText);
	}

	@Test
	public void test__extractTableInnerHTMLAsArray() throws IOException{
		
		loadURLIntoDriver("table_inner_html.html");
		ArrayList<String> gotRows = wdUtils.extractTableInnerHTMLAsArray(By.id("tableTest"));
		
		List<String> expRows = new ArrayList<>();
		expRows.add("<td>one</td>");
		expRows.add("<td><b>two</b> three</td>");
		
		AssertHelpers.assertUnOrderedSameElements("Inner HTML results not as expected", expRows, gotRows);
	}
	
	@Test
	public void test__extractTableTextAsJson__FromAGivenElement() throws IOException {
		
		loadURLIntoDriver("small_table.html");
		WebElement withinElement = wdUtils.driver.findElement(By.id("tableDiv"));

		ArrayNode gotTableText = wdUtils.extractTableTextAsJson(By.id("smallTable"), withinElement);

		String expTableText =
				"["+
				"  [\"Name\", \"Age\"],"+
				"  [\"Alain\", \"51\"],"+
				"  [\"Mathieu\", \"23\"]"+
				"]"+
				"";
		AssertHelpers.assertEqualsJsonCompare(expTableText, gotTableText);
	}
	
	@Test
	public void test__elementExists__ElementDoesNotExist__ReturnsFalse() throws IOException {
		loadURLIntoDriver("small_table.html");
		boolean exists = wdUtils.elementExists(By.id("nonexistant_id"));
		assertFalse(exists);
	}
	
	@Test
	public void test__elementExists__ElementDoesExist__ReturnsTrue() throws IOException {
		loadURLIntoDriver("small_table.html");
		boolean exists = wdUtils.elementExists(By.id("tableDiv"));
		assertTrue(exists);
	}

	

	/*****************************************************************************
	 * HELPER METHODS
	 *****************************************************************************/
	
	public void loadURLIntoDriver(String pathFromRoot) throws IOException {
		URL url = ResourceGetter.getResourceFileUrl("local_html_files/"+pathFromRoot);
		wdUtils.driver.get(url.toString());		
	}
}
