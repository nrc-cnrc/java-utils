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
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
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

import ca.nrc.data.harvesting.SearchEngine.SearchEngineException;

import ca.nrc.data.harvesting.SearchEngine.SearchEngineException;

/**
 * PageHarvester is a wrapper for Htmlcleaner for crawling a page, cleaning a
 * malformated HTMl page, finding the hyperlink in this page, and extracting the
 * page content.
 */

public class PageHarvester {
	
	private static final Logger logger = LogManager.getLogger(PageHarvester.class);

	private HtmlCleaner cleaner;

	private String html;
	private String text;
	private String title;
	private URL currentURL;
	private IPageVisitor visitor;
	private TagNode currentRoot;
	public String error = null;
	
	private int connectionTimeoutSecs = 5;
	public int getConnectionTimeoutSecs() {
		return connectionTimeoutSecs;
	}
	public PageHarvester setConnectionTimeoutSecs(int connectionTimeoutSecs) {
		this.connectionTimeoutSecs = connectionTimeoutSecs;
		return this;
	}
	

	// 0 --> last page harvest went OK
	//       otherwise, equals the error status returned by the server
	public int failureStatus = 0;

	public PageHarvester() {
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

	/**
	 * Crawl a web page, clean mal-formated tags and extractor main content
	 * 
	 * @param url
	 * @throws IOException
	 */

	public String getHtml() {
		return html;
	}

	public String getText() {
		return text;
	}

	public String getMainText() throws PageHarvesterException {
		String html = getHtml();
		String text = null;
		
		if (html != null) {
			String mainText;
			try {
				
				text = KeepEverythingExtractor.INSTANCE.getText(html);
				mainText = ArticleExtractor.INSTANCE.getText(html);
			} catch (BoilerpipeProcessingException e) {
				throw new PageHarvesterException(e, "Failed to get the main content of the article for url: "+this.currentURL.toString());
			}

			if (mainText != null && !mainText.isEmpty()) {
				text = mainText;
			}

			// Make sure the main text includes the page's title
			TagNode titleNode = getDOM().findElementByName("title", true);
			String title = titleNode != null ? cleaner.getInnerHtml(titleNode) : "";
			if (!title.isEmpty() && !text.contains(title)) {
				text = title + "\n\n" + text;
			}			
		}
		
		return text;
	}

	public TagNode getDOM() {
		return currentRoot;
	}

	public String crawlPage(URL url) throws IOException {
		TagNode TrainNode = cleaner.clean(url);
		return cleaner.getInnerHtml(TrainNode);
	}

	public List<SearchEngine.Hit> crawlHits(SearchEngine.Query query, SearchEngine.IHitVisitor hitVisitor)
			throws IOException, SearchEngineException, PageHarvesterException {
		SearchEngine engine = new BingSearchEngine();
		List<SearchEngine.Hit> hits = engine.search(query);
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
	 * Set PageVisitor class
	 * 
	 * @param visitor
	 */
	public void attachVisitor(IPageVisitor visitor) {
		this.visitor = visitor;
	}

	/**
	 * Crawl a web page
	 * 
	 * @param url
	 * @throws IOException
	 */
	public String harvestSinglePage(URL url) throws PageHarvesterException {
		harvestSinglePage(url.toString());
		String content = this.getHtml();
		return content;
	}
	
	public void harvestSinglePage(String url) throws PageHarvesterException {
		error = null;
		getPage(url);
		if (visitor != null) {
			visitor.visitPage(url, html, text);
		}
	}

	/**
	 * @param url
	 * @param linkAttrName
	 * @param pageAttrName
	 * @throws IOException
	 * @throws PageHarvesterException 
	 */
	public void harvestHyperLinks(String url, final String linkAttrName, final String pageAttrName)
			throws IOException, PageHarvesterException {
		this.getPage(url);
		boolean keepGoing = true;
		while (keepGoing) {
			final URL pageUrl = new URL(url);
			final TagNode root = cleaner.clean(pageUrl);

			final TagNode main = root.findElementByName("main", true);
			final TagNode body = (main != null) ? main : root.findElementByName("body", true);

			final PageLinkVisitor lv = new PageLinkVisitor(this, pageUrl, linkAttrName, pageAttrName);
			// traverse whole DOM and find hyperlink and next page link
			body.traverse(lv);
			keepGoing = false;
		}
	}

	private void getPage(String url) throws PageHarvesterException {
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
			
			parseHTML(html);

		} catch (IOException exc) {
			throw new PageHarvesterException(exc, "Failed to get content of url: "+url);
		}
		
	}

	protected void parseHTML(String html) throws PageHarvesterException {
		if (null == html) return;
		
		currentRoot = cleaner.clean(html.toString());
		title = null;
		
		inlineAllIFramesContent(currentRoot);

		this.html = cleaner.getInnerHtml(currentRoot);
		this.text = getMainText();
		
		TagNode elt = currentRoot.findElementByName("title", true);
		if (elt != null) {
			title = elt.getText().toString();
		}
		
		// If no <TITLE> element, take the first Hn element of the highest 
		// level present
		if (title == null) {
			for (String hLevel: new String[] {"h1", "h2", "h3", "h4"}) {
				elt = currentRoot.findElementByName(hLevel, true);
				if (elt != null) {
					title = elt.getText().toString();
					break;
				}
			}
		}	
	}

	protected String getHttpPage(URL url) throws PageHarvesterException, IOException {
		String oldUserAgent = System.getProperty("http.agent");
		failureStatus  = 0;
		HttpURLConnection conn = null;
		
		try {
			conn = (HttpURLConnection) url.openConnection();
			int timeout = getConnectionTimeoutSecs()*1000; //set timeouts to 5 seconds
			conn.setConnectTimeout(timeout); 
			conn.setReadTimeout(timeout);
			
			int status = conn.getResponseCode();
			if (status == HttpURLConnection.HTTP_OK) {
				return readPage(conn);
			} else if (status == HttpURLConnection.HTTP_MOVED_TEMP
					   || status == HttpURLConnection.HTTP_MOVED_PERM
					   || status == HttpURLConnection.HTTP_SEE_OTHER) {
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
				failureStatus = status;
			}
		} catch (java.net.SocketTimeoutException exc) {
			error = "Connection timed out for url: "+url;
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
		final StringBuffer htmlBuff = new StringBuffer();
		final BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
		String inputLine;
		while ((inputLine = in.readLine()) != null) {
			htmlBuff.append(inputLine);
		}
		in.close();
		return htmlBuff.toString();
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

	public String getTitle() {
		return this.title;
	}

}
