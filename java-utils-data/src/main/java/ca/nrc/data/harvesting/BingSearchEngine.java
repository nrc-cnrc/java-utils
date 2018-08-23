package ca.nrc.data.harvesting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.experimental.max.MaxHistory;


public class BingSearchEngine extends SearchEngine {
	
	private static final String BingPropertyFile = "/ca/nrc/conf/bing.properties";
	private static final String bingSearchUrl = "https://api.cognitive.microsoft.com/bing/v7.0/search";

	final Map<Object, Object> parameters = new HashMap<Object, Object>();

	public BingSearchEngine() throws IOException, SearchEngineException {
		init(BingPropertyFile);
	}

	public BingSearchEngine(String propFile) throws IOException, SearchEngineException {
		init(propFile);
	}
	
	private void init(String propFile) throws IOException, SearchEngineException{
		final InputStream in = BingSearchEngine.class.getResourceAsStream(propFile);
		final Properties propsFile = new Properties();
		try {
			propsFile.load(in);
		} catch (Exception exc) {
			throw new SearchEngine.SearchEngineException("Could not find Bing properties file: "+propFile, exc);
		}
		in.close();
		final Enumeration<Object> keys = propsFile.keys();
		while (keys.hasMoreElements()) {
			Object next = keys.nextElement();
			parameters.put(next, propsFile.get(next));
		}
	}

	@Override
	public List<SearchEngine.Hit> search(SearchEngine.Query seQuery) throws SearchEngineException {
		Logger tLogger = LogManager.getLogger("ca.nrc.data.harvesting.BingSearchEngine.search");
		
		final List<SearchEngine.Hit> results = new ArrayList<SearchEngine.Hit>();
		final CloseableHttpClient httpClient = HttpClients.createDefault();
		
		String queryString = makeBingQueryString(seQuery);
		
		boolean keepGoing = true;
		int hitsPageNum = 0;
		while (keepGoing) {
			try {
				parameters.put("q",
						String.format("%s -filetype:pdf -filetype:ppt -filetype:doc -filetype:img -filetype:bmp -filetype:png -filetype:jpg -filetype:gif -filetype:zip -filetype:jar -filetype:mp3 -filetype:avi", queryString));
				if (seQuery.maxHits > 0) {
					parameters.put("count", seQuery.maxHits);
				}
				parameters.put("offset", hitsPageNum);
				
				String url = bingSearchUrl;
				String fullURL = addQueryStringToUrl(url, parameters);
							
				tLogger.trace("Getting hits from Bing fullURL="+fullURL);
				HttpGet httpGet = new HttpGet(fullURL);
		
				String subscrKey = parameters.get("subscription-key").toString();
				httpGet.addHeader("Ocp-Apim-Subscription-Key", subscrKey);
				httpGet.addHeader("Accept", parameters.get("Accept").toString());
		
	
				int  MAX_TRIES = 2;
				int ok = 0;
				CloseableHttpResponse httpResponse = null;
				for (int ii=0; ii < MAX_TRIES; ii++) {
					httpResponse = httpClient.execute(httpGet);
					ok = httpResponse.getStatusLine().getStatusCode();
					if (ok == 429) {
						// Too many requests...
						// Sleep for a second and try again
						Thread.sleep(1*1000);
					} else {
						break;
					}
				}
				if (ok == HttpURLConnection.HTTP_OK) {
					final BufferedReader reader = new BufferedReader(
							new InputStreamReader(httpResponse.getEntity().getContent()));
					final StringBuffer response = new StringBuffer();
					String inputLine;
					while ((inputLine = reader.readLine()) != null) {
						response.append(inputLine);
					}
					reader.close();
					final JSONObject json = new JSONObject(response.toString());
					if (!json.has("webPages")) {
						keepGoing = false;
					} else {
						final JSONObject page = json.getJSONObject("webPages");
						final JSONArray jsonResults = page.getJSONArray("value");
						final int resultsLength = jsonResults.length();
						if (resultsLength == 0) {
							keepGoing = false;
						} else {
							tLogger.trace("resultsLength="+resultsLength);
							for (int i = 0; i < resultsLength; i++) {
								final JSONObject aResult = jsonResults.getJSONObject(i);						
								String directURL = getHitDirectURL(aResult.getString("url"));
								URL hitURL = new URL(directURL);
								
								results.add(new SearchEngine.Hit(hitURL, aResult.getString("name"),
									aResult.getString("snippet")));
								if (results.size() == seQuery.maxHits) break;
							}
						}
					}
				}
			} catch (Exception exc) {
				throw new SearchEngineException(exc);
			}
			
			if (results.size() >= seQuery.maxHits) {
				keepGoing = false;
			} else {
				hitsPageNum++;
			}			

		}	
		
		try {
			httpClient.close();
		} catch (IOException exc) {
			throw new SearchEngineException(exc);
		}
		
		return results;
	}

