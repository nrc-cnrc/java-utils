package ca.nrc.data.harvesting;

import java.io.BufferedReader;
import java.io.IOException;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import ca.nrc.datastructure.Pair;

public class BingSearchEngine extends SearchEngine {
	
	private static final String bingSearchUrl = "https://api.cognitive.microsoft.com/bing/v7.0/search";

	public Map<Object, Object> parameters = new HashMap<Object, Object>();

	// Bing Web Search API key obtained from Microsof Azure:
	//
	//    https://www.microsoft.com/en-us/bing/apis/bing-web-search-api
	//
	private String bingKey;

	public BingSearchEngine() throws SearchEngineException {
		init_BingSearchEngine((String)null);
	}

	public BingSearchEngine(String _bingKey) throws SearchEngineException {
		super();
		init_BingSearchEngine(_bingKey);
	}

	private void init_BingSearchEngine(String _bingKey) throws SearchEngineException {
		this.bingKey = _bingKey;
		parameters.put("subscription-key", _bingKey);
		parameters.put("count",  10);
		parameters.put("offset", 0);
		parameters.put("mkt", "en-us");
		parameters.put("setLang", "en");
		parameters.put("safesearch", "Moderate");
		parameters.put("Accept", "application/json");

		return;
	}
	
	@Override
	protected SearchResults searchRaw(Query seQuery) throws SearchEngineException {
		Logger tLogger = Logger.getLogger("ca.nrc.data.harvesting.BingSearchEngine.search");
		
		String queryStr = seQuery.fuzzyQuery;
		
		final List<SearchEngine.Hit> hitList = new ArrayList<SearchEngine.Hit>();
		final CloseableHttpClient httpClient = HttpClients.createDefault();
		
		Pair<Integer,Integer> pageRange = seQuery.computeFirstAndLastPage();
		Integer currentPage = pageRange.getFirst();
		Integer lastPage = pageRange.getSecond();
		
		String queryString = makeBingQueryString(seQuery);
		
		Long totalEstHits = new Long(0);
		
		boolean keepGoing = true;
		while (keepGoing) {
			if (currentPage > lastPage) break;
			int offset = currentPage * seQuery.hitsPerPage;
			parameters.put("q",
					String.format("%s -filetype:pdf -filetype:ppt -filetype:doc -filetype:img -filetype:bmp -filetype:png -filetype:jpg -filetype:gif -filetype:zip -filetype:jar -filetype:mp3 -filetype:avi", queryString));
			parameters.put("count", seQuery.hitsPerPage);
			parameters.put("setLang", seQuery.lang);
			parameters.put("offset", offset);
			
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
				try {
					httpResponse = httpClient.execute(httpGet);
				} catch (IOException e) {
					throw new SearchEngineException("Could not GET bing url: "+fullURL, e);
				}
				ok = httpResponse.getStatusLine().getStatusCode();
				if (ok == 429) {
					// Too many requests...
					// Sleep for a second and try again
					try {
						Thread.sleep(1*1000);
					} catch (InterruptedException e) {
						// This should never happen
						e.printStackTrace();
					}
				} else {
					break;
				}
			}
			if (ok != HttpURLConnection.HTTP_OK) {
				throw new SearchEngineException("Could not fetch results from Bing. Is your ca_nrc_javautils_bingkey property set correctly?");
			} else {
				BufferedReader reader;
				StringBuffer response = null;
				try {
					reader = new BufferedReader(
							new InputStreamReader(httpResponse.getEntity().getContent()));
					response = new StringBuffer();
					String inputLine;
					while ((inputLine = reader.readLine()) != null) {
						response.append(inputLine);
					}
					reader.close();
				} catch (UnsupportedOperationException | IOException e) {
					throw new SearchEngineException("Unable to read response from bing url: "+fullURL, e);
				}
					
				final JSONObject json = new JSONObject(response.toString());
				if (!json.has("webPages")) {
					keepGoing = false;
				} else {
					final JSONObject page = json.getJSONObject("webPages");
					totalEstHits = page.getLong("totalEstimatedMatches");
					final JSONArray jsonResults = page.getJSONArray("value");
					final int resultsLength = jsonResults.length();
					if (resultsLength == 0) {
						keepGoing = false;
					} else {
						for (int i = 0; i < resultsLength; i++) {
							final JSONObject aResult = jsonResults.getJSONObject(i);						
							String directURL = getHitDirectURL(aResult.getString("url"));
							URL hitURL = null;
							try {
								hitURL = new URL(directURL);
							} catch (MalformedURLException e) {
								throw new SearchEngineException("Bing hit was not a valid URL: "+hitURL, e);
							}
							
							SearchEngine.Hit newHit = 
									new SearchEngine.Hit(hitURL, aResult.getString("name"),
											aResult.getString("snippet"));
							
							newHit.outOfTotal = totalEstHits;										
							hitList.add(newHit);
							if (seQuery.maxHits != null && hitList.size() == seQuery.maxHits) break;
						}
					}
				}
			}
			
			if (seQuery.maxHits != null && hitList.size() >= seQuery.maxHits) {
				keepGoing = false;
			} else {
				currentPage++;
			}			
		}	
		
		try {
			httpClient.close();
		} catch (IOException exc) {
			throw new SearchEngineException(exc);
		}
		
		tLogger.trace("Upon exit, for queryStr="+queryStr+", results.size()="+hitList.size());
		
		SearchResults results = new SearchResults();
		results.retrievedHits = hitList;
		results.estTotalHits = totalEstHits;
		
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
		
		queryString = possiblyAddLanguageHelper(queryString, seQuery.lang);
		
		return queryString;
	}

	/**
	 * For some languages like Inuktut, Bing needs a bit of "help" in order
	 * to focus on pages in that language. This method adds some terms to 
	 * the query string in order to provide such "help".
	 */
	private String possiblyAddLanguageHelper(String queryString, String lang) {
		if (lang.equals("iu")) {
			queryString += " AND -(\"the\")";
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

	protected String getHitDirectURL(String bingHitURL) throws SearchEngineException {
		String directURL = bingHitURL;
		URI bingURI;
		try {
			bingURI = new URI(bingHitURL);
		} catch (URISyntaxException e) {
			throw new SearchEngineException("Bing Hit was not a URL: "+bingHitURL, e);
		}
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
	 * @throws SearchEngineException 
	 * @throws UnsupportedEncodingException
	 */
	protected String addQueryStringToUrl(String url, final Map<Object, Object> parameters) throws SearchEngineException {
		if (parameters == null || parameters.isEmpty()) {
			return url;
		}

		for (Map.Entry<Object, Object> parameter : parameters.entrySet()) {
			String parameterKey = parameter.getKey().toString();
			String encodedKey;
			try {
				encodedKey = URLEncoder.encode(parameterKey, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new SearchEngineException("Could not encode search parameter to UTF8: "+parameterKey, e);
			}

			String parameterValue = parameter.getValue().toString();
			String encodedValue;
			try {
				encodedValue = URLEncoder.encode(parameterValue, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new SearchEngineException("Could not encode value of search parameter '"+parameterKey+"' to UTF8 (value was: '"+parameterValue+"').");
			}
			
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
