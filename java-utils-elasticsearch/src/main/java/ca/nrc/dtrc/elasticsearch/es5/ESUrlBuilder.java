package ca.nrc.dtrc.elasticsearch.es5;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class ESUrlBuilder {
	private String indexName = null;
	private String serverName = null;
	private Integer port = null;
	
	private String endPoint = null;
	
	private Boolean endPointBeforeType = null;
	
	private String docType = null;
	
	private String docID = null;
	
	private boolean scrolling = false;

	private boolean waitForRefresh = false;
	
	public ESUrlBuilder(String _serverName, int _port) {
		this.serverName = _serverName;
		this.port = _port;
	}

	public ESUrlBuilder(String _indexName, String _serverName, int _port) {
		this.indexName = _indexName;
		this.serverName = _serverName;
		this.port = _port;
	}
	
	private String baseUrl() {
		String _url = "http://"+serverName+":"+port+"/";
		return _url;
	}
	
	public ESUrlBuilder forDocument(Document doc) {
		this.docID = doc.getId();
		this.docType = doc.getClass().getName();
		return this;
	}
	
	public ESUrlBuilder forClass(Class<? extends Document> _docClass) {
		this.docType = _docClass.getName();
		return this;
	}

	public ESUrlBuilder forDocType(String _className) {
		this.docType = _className;
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

	public ESUrlBuilder endPointBeforeType(boolean flag) {
		this.endPointBeforeType = flag;
		return this;
	}
	
	public URL build() throws ElasticSearchException {
		String endPointStr = "";
		if (endPoint != null) {
			endPointStr = "/"+endPoint.toString();
		}

		Boolean epBeforeType = endPointBeforeType;
		if (epBeforeType == null) epBeforeType = false;
		
		String type = docType;
		if (type == null || type.equals("")) {
			type = "";
		} else {
			type = "/"+type;
		}
		
		String id = docID;
		if (id == null) {
			id = "";
		} else {
			id = "/"+id;
		}
		
		String _urlStr = baseUrl();
		if (indexName != null && endPoint != "_search/scroll") _urlStr += indexName;
		
		if (epBeforeType) {
			_urlStr += endPointStr + type;
		} else {
			_urlStr += type + id + endPointStr;
		}

		_urlStr = addURLArguments(_urlStr);

		_urlStr = _urlStr.replaceAll("(?<!http:)//", "/");
				
		URL url = null;
		try {
			url = new URL(_urlStr);
		} catch (MalformedURLException exc) {
			throw new ElasticSearchException("Malformed ElasticSearch URL "+url, exc);
		}
		
		return url;			
	}

	private String addURLArguments(String urlStr) {
		Map<String,String> args = new HashMap<String,String>();
		if (scrolling) {
			args.put("scroll", "1m");
		}
		if (waitForRefresh) {
			args.put("refresh", "wait_for");
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
}
