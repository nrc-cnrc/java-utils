package ca.nrc.data.harvesting;

import java.net.URL;

import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class PageHarvester_WebDriverTest extends PageHarvesterTest {
	
	protected  PageHarvester makeHarvesterToTest() {
		PageHarvester harvester = new PageHarvester_WebDriver();
		return harvester;
	}

	
	@Test
	public void test__DELETE_ME() throws Exception {
		PageHarvester_WebDriver harvester = new PageHarvester_WebDriver();
//		URL url = new URL("https://en.wikipedia.org/wiki/Main_Page");
		URL url = new URL("https://en.wikipedia.org/");
		String gotText = harvester.harvestSinglePage(url);
		System.out.println("Text from : "+url+"\n\n"+gotText);
	}
}
