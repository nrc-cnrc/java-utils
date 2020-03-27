package ca.nrc.data.harvesting;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import ca.nrc.config.ConfigException;
import ca.nrc.data.harvesting.SearchEngine.Hit;
import ca.nrc.data.harvesting.SearchEngine.IHitVisitor;
import ca.nrc.data.harvesting.SearchEngine.Query;
import ca.nrc.data.harvesting.SearchEngine.SearchEngineException;

public class PageHarvester_HtmlUnit extends PageHarvester_JSEnabled {
	
	/** HtmlUnit client used to download and process pages */
	WebClient webClient = new WebClient();
	
	/** Plain text content of last downloaded page */
	private String text;

	/** HTML of the last downloaded page */
	private String html;

	private URL currentURL;

	public  void clickOn(String eltRegexp) {
		
	}

	@Override
	protected void getPage(String url) throws PageHarvesterException {
	    try {
			final HtmlPage page = webClient.getPage(url);
			List<DomElement> bodies = page.getElementsByTagName("body");
			if (bodies.size() == 0) {
				throw new PageHarvesterException(
						"Could not find <body> tag of page "+url);
			}
			DomElement body = bodies.get(0);
			html = body.asText();
			text = body.getTextContent();
			currentURL = page.getUrl();
			
		} catch (FailingHttpStatusCodeException | IOException e) {
			throw new PageHarvesterException("", e);
		}
	}

	@Override
	public String getHtml() {
		return html;
	}

	@Override
	public String getTitle() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getText() {
		return text;
	}

	@Override
	public URL getCurrentURL() {
		return currentURL;
	}

	@Override
	public void harvestHyperLinks(String url, String linkAttrName, String pageAttrName)
			throws IOException, PageHarvesterException {
		throw new PageHarvesterException("Method not implemented yet");
	}

	@Override
	public String crawlPage(URL url) throws IOException {
		throw new IOException("Method not implemented yet");
	}

	@Override
	public List<Hit> crawlHits(Query query, IHitVisitor hitVisitor)
			throws IOException, SearchEngineException, PageHarvesterException, ConfigException {
		throw new PageHarvesterException("Method not implemented yet");
	}
	
}
