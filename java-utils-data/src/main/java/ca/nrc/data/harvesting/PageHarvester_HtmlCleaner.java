/*
 * Description
 * 		Lightweight class for harvesting web pages. 
 * 		Not appropriate for large scale crawling of web sites.
 * 
 * 		For more details on how to use this class, see the 
 * 		DOCUMENTATION TESTS section of the test case
 * 		PageHarvesterTest.
 */
package ca.nrc.data.harvesting;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.extractors.ArticleExtractor;
import de.l3s.boilerpipe.extractors.KeepEverythingExtractor;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

import ca.nrc.config.ConfigException;
import ca.nrc.data.harvesting.SearchEngine.SearchEngineException;

/**
 * PageHarvester is a wrapper for Htmlcleaner for crawling a page, cleaning a
 * malformated HTMl page, finding the hyperlink in this page, and extracting the
 * page content.
 */

public class PageHarvester_HtmlCleaner extends PageHarvester {
	
	private static final Logger logger = LogManager.getLogger(PageHarvester_HtmlCleaner.class);

	private String bingSearchAPIKey = null;
	private HtmlCleaner cleaner;

	private String html;
	private String wholeText;
	private String mainText;
	private URL currentURL;
	private TagNode currentRoot;
	
	// 0 --> last page harvest went OK
	//       otherwise, equals the error status returned by the server
	public int failureStatus = 0;

	public PageHarvester_HtmlCleaner() {
		super();
		init_PageHarvester_HtmlCleaner((String)null);
	}

	public PageHarvester_HtmlCleaner(String _bingSearchAPIKey) {
		super();
		init_PageHarvester_HtmlCleaner(_bingSearchAPIKey);
	}

	private void init_PageHarvester_HtmlCleaner(String _bingSearchAPIKey) {
		this.bingSearchAPIKey = _bingSearchAPIKey;
		cleaner = makeCleaner();
	}

	private HtmlCleaner makeCleaner() {
		HtmlCleaner theCleaner = new HtmlCleaner();
		final CleanerProperties props = theCleaner.getProperties();
		props.setAllowHtmlInsideAttributes(true);
		props.setAllowMultiWordAttributes(true);
		props.setRecognizeUnicodeChars(true);
		props.setOmitComments(true);

		return theCleaner;
	}

	@Override
	public URL getCurrentURL() {
		return currentURL;
	}
	
	@Override
	public String getTitle() {
		return this.currentTitle;
	}	
	
	@Override
	public String getHtml() {
		return html;
	}

	@Override
	public String getText() {
		return wholeText;
	}

	@Override
	public String getMainText() {
		return mainText;
	}

	public String extractMainText(String html) throws PageHarvesterException {
		
		if (html != null) {
			try {
				this.wholeText = KeepEverythingExtractor.INSTANCE.getText(html);
				mainText = ArticleExtractor.INSTANCE.getText(html);
			} catch (BoilerpipeProcessingException e) {
				throw new PageHarvesterException(e, "Failed to get the main content of the article for url: "+this.currentURL.toString());
			}

			// Make sure the main text includes the page's title
			TagNode titleNode = getDOM().findElementByName("title", true);
			String title = titleNode != null ? cleaner.getInnerHtml(titleNode) : "";
			if (!title.isEmpty() && !this.wholeText.contains(title)) {
				this.wholeText = title + "\n\n" + this.wholeText;
			}			
		}
		
		return wholeText;
	}

	public TagNode getDOM() {
		return currentRoot;
	}

	@Override
	public String crawlPage(URL url) throws IOException {
		TagNode TrainNode = cleaner.clean(url);
		return cleaner.getInnerHtml(TrainNode);
	}

	@Override
	public List<SearchEngine.Hit> crawlHits(SearchEngine.Query query, SearchEngine.IHitVisitor hitVisitor)
			throws IOException, SearchEngineException, PageHarvesterException, ConfigException {
		if (bingSearchAPIKey == null) {
			throw new PageHarvesterException("Crawling hits requires that acquire a Bing Web Search API from Azure, and pass it to the PageHarvester.\nYou can obtain a key from:\n\n   https://www.microsoft.com/en-us/bing/apis/bing-web-search-api");
		}
		SearchEngine engine = new BingSearchEngine(bingSearchAPIKey);
		List<SearchEngine.Hit> hits = engine.search(query).retrievedHits;
		for (SearchEngine.Hit aHit : hits) {
			try {
				hitVisitor.visitHit(aHit);
			} catch (Exception exc) {
				throw new PageHarvesterException(exc);
			}
		}
		return hits;
	}

	
	/**
	 * @param url
	 * @param linkAttrName
	 * @param pageAttrName
	 * @throws IOException
	 * @throws PageHarvesterException 
	 */
	public void harvestHyperLinks(String url, String linkAttrName, final String pageAttrName)
			throws IOException, PageHarvesterException {
		if (linkAttrName == null) {
			linkAttrName = "href";
		}
		this.getPage(url);
		boolean keepGoing = true;
// 2019-06-24: [AD] Not sure why this while() was here, but it was causing an infinite loop today
//    So, I commented it out for now.
//
//		while (keepGoing) {
			final URL pageUrl = new URL(url);
			final TagNode root = cleaner.clean(pageUrl);

			final TagNode main = root.findElementByName("main", true);
			final TagNode body = (main != null) ? main : root.findElementByName("body", true);

			final PageLinkVisitor lv = new PageLinkVisitor(this, pageUrl, linkAttrName, pageAttrName);
			// traverse whole DOM and find hyperlink and next page link
			body.traverse(lv);
//			keepGoing = false;
//		}
	}

