package ca.nrc.data.bin;

import ca.nrc.data.harvesting.PageHarvester_HtmlCleaner;
import org.junit.Test;

public class HarvestSinglePage {

	public static void main(String[] args) throws Exception {
		String[] urls = new String[] {
			"http://compassrm.com/",
			"http://compassrm.com/about-us/"
		};
		PageHarvester_HtmlCleaner harvester = new PageHarvester_HtmlCleaner();
		for (String url: urls) {
			harvester.harvestSinglePage(url);
			System.out.println(
				"url:"+url+"\n" +
				"MAIN plain text\n"+
				"-------\n"+
				harvester.getMainText()+"\n"+
				"-------"
			);
		}

	}


}
