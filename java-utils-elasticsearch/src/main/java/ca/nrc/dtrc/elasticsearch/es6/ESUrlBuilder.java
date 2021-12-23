package ca.nrc.dtrc.elasticsearch.es6;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class ESUrlBuilder {
	private String indexName = null;
	private String serverName = null;
	private Integer port = 9206;
	
	private String endPoint = null;

	/** If true, then do not to include '_doc' keyword in the URL */
	private Boolean _noDocKeyword = null;

	private String cat = null;
	
	private String docType = null;

	private Document doc = null;
	
	private String docID = null;
	
	private boolean scrolling = false;

	private boolean waitForRefresh = false;

	/** If true:
	 *    URL has form http://endpoint/...
	 *  otherwise the endpoint will come later in the URL
	 *
	 *  By default set to false
	 */
	private boolean epShouldComesFirst = false;

	public ESUrlBuilder(String _indexName, String _serverName, Integer _port) {
		this.indexName = _indexName;
		this.serverName = _serverName;
		if (_port != null) {
			this.port = _port;
		}
	}
	
	private String serverWithPortPart() {
		String _url = "http://"+serverName+":"+port+"/";
		return _url;
	}
	
	public ESUrlBuilder forDocument(Document doc) throws ElasticSearchException{
		doc.ensureNonNulType();
		this.doc = doc;
		return this;
	}
	
	public ESUrlBuilder forClass(Class<? extends Document> _docClass) {
		this.docType = _docClass.getName();
		return this;
	}

	public ESUrlBuilder epComesFirs() {
		epShouldComesFirst = true;
		return this;
	}

	public ESUrlBuilder forDocType(String _docType) {
		this.docType = _docType;
		return this;
	}
	
	public ESUrlBuilder forDocID(String _docID) {
		this.docID = _docID;
		if (this.docID != null) {
			docID = docID.replaceAll("%", "%B6");
			docID = docID.replaceAll("/", "%2F");
		}
		return this;
	}
	
	public ESUrlBuilder forEndPoint(String _endPoint) {
		this.endPoint = _endPoint;
		if (_endPoint.equals("_mapping")) {
			_noDocKeyword = false;
		}
		return this;
	}

	public ESUrlBuilder refresh(boolean _wait) {
		this.waitForRefresh = _wait;
		return this;
	}

	public ESUrlBuilder scroll() {
		return scroll(null);
	}
	
	
	public ESUrlBuilder scroll(String _scrollID) {
		this.scrolling = true;
		return this;
	}

	public URL build() throws ElasticSearchException {
		String serverAndPort = serverWithPortPart();
		String doc = docPart();
		String id = docIDPart();
		String index = indexPart();
		String epoint = endpointPart();

		String _urlStr = serverAndPort;

		_urlStr += index;
		if (epShouldComesFirst) {
			_urlStr += epoint;
		}
		_urlStr += doc;
		_urlStr += id;
		if (!epShouldComesFirst) {
			_urlStr += epoint;
		};

		if (cat != null) {
			_urlStr += "_cat/"+cat;
		}

		_urlStr = addURLArguments(_urlStr);

		_urlStr = fixSlashes(_urlStr);

		URL url = null;
		try {
			url = new URL(_urlStr);
		} catch (MalformedURLException exc) {
			throw new ElasticSearchException("Malformed ElasticSearch URL "+url, exc);
		}


		return url;			
	}

	private String fixSlashes(String urlStr) {
		urlStr = urlStr.replaceAll("(?<!http:)//", "/");

		// If the last part of the URL is either a docID or
		// endpoint, make sure it's not followed by a slash
		urlStr = urlStr.replaceAll("\\{(ep|id)\\}/$", "");

		// Make sure to remove slash before the URL arguments
		//
		urlStr = urlStr.replaceAll("/\\?", "?");

		// URL cannot end with _doc/
		urlStr = urlStr.replaceAll("_doc/$", "_doc");

		// Remove all the markers that indicate what type of
		// component lies in different parts of the URL
		urlStr = urlStr.replaceAll("\\{(ep|id)\\}", "");

		return urlStr;
	}

	private String endpointPart() {
		String endPointStr = "";
		if (endPoint != null) {
			endPointStr = endPoint.toString() + "{ep}/";
		}
		return endPointStr;
	}

	private String indexPart() {
		String ind = "";
		if (includeIndexName()) {
			ind = indexName+"/";
		}
		return ind;
	}

	private String docIDPart() throws ElasticSearchException {
		String rawID = docID;
		if (rawID == null && doc != null) {
			rawID = doc.getIdWithoutType();
		}
		String type = docType;
		if (type == null && doc != null) {
			type = doc.type;
		}
		String idPart = null;
		if (type != null && rawID != null) {
			idPart = Document.docID(type, rawID);
		}
		if (idPart == null) {
			idPart = "";
		} else {
			idPart = idPart + "{id}/";
		}

		return idPart;
	}

	private String docPart() {
		String _doc = "";
		if (shouldIncludeDocInURL()) {
			_doc = "_doc/";
		}

		return _doc;
	}


	private boolean includeIndexName() {
		Boolean answer = null;

		if (indexName == null) {
			answer = false;
		}

		if (answer == null && cat != null) {
			answer = false;
		}

		if (answer == null && indexName != null) {
			if (endPoint != null &&
				endPoint.equals("_search/scroll")) {
				answer = false;
			} else {
				answer = true;
			}
		}

		if (answer == null) {
			answer = true;
		}

		return answer;
	}

	private boolean shouldIncludeDocInURL() {
		Boolean answer = null;
		if (_noDocKeyword != null) {
			answer = !_noDocKeyword;
		}
		if (answer == null && cat != null) {
			answer = false;
		}
		if (answer == null) {
			if (endPoint == null) {
				answer = true;
			} else if (endPoint.matches("(_update|_mapping)")) {
				answer = true;
			}
		}
		if (answer == null) {
			answer = false;
		}
		return answer;
	}

	private String addURLArguments(String urlStr) {
		Map<String,String> args = new HashMap<String,String>();
		if (scrolling) {
			args.put("scroll", "1m");
		}
		if (waitForRefresh) {
			args.put("refresh", "wait_for");
		}
		if (cat != null) {
			args.put("format", "json");
		}

		boolean addQuestionMark = true;
		int argCount = 0;
		for (Map.Entry<String, String> entry : args.entrySet()) {
			argCount++;
			String name = entry.getKey();
			String val = entry.getValue();
			if (addQuestionMark) {
				urlStr += "?";
				addQuestionMark = false;
			}
			if (argCount > 1) {
				urlStr += "&";
			}
			urlStr += name;
			if (val != null) {
				urlStr += "="+val;
			}
		}

		return urlStr;
	}

	public ESUrlBuilder noDocKeyword() {
		this._noDocKeyword = true;
		return this;
	}

	public ESUrlBuilder cat(String what) {
		this.cat = what;
		return this;
	}
}