	@Override
	protected void loadPage(String url) throws PageHarvesterException {
		Logger tLogger = Logger.getLogger("ca.nrc.data.harvesting.PageHarvester_HtmlCleaner.loadPage");
		tLogger.trace("url="+url);
		try {
			this.currentURL = new URL(url);

			URL urlObj = currentURL;
			String protocol = urlObj.getProtocol();

			if (protocol.equals("file")) {
				this.html = getFilePage(urlObj);
			} else if (protocol.equals("jar")) {
				this.html = getJarPage(urlObj);
			} else if (protocol.equals("http") || protocol.equals("https")) {
				this.html = getHttpPage(urlObj);
			} else {
				throw new IOException("Unsupported protocol: " + protocol + " found in URL " + url);
			}

			if (html == null) {
				throw new PageHarvesterException("Could not download page "+url);
			}

			tLogger.trace("loading html=\n"+html);

			currentRoot = cleaner.clean(html);
			inlineAllIFramesContent(currentRoot);
			this.html = cleaner.getInnerHtml(currentRoot);

			tLogger.trace("parsing specific elments from the html");
			parseCurrentPage();

		} catch (IOException exc) {
			throw new PageHarvesterException(exc, "Failed to get content of url: "+url);
		}

		tLogger.trace("exiting");

		return;
	}

	@Override
	protected String text4elemement(String tagName, boolean failIfMoreThanOne)
		throws PageHarvesterException {
		String eltText = null;
		if (currentRoot != null) {
			TagNode elt = currentRoot.findElementByName(tagName, true);
			if (elt != null) {
				eltText = elt.getText().toString();
			}
		}
		return eltText;
	}

	@Override
	protected void parseCurrentPageText() throws PageHarvesterException {
		this.wholeText = null;
		this.mainText = null;
		if (html != null) {
			try {
				this.wholeText = KeepEverythingExtractor.INSTANCE.getText(html);
				this.mainText = ArticleExtractor.INSTANCE.getText(html);
			} catch (BoilerpipeProcessingException e) {
				throw new PageHarvesterException(e);
			}
		}
		return;
	}

	@Override
	public void harvestSingleLink(String linkText) throws PageHarvesterException {
		linkText = linkText.toLowerCase();
		TagNode[] aElts =
			currentRoot.getElementsByName("a", true);
		List<TagNode> matchingElts = new ArrayList<TagNode>();
		for (TagNode anElt: aElts) {
			String eltAnchor = anElt.getText().toString();
			if (eltAnchor.toLowerCase().equals(linkText)) {
				matchingElts.add(anElt);
			}
		}

		if (matchingElts.size() == 0) {
			throw new PageHarvesterException("No link was found with anchor text '"+linkText+"'");
		}
		if (matchingElts.size() > 1) {
			throw new PageHarvesterException("More than one ("+matchingElts.size()+") link had anchor text '"+linkText+"'");
		}

		String href = matchingElts.get(0).getAttributeByName("href");
		try {
			URL url = new URL(currentURL, href);
			harvestSinglePage(url);
		} catch (MalformedURLException e) {
			throw new PageHarvesterException(e);
		}
		return;
	}

	protected String getHttpPage(URL url) throws PageHarvesterException, IOException {
		Logger tLogger = Logger.getLogger("ca.nrc.data.harvesting.PageHarvester_HtmlCleaner.getHttpPage");
		tLogger.trace("Getting url="+url);
		String oldUserAgent = System.getProperty("http.agent");
		failureStatus  = 0;
		HttpURLConnection conn = null;
		
		try {
			conn = (HttpURLConnection) url.openConnection();
			int timeout = getConnectionTimeoutSecs()*1000; //set timeouts to 5 seconds
			conn.setConnectTimeout(timeout); 
			conn.setReadTimeout(timeout);
			
			int status = conn.getResponseCode();
			tLogger.trace("Got status="+status);
			if (status == HttpURLConnection.HTTP_OK) {
				tLogger.trace("Status was OK");
				return readPage(conn);
			} else if (status == HttpURLConnection.HTTP_MOVED_TEMP
					   || status == HttpURLConnection.HTTP_MOVED_PERM
					   || status == HttpURLConnection.HTTP_SEE_OTHER) {
				tLogger.trace("Status was one of MOVED, MOVED_PERM or SEE_OTHER");
				// normally, 3xx is redirect
				// get redirect url from "location" header field
				String newUrl = conn.getHeaderField("Location");

				// open the new connnection again
				conn = (HttpURLConnection) new URL(newUrl).openConnection();
				status = conn.getResponseCode();
				if (status == HttpURLConnection.HTTP_OK) {
					return readPage(conn);
				} else {
					failureStatus = status;
				}
			} else {
				tLogger.trace("Status was a failure status");
				failureStatus = status;
			}
		} catch (java.net.SocketTimeoutException exc) {
			setError("Connection timed out for url: "+url);
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
			if (oldUserAgent != null) {
				System.setProperty("http.agent", oldUserAgent);
			}
		}
		return null;
	}

