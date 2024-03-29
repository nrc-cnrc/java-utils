package ca.nrc.data.harvesting;

/*
 * This test case is split into three sections
 * 
 * DOCUMENTATION TESTS: 
 *    These tests provide basic examples of use of the class. They are 
 *    optimized for readability, and generally do not check any assertions.
 *    If you want to know how to use this class, just consult that section.
 *    
 * INTERNAL TESTS:
 *    These tests actually check that the internals of the class work
 *    properly. They are harder to read and comprehend, but they do
 *    actually check the behavior of the class using assertions.
 */
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ca.nrc.testing.AssertString;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ca.nrc.data.harvesting.SearchEngine.Hit;
import ca.nrc.file.ResourceGetter;
import de.l3s.boilerpipe.BoilerpipeProcessingException;
import org.junit.jupiter.api.Assertions;

public abstract class PageHarvesterTest {

	private PageHarvester harvester = null;
	private String bingTestKey;

	@Before
	public void setUp() throws Exception {
		this.bingTestKey = SearchEngine_BingTest.assumeTestBingKeyIsDefined();
		harvester = makeHarvesterToTest();
	}
	
	protected abstract PageHarvester makeHarvesterToTest();
	
	/*******************************************************************************
	 * DOCUMENTATION TESTS
	 ********************************************************************************/


	@SuppressWarnings("unused")
	@Test
	public void test__PageHarvester__Synopsis() throws Exception {
		// First, you create a harvester
		PageHarvester harvester = makeHarvesterToTest();
	
		//
		// You can then use the harvester to get the content of a single page
		// Note: For the purposes of this test, we use a local file:// url, so
		// that the test does not depend on external urls.
		//
		String urlHelloWorld = ResourceGetter.getResourceFileUrl("local_html_files/hello_world.html").toString();
		harvester.harvestSinglePage(urlHelloWorld);
		
		// Get the HTML content
		String html = harvester.getHtml();
		
		// Get the plain-text content of the page.
		//
		// You can get the plain text of the COMPLETE web page, including
		// things like banners, menus, advertisements, etc...
		//
		String plainText_whole = harvester.getText();

		// 
		// Alternatively, you can get the plain text for the "main" part of the
		// page, i.e. excluding things like banners, menus, advertisements, etc..
		//
		String plainText_main = harvester.getMainText();
				
		// Get the title of the page
		String title = harvester.getTitle();
		
		//
		// You can also use a harvester to visit all the hits returned by 
		// a search engine query.
		//
		// This can only be done if you have provided a Bing Web Search API
		// key to the harvester. This test assumes that the key is provided
		// in a config prop file.
		//
		harvester = new PageHarvester_HtmlCleaner(bingTestKey);

		//
		// First, you need to define a class of IHitVisitor. For the purpose
		// of this test, the visitor only collects the hit urls.
		//
		final class TestVisitor implements SearchEngine.IHitVisitor {
			public List<URL> hitURLs = new ArrayList<URL>();
			@Override
			public void visitHit(Hit hit) {
				hitURLs.add(hit.url);
			}
		}
		TestVisitor visitor = new TestVisitor();

		//
		// Then you invoke the crawlHits method, passing it the
		// visitor and a SearchEngine.Query.
		//
		// The visitor will be invoked on each of the hits found.
		//
		harvester.crawlHits(new SearchEngine.Query("donald trump"), visitor);

		// You can ask the harvester to harvest a link that has a particular
		// anchor text
		//
		harvester.harvestSinglePage(urlHelloWorld);
		harvester.harvestSingleLink("Google Link");
	}
		
	/*******************************************************************************
	 * VERIFICATION TESTS
	 ********************************************************************************/	
		
	@Test
	public void test__harvestSinglePage__HappyPath() throws IOException, BoilerpipeProcessingException, PageHarvesterException {
		String url = "http://en.wikipedia.org/";
		harvester.harvestSinglePage(url);
		
		String expTitleHead = "Wikipedia, the free encyclopedia";
		String expTitleText = "Wikipedia, the free encyclopedia";

		String html = harvester.getHtml();
		AssertString.assertStringContains(html, "<title>"+expTitleHead+"</title>");
		
		// Plain-text should not contain any HTML code
		String plainText = harvester.getText();
		AssertString.assertStringDoesNotContain(plainText, "<title>");
		AssertString.assertStringContains(plainText, expTitleText);
		
		// Main text should NOT contain HTML codes, nor side bars
		String mainText = harvester.getMainText();
		Assertions.assertTrue(mainText != null && !mainText.isEmpty());
		AssertString.assertStringDoesNotContain(mainText, "<title>");
		AssertString.assertStringDoesNotContain(mainText, "About Wikipedia");

		String gotTitle = harvester.getTitle();
		AssertString.assertStringEquals(expTitleText, gotTitle);
	}

