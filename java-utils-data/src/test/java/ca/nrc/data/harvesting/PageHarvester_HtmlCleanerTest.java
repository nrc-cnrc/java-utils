package ca.nrc.data.harvesting;

import org.junit.Test;

import ca.nrc.testing.AssertString;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;

public class PageHarvester_HtmlCleanerTest extends PageHarvesterTest {

	@Override
	protected PageHarvester makeHarvesterToTest() {
		return new PageHarvester_HtmlCleaner();
	}
	
	///////////////////////////////////
	// VERIFICATION TESTS
	//////////////////////////////////
	
	@Test
	public void test__parseCurrentPage__PageWithTitleElement__UsesTheTitleElementForTitle()
		throws Exception {
		String html = 
				"<html><head><title>Hello world</title></head>\n"+
				"<body><h1>This should NOT be the title</h1></body></html>";
		URL url = createTempURLWithHtml(html);
		PageHarvester_HtmlCleaner harvester = new PageHarvester_HtmlCleaner();
		harvester.loadPage(url.toString());
		harvester.parseCurrentPage();
		String gotTitle = harvester.getTitle();
		String expTitle = "Hello world";
		AssertString.assertStringEquals(gotTitle, expTitle);		
	}

	private URL createTempURLWithHtml(String html) throws Exception {
		File tempFile = File.createTempFile("temp-url", ".html");
		Files.write(tempFile.toPath(), html.getBytes());

		return tempFile.toURI().toURL();
	}

	@Test
	public void test__parseCurrentPage__PageWithNoTitleEltAndAnH1Elt__UsesTheH1EltForTitle()
		throws Exception {
		String html = "<html><body><h1>Hello world</h1></body></html>";
		URL url = createTempURLWithHtml(html);
		PageHarvester_HtmlCleaner harvester = new PageHarvester_HtmlCleaner();

		harvester.loadPage(url.toString());
		harvester.parseCurrentPage();
		String gotTitle = harvester.getTitle();
		String expTitle = "Hello world";
		AssertString.assertStringEquals(gotTitle, expTitle);		
	}

	@Test
	public void test__parseCurrentPage__PageWithNoTitleNorH1EltButAH2Elt__UsesTheH2EltForTitle()
		throws Exception {
		String html = 
				"<html><body><h2>Hello world</h2></body></html>";
		PageHarvester_HtmlCleaner harvester = new PageHarvester_HtmlCleaner();

		URL url = createTempURLWithHtml(html);
		harvester.loadPage(url.toString());
		harvester.parseCurrentPage();
		String gotTitle = harvester.getTitle();
		String expTitle = "Hello world";
		AssertString.assertStringEquals(gotTitle, expTitle);		
	}

	@Test
	public void test__parseCurrentPage__PageWhoseTitleIsInH1ButH2EltPrecedesIt__UsesTheH1EltForTitle()
		throws Exception {
		String html = 
				"<html><body><h2>not the title</h2><h1>Hello world</h1></body></html>";
		PageHarvester_HtmlCleaner harvester = new PageHarvester_HtmlCleaner();

		URL url = createTempURLWithHtml(html);
		harvester.loadPage(url.toString());

		harvester.parseCurrentPage();
		String gotTitle = harvester.getTitle();
		String expTitle = "Hello world";
		AssertString.assertStringEquals(gotTitle, expTitle);		
	}

	@Test
	public void test__parseCurrentPage__PageWithNothingThatLooksLikeATitle__ReturnsNull()
		throws Exception {
		String html = 
				"<html>Hello world</html>";
		PageHarvester_HtmlCleaner harvester = new PageHarvester_HtmlCleaner();

		URL url = createTempURLWithHtml(html);
		harvester.loadPage(url.toString());
		harvester.parseCurrentPage();
		String gotTitle = harvester.getTitle();
		String expTitle = null;
		AssertString.assertStringEquals(gotTitle, expTitle);		
	}	
}