	protected String makeBingQueryString(Query seQuery) {
		String queryString = null;
		if (seQuery.terms != null) {
			queryString = makeBingQueryString_TermsList(seQuery);
		} else {
			queryString = makeBingQueryString_FuzzyQueryString(seQuery);
		}
	
		SearchEngine.Type[] types = seQuery.types;
		if (!Arrays.asList(types).contains(SearchEngine.Type.ANY)) {
			// We need to somehow restrict the search to certain types of
			// pages (ex: "news", "blogs").
			// 
			// Unfortunately, Bing API does not allow to do this explicitly.
			//
			// Instead, we add some keywords to the original query string.
			
			String additionalTerms = " AND +(";
			boolean isFirst = true;
			for (Type aType: types) {
				if (!isFirst) {additionalTerms = additionalTerms + " OR ";}
				additionalTerms = additionalTerms + "\"";				
				if (aType == Type.BLOG) {
					additionalTerms = additionalTerms + "blog";
				} else if (aType == Type.NEWS) {
					additionalTerms = additionalTerms + "news";
				}
				additionalTerms = additionalTerms + "\"";				
				isFirst = false;
			}
			additionalTerms = additionalTerms + ")";
			queryString = queryString + additionalTerms;
			
		}
		
		if (seQuery.getSite() != null) {
			queryString = "+site:"+seQuery.getSite()+" "+queryString;
		}
		
		return queryString;
	}

	private String makeBingQueryString_FuzzyQueryString(Query seQuery) {
		String queryString = seQuery.fuzzyQuery;
		return queryString;
	}

	private String makeBingQueryString_TermsList(Query seQuery) {
		String queryString = null;
		if (seQuery.terms.size() > 0) {
			/*
			 * Ideally, we would specify a query that puts all terms between
			 * double quotes, but Bing seems to return empty hits as soon
			 * as we have 3 or more such phrases.
			 */
//			queryString = "(\"";
//			queryString += StringUtils.join(seQuery.terms, "\" \"");
//			queryString += "\")";
			
			/*
			 * So instead, we just specify the list of terms as a fuzzy query
			 * where all terms are separated by commas.
			 */
			queryString = StringUtils.join(seQuery.terms, ", ");
		}
		
		return queryString;
	}

	protected String getHitDirectURL(String bingHitURL) throws MalformedURLException, URISyntaxException {
		String directURL = bingHitURL;
		URI bingURI = new URI(bingHitURL);
		List<NameValuePair> params = URLEncodedUtils.parse(bingURI, "UTF-8");
		for (NameValuePair param : params) {
		  if (param.getName().equals("r")) {
			  directURL = param.getValue();
			  break;
		  }
		}
				
		return directURL;
	}

	/**
	 * Add Query String To the Url
	 * 
	 * @param url
	 * @param parameters
	 * @return string full url
	 * @throws UnsupportedEncodingException
	 */
	protected String addQueryStringToUrl(String url, final Map<Object, Object> parameters)
			throws UnsupportedEncodingException {
		if (parameters == null || parameters.isEmpty()) {
			return url;
		}

		for (Map.Entry<Object, Object> parameter : parameters.entrySet()) {
			final String encodedKey = URLEncoder.encode(parameter.getKey().toString(), "UTF-8");
			String encodedValue = URLEncoder.encode(parameter.getValue().toString(), "UTF-8");
			
			// Note: For some reason, encode() changes '+' to '%2B' (entity for space).
			//  eventhoughb '+' is a valid URL character.
			//
			//  So, undo that.
			//
			encodedValue = encodedValue.replaceAll("%2B", "+");
			if (!url.contains("?")) {
				url += "?" + encodedKey + "=" + encodedValue;
			} else {
				url += "&" + encodedKey + "=" + encodedValue;
			}
		}
		return url;
	}
}