	protected String getJarPage(URL url) throws IOException {
		String content = "";
		JarURLConnection conn = (JarURLConnection)(url.openConnection());
		InputStream in = conn.getInputStream();
		BufferedInputStream buffIn = new BufferedInputStream(in);
		byte[] bytesContent = new byte[1024];
		int bytesRead = 0;
		while((bytesRead = buffIn.read(bytesContent)) != -1) { 
		    content += new String(bytesContent, 0, bytesRead, "UTF-8");              
		}	
		return content;
	}	
	
	private String readPage(HttpURLConnection conn) throws IOException {
		Logger tLogger = Logger.getLogger("ca.nrc.data.harvesting.PageHarvester_HtmlCleaner.readPage");
		String result;
		try {
			tLogger.trace("Reading from conn="+conn);
			final StringBuffer htmlBuff = new StringBuffer();
			final BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				htmlBuff.append(inputLine);
			}
			in.close();
			result = htmlBuff.toString();
			tLogger.trace("returning result=\n" + result);
		} catch (Exception e) {
			tLogger.trace("Exception raised: "+e);
			throw e;
		}
		return result;
	}

	private String getFilePage(URL url) throws IOException {
		Logger tLogger = LogManager.getLogger("ca.nrc.data.harvesting.PageHarvester.getFilePage");
		tLogger.trace("** getting url="+url.toString());
		
		BufferedReader br = null;
		StringBuilder htmlBuilder = new StringBuilder();
		String fPath = url.getFile();
		fPath = URLDecoder.decode(fPath, java.nio.charset.StandardCharsets.UTF_8.toString());
		tLogger.trace("** getting fPath="+fPath);

		try {
			String sCurrentLine;
			br = new BufferedReader(new InputStreamReader(
			         new FileInputStream(fPath), "UTF-8"));
			tLogger.trace("** after opening BufferedReader");
			
			while ((sCurrentLine = br.readLine()) != null) {
				htmlBuilder.append(sCurrentLine);
			}
		} finally {
			if (br != null){
				br.close();
			}
		}
		
		String html = htmlBuilder.toString();
		
		return html;
	}

	/*
	 * Replace all <iframe> nodes with the content of the page they are
	 * including.
	 */
	private void inlineAllIFramesContent(TagNode currentRoot)  {
		List<? extends TagNode> iframeNodes = currentRoot.getElementListByName("iframe", true);
		for (TagNode anIframeNode : iframeNodes) {
			try {
				inlineFrameContent(anIframeNode);
			} catch (Exception e) {
				logger.error(String.format("Error to process the iframe src's url <%s> in a webpage: %s.",
						anIframeNode.getAttributeByName("src"), currentURL.getPath()));
			}
		}
	}

	private void inlineFrameContent(TagNode iframeNode) throws IOException {
		/*
		 * Get the BODY of the page that's being displayed inside the iframe.
		 */
		String inclPageRelURL = iframeNode.getAttributeByName("src");
		if (inclPageRelURL != null && inclPageRelURL.equals("about:blank")) {return;}

		// Note: inclPageRelURL == null means that the iframe does not
		// have a src= attribute. Therefore, there is no external
		// content to be inlined
		if (inclPageRelURL != null) {
			HtmlCleaner tempCleaner = makeCleaner();
			URL inclPageAbsURL = new URL(currentURL, inclPageRelURL);
			TagNode root = tempCleaner.clean(inclPageAbsURL);
			TagNode inclBody = root.findElementByName("body", false);

			/*
			 * Replace content of the iframe by the body of the page
			 */
			iframeNode.removeAllChildren();

			for (TagNode anIncludedNode : inclBody.getChildTagList()) {
				iframeNode.addChild(anIncludedNode);
			}

			/* Remove the src attribute of the iframe. */
			iframeNode.removeAttribute("src");
		}
	}

	public String cleanContent(String text) {
		text = StringEscapeUtils.unescapeHtml4(text.toString());
		return text.replaceAll("(?:[\\s]*[\\r\\n]+[\\s]*){2,}", "\n\n");
	}

	public static boolean isHtml(String text) {
		String t = text;
		if (text.length() > 2048) {
			t = text.substring(1, 2048);
		}

		t = t.toLowerCase();
		if (!text.contains("<html"))
			return false;
		if (!text.contains("<head"))
			return false;
		return true;
	}	
}