package ca.nrc.data.harvesting;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.nrc.config.ConfigException;
import ca.nrc.data.harvesting.SearchEngine.Hit;
import ca.nrc.data.harvesting.SearchEngine.IHitVisitor;
import ca.nrc.data.harvesting.SearchEngine.Query;
import ca.nrc.data.harvesting.SearchEngine.SearchEngineException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class PageHarvester {

	/** When downloading a page, try this many times before giving up */
	public int maxTries = 3;
	public PageHarvester setMaxTries(int _maxTries) {
		maxTries = _maxTries;
		return this;
	}

	protected IPageVisitor visitor;
	private String error;
	protected String currentTitle;
	protected static Map<String,DownloadActivity> downloadActivities =
		new HashMap<String,DownloadActivity>();

	protected boolean harvestFullText = false;
		public void setHarvestFullText(boolean _fullText) {
			this.harvestFullText = _fullText;
		}

	/** Downloads the page into the harvester */
	protected abstract void loadPage(String url) throws PageHarvesterException;

	/** Parse the HTML of the current page to identif the plain text
	 * of the the COMPLETE page as well as its MAIN part (i.e. the
	 * part that excludes things like banners, menus, advertisements, etc.)
	 */
	protected abstract void parseCurrentPageText() throws PageHarvesterException;


	/** Get Html of last downloaded page 
	 * @throws PageHarvesterException */
	public abstract String getHtml() throws PageHarvesterException;

	protected abstract String text4elemement(
		String tagName, boolean failIfMoreThanOne) throws PageHarvesterException;
	
	/** Get Title of last downloaded page 
	 * @throws PageHarvesterException */
	public abstract String getTitle() throws PageHarvesterException;
	
	/** Get COMPLETE plain text of last downloaded page
	 * @throws PageHarvesterException */
	public abstract String getText() throws PageHarvesterException;

	/** Get MAIN plain text of last downloaded page.
	 *  This excludes things like banners, menus, advertisements, etc.
	 * @throws PageHarvesterException */
	public abstract String getMainText() throws PageHarvesterException;

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

	public abstract void harvestSingleLink(String linkText) throws PageHarvesterException;

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
			visitor.visitPage(url, getHtml(), getMainText());
		}
	}

	protected void getPage(String urlStr) throws PageHarvesterException {
		Logger tLogger = LogManager.getLogger("ca.nrc.data.harvesting.PageHarvester.getPage");
		URL url = null;
		try {
			url = new URL(urlStr);
		} catch (MalformedURLException e) {
			throw new PageHarvesterException(e);
		}
		int totalTries = 0;
		while (true) {
			totalTries++;
			tLogger.trace("getting urlStr="+urlStr+" for the "+totalTries+"nth time");
			try {
				bePoliteWithHost(url);
				Long startMSecs = System.currentTimeMillis();
				loadPage(urlStr);
				Long endMSecs = System.currentTimeMillis();
				logDownload(urlStr, startMSecs, endMSecs);
			} catch (DownloadActivityException e) {
				if (totalTries < maxTries) {
					tLogger.trace("Exception raised "+e+". Trying again");
					continue;
				} else {
					tLogger.trace("Could not download the page after "+totalTries+" tries");
					throw new PageHarvesterException(e);
				}
			}
			parseCurrentPage();
			tLogger.trace("URL successfully downloaded");
			break;
		}
	}

	/**
	 * This method will sleep for an appropriate amount of time in order to
	 * not harass a particular host with too many requests in too short a time.
	 */
	private void bePoliteWithHost(URL url) throws PageHarvesterException {
		DownloadActivity lastDownload = downloadActivities.get(url.getHost());
		if (lastDownload != null) {
			Long nowMSecs = System.currentTimeMillis();
			Long elapsedSinceLastDownload = nowMSecs - lastDownload.endedAtMsecs;

			// The sleep time is calculated in such a way that if the last download
			// took N msecs, then we won't bother that host until N msecs later.
			//
			Long msecsToSleep =
				lastDownload.lastedMsecs - elapsedSinceLastDownload;

			if (msecsToSleep > 0) {
				try {
					Thread.sleep(msecsToSleep);
				} catch (InterruptedException e) {
					throw new PageHarvesterException(e);
				}
			}
		}
	}

	private void logDownload(String urlStr, Long startMSecs, Long endMSecs) throws DownloadActivityException {
		DownloadActivity activity =
			new DownloadActivity(urlStr, startMSecs, endMSecs);
		downloadActivities.put(activity.host, activity);
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
}
