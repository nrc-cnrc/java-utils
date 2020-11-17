package ca.nrc.data.harvesting;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import ca.nrc.config.ConfigException;
import ca.nrc.data.harvesting.SearchEngine.Hit;
import ca.nrc.data.harvesting.SearchEngine.IHitVisitor;
import ca.nrc.data.harvesting.SearchEngine.Query;
import ca.nrc.data.harvesting.SearchEngine.SearchEngineException;

public abstract class PageHarvester {

	protected IPageVisitor visitor;
	private String error;
	protected String currentTitle;

	protected boolean harvestFullText = false;
		protected void setHarvestFullText(boolean _fullText) {
			this.harvestFullText = _fullText;
		}

	/** Downloads the page into the harvester */
	protected abstract void loadPage(String url) throws PageHarvesterException;

	/** Get Html of last downloaded page 
	 * @throws PageHarvesterException */
	public abstract String getHtml() throws PageHarvesterException;

	protected abstract String text4elemement(
		String tagName, boolean failIfMoreThanOne) throws PageHarvesterException;
	
	/** Get Title of last downloaded page 
	 * @throws PageHarvesterException */
	public abstract String getTitle() throws PageHarvesterException;
	
	/** Get plain text of last downloaded page 
	 * @throws PageHarvesterException */
	public abstract String getText() throws PageHarvesterException;
	
	/** Get ACTUAL URL of last downloaded page.
	 *  This may be different from the URL that was provided to 
	 *  the harvester (for example, if the original page contained an 
	 *  auto-forward) 
	 * @throws PageHarvesterException */
	public abstract URL getCurrentURL() throws PageHarvesterException;
	
	/** Follow all hyperlinks contained on a page, and harvest each of those
	 *  pages. */
	public abstract void harvestHyperLinks(String url, final String linkAttrName, 
			final String pageAttrName)
			throws IOException, PageHarvesterException;
	
	public abstract String crawlPage(URL url) throws IOException;
	public abstract List<Hit> crawlHits(Query query, IHitVisitor hitVisitor) 
			throws IOException, SearchEngineException, PageHarvesterException, 
					ConfigException;
	
	private int connectionTimeoutSecs = 5;
	public int getConnectionTimeoutSecs() {
		return connectionTimeoutSecs;
	}
	
	public PageHarvester setConnectionTimeoutSecs(int connectionTimeoutSecs) {
		this.connectionTimeoutSecs = connectionTimeoutSecs;
		return this;
	}


	/** Set Error of of last page download */	
	public void setError(String _err) {
		this.error = _err;
	}
	
	/** Getet Error of of last page download */	
	public String getError() {
		return error;
	}
	
	/**
	 * Set PageVisitor class
	 * 
	 * @param visitor
	 */
	public void attachVisitor(IPageVisitor visitor) {
		this.visitor = visitor;
	}
	
	/**
	 * Download a single web page
	 * 
	 * @param url
	 * @throws IOException
	 */
	public String harvestSinglePage(URL url) throws PageHarvesterException {
		harvestSinglePage(url.toString());
		String content = this.getHtml();
		return content;
	}
	
	/**
	 * Download a single web page
	 * 
	 * @param url
	 * @throws IOException
	 */
	public void harvestSinglePage(String url) throws PageHarvesterException {
		setError(null);
		getPage(url);
		if (visitor != null) {
			visitor.visitPage(url, getHtml(), getText());
		}
	}

	protected void getPage(String url) throws PageHarvesterException {
		loadPage(url);
		parseCurrentPage();
	}

	protected void parseCurrentPage() throws PageHarvesterException {
		parseCurrentPageTitle();
		parseCurrentPageText();
	}

	protected void parseCurrentPageTitle() throws PageHarvesterException {
		String html = getHtml();
		boolean failIfMoreThanOne = true;
		currentTitle = text4elemement("title", failIfMoreThanOne);

		// If no <TITLE> element, take the first Hn element of the highest
		// level present
		if (currentTitle == null) {
			for (String hLevel : new String[]{"h1", "h2", "h3", "h4"}) {
				currentTitle = text4elemement(hLevel, false);
				if (currentTitle != null) {
					break;
				}
			}
		}

		return;
	}


	protected void parseCurrentPageText() throws PageHarvesterException {

	}

	public abstract void harvestSingleLink(String linkText) throws PageHarvesterException;
}
