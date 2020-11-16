package ca.nrc.data.harvesting;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import ca.nrc.config.ConfigException;
import ca.nrc.data.harvesting.SearchEngine.Hit;
import ca.nrc.data.harvesting.SearchEngine.IHitVisitor;
import ca.nrc.data.harvesting.SearchEngine.Query;
import ca.nrc.data.harvesting.SearchEngine.SearchEngineException;
import ca.nrc.ui.web.testing.WebDriverFactory;

public abstract class PageHarvester_WebDriver extends PageHarvester {

	WebDriver _driver = null;

	protected abstract WebDriver makeDriver() throws PageHarvesterException;

	protected WebDriver webDriver() throws PageHarvesterException {
		if (_driver == null) {
			_driver = makeDriver();
		}
		return _driver;
	}
	
	
	@Override
	protected void loadPage(String url) throws PageHarvesterException {
		webDriver().get(url);
	}

	@Override
	public String getHtml() throws PageHarvesterException {
		String html = webDriver().getPageSource();
		return html;
	}

	@Override
	public String getTitle() throws PageHarvesterException {
		String title = webDriver().getTitle();
		return title;
	}

	@Override
	public String getText() throws PageHarvesterException {
		WebElement bodyElt = webDriver().findElement(By.tagName("body"));
		String body = bodyElt.getText();
		
		return body;
	}

	@Override
	public URL getCurrentURL() throws PageHarvesterException {
		String urlStr = webDriver().getCurrentUrl();
		URL url;
		try {
			url = new URL(urlStr);
		} catch (MalformedURLException e) {
			throw new PageHarvesterException(e);
		}
	
		return url;
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
		// TODO Auto-generated method stub
		throw new PageHarvesterException("Method not implemented yet");
	}

	@Override
	protected String text4elemement(String tagName,
		 boolean failIfMoreThanOne) throws PageHarvesterException {
		String text = null;
		List<WebElement> elts = webDriver().findElements(By.name(tagName));
		if (elts != null) {
			if (elts.size() > 1 && failIfMoreThanOne) {
				throw new PageHarvesterException(
					"More than one ("+elts.size()+
					") elements found with tag name '"+tagName+"'");
			}
			if (!elts.isEmpty()) {
				text = elts.get(0).getText();
			}
		}

		return text;
	}


}