	@Test(expected = PageHarvesterException.class)
	public void test__harvestSinglePage__PageThatDoesNotExist__raisesPageHarvesterException() throws IOException, BoilerpipeProcessingException, PageHarvesterException {
		String url = "http://www.fg3q45qfaret.23445rtwert/pageOnServerThatDoesNotExist.html";
		harvester.harvestSinglePage(url);
	}
	
	@Test
	public void testeIFrameParserError() throws Exception {
		// Note: For the purposes of this test, we use a local file:// url, so
		// that the test does not depend on external urls.
		String existingUrl = ResourceGetter.getResourceFileUrl("local_html_files/iframe-eorror.html").toString();
		harvester.harvestSinglePage(existingUrl);

		String gotHtml = harvester.getHtml();
		TagNode node = new HtmlCleaner().clean(gotHtml);
		assertTrue(node.findElementByName("body", true) != null);
	}

	@Test
	public void test__harvestSingleLink__HappyPath() throws Exception {
		String urlHelloWorld = ResourceGetter.getResourceFileUrl("local_html_files/hello_world.html").toString();
		harvester.harvestSinglePage(urlHelloWorld);
		String linkText = "Google Link";
		harvester.harvestSingleLink(linkText);
		new HarvesterAsserter(harvester)
			.currentPageTitleContains("Google")
			;
	}

	@Test @Ignore
	public void testHyperLink() throws Exception {
		Assert.fail("Ignore this failure for now. There is a very weird bug in PageLinkVisitor.visit()\n"+
	         "(see comments in that method).\n" +
			 "In any case, we don't use harvestHyperlinks in a while so the bug is not high prioirty.");
		
		// Note: For the purposes of this test, we use a local file:// url, so
		// that the test does not depend on external urls.
		String existingUrl = ResourceGetter.getResourceFileUrl("local_html_files/hello_world.html").toString();
		harvester.harvestSinglePage(existingUrl);

		String gotHtml = harvester.getHtml();
		assertTrue("Html code was not as expected.", gotHtml.matches("[\\s\\S]*<title>\\s*Hello\\s+World[\\s\\S]*"));

		String gotText = harvester.getText();
		assertTrue("Plain text content was not as expected.", gotText.matches("\\s*Hello World[\\s\\S]+"));

		IPageVisitorCollector urlCollector = new IPageVisitorCollector();
		harvester.attachVisitor(urlCollector);

		String url = ResourceGetter.getResourceFileUrl("local_html_files/hello_world.html").toURI().toURL().toString();
		String nextPageAttrName = "href";
		String nextPageAttrValue = "Next";

		harvester.harvestHyperLinks(url, nextPageAttrName, nextPageAttrValue);
		List<String> expected = Arrays.asList("http://www.nrc-cnrc.gc.ca/index.html");
		List<String> re = urlCollector.getVistedURL();
		int index = 0;
		for (String next : re) {
			String href = expected.get(index++);
			assertEquals("Error for href " + href, href, next);
		}
	}

	@Test(expected = ca.nrc.data.harvesting.PageHarvesterException.class)
	public void testNonExistantURL() throws Exception {
		String nonexistantUrl = "http://asdfadsfewr.asdfawerqre.com";
		harvester.harvestSinglePage(nonexistantUrl);
	}
	
	@Test
	public void test__PageWithAnIframeThatIncludesExteralPage__ShouldIncludeContentOfTheIFramedPage() throws IOException, PageHarvesterException {
		
		//
		// This is a page that has an iframe with a src= attribute that refers
		// to a second page.
		// 
		// We expect the harvester to insert the content of the second page
		// inside the frame of the first page.
		//
		String url = ResourceGetter.getResourceFileUrl("local_html_files/short_pages/page_with_iframe_inclusion.html").toString();
		
		harvester.harvestSinglePage(url);
		String gotContent = harvester.getText();
		String expSubstring = "Content included through an iframe";
		AssertString.assertStringContains(gotContent, expSubstring);
	}

	@Test
	public void test__PageWithAnAlreadyInlinedIframe() throws IOException, PageHarvesterException {
		//
		// This is page with an iframe, but the iframe does NOT have an src=
		// attribute.
		//
		// In other words, the content of the iframe is already inlined in the 
		// page.
		//
		String url = ResourceGetter.getResourceFileUrl("local_html_files/short_pages/page_with_iframe_already_inlined.html").toString();
		harvester.harvestSinglePage(url);
		String gotContent = harvester.getText();
		String expSubstring = "This content was already inlined in the iframe of the page";
		AssertString.assertStringContains(gotContent, expSubstring);
	}

	@After
	public void tearDown() throws Exception {
	}

	class IPageVisitorCollector implements IPageVisitor {

		List<String> vistedURL = new ArrayList<String>();

		@Override
		public void visitPage(String url, String htmlContent, String mainText) {
			vistedURL.add(url);
			// TODO Should actually collect the URL into a list attribute
		}

		public List<String> getVistedURL() {
			return vistedURL;
		}
	}
}
