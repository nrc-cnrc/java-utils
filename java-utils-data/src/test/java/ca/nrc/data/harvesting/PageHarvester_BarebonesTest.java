package ca.nrc.data.harvesting;

import org.junit.Test;

import ca.nrc.testing.AssertHelpers;
import ca.nrc.testing.AssertString;

public class PageHarvester_BarebonesTest extends PageHarvesterTest {

	@Override
	protected PageHarvester makeHarvesterToTest() {
		return new PageHarvester_Barebones();
	}
	
	///////////////////////////////////
	// VERIFICATION TESTS
	//////////////////////////////////
	
	@Test
	public void test__parseHtml__PageWithTitleElement__UsesTheTitleElementForTitle() throws PageHarvesterException {
		String html = 
				"<html><head><title>Hello world</title></head>\n"+
				"<body><h1>This should NOT be the title</h1></body></html>";		
		PageHarvester_Barebones harvester = new PageHarvester_Barebones();
		
		harvester.parseHTML(html);
		String gotTitle = harvester.getTitle();
		String expTitle = "Hello world";
		AssertString.assertStringEquals(gotTitle, expTitle);		
	}
	
	@Test
	public void test__parseHtml__PageWithNoTitleEltAndAnH1Elt__UsesTheH1EltForTitle() throws PageHarvesterException {
		String html = 
				"<html><body><h1>Hello world</h1></body></html>";
		PageHarvester_Barebones harvester = new PageHarvester_Barebones();
		
		harvester.parseHTML(html);
		String gotTitle = harvester.getTitle();
		String expTitle = "Hello world";
		AssertString.assertStringEquals(gotTitle, expTitle);		
	}

	@Test
	public void test__parseHtml__PageWithNoTitleNorH1EltButAH2Elt__UsesTheH2EltForTitle() throws PageHarvesterException {
		String html = 
				"<html><body><h2>Hello world</h2></body></html>";
		PageHarvester_Barebones harvester = new PageHarvester_Barebones();
		
		harvester.parseHTML(html);
		String gotTitle = harvester.getTitle();
		String expTitle = "Hello world";
		AssertString.assertStringEquals(gotTitle, expTitle);		
	}

	@Test
	public void test__parseHtml__PageWhoseTitleIsInH1ButH2EltPrecedesIt__UsesTheH1EltForTitle() throws PageHarvesterException {
		String html = 
				"<html><body><h2>not the title</h2><h1>Hello world</h1></body></html>";
		PageHarvester_Barebones harvester = new PageHarvester_Barebones();
		
		harvester.parseHTML(html);
		String gotTitle = harvester.getTitle();
		String expTitle = "Hello world";
		AssertString.assertStringEquals(gotTitle, expTitle);		
	}

	@Test
	public void test__parseHtml__PageWithNothingThatLooksLikeATitle__ReturnsNull() throws PageHarvesterException {
		String html = 
				"<html>Hello world</html>";
		PageHarvester_Barebones harvester = new PageHarvester_Barebones();
		
		harvester.parseHTML(html);
		String gotTitle = harvester.getTitle();
		String expTitle = null;
		AssertString.assertStringEquals(gotTitle, expTitle);		
	}
}
