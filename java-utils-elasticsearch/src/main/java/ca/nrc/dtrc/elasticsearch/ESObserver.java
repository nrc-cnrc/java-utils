package ca.nrc.dtrc.elasticsearch;


import ca.nrc.datastructure.Cloner;

import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class ESObserver {

	protected ESFactory esFactory = null;

	public void setObservedIndex(String _indexName) {
 		this.esFactory.indexName = _indexName;
	}

	protected abstract void onBulkIndex(
		int fromLine, int toLine, String indexName, String docTypeName)
		throws ElasticSearchException;

	protected abstract void beforePUT(URL url, String json)
		throws ElasticSearchException;

	protected abstract void afterPUT(URL url, String json)
		throws ElasticSearchException;

	protected abstract void beforePOST(URL url, String json)
		throws ElasticSearchException;

	protected abstract void afterPOST(URL url, String json)
		throws ElasticSearchException;

	protected abstract void beforeGET(URL url)
		throws ElasticSearchException;

	protected abstract void afterGET(URL url)
		throws ElasticSearchException;

	protected abstract void beforeDELETE(URL url, String json)
		throws ElasticSearchException;

	protected abstract void afterDELETE(URL url, String json)
		throws ElasticSearchException;

	public abstract void observeBeforeHEAD(URL url, String json)
		throws ElasticSearchException;

	public abstract void observeAfterHEAD(URL url, String json)
		throws ElasticSearchException;

	private Set<String> typesToObserve = null;

	public ESObserver(ESFactory _esFactory) throws ElasticSearchException {
		init_ESObserver(_esFactory, new String[0]);
	}

	public ESObserver(ESFactory _esFactory, String... types) throws ElasticSearchException {
		init_ESObserver(_esFactory, types);
	}

	private void init_ESObserver(ESFactory _esFactory, String[] types) throws ElasticSearchException {
		try {
			// Clone the factory to avoid circular references
			esFactory = Cloner.clone(_esFactory);
		} catch (Cloner.ClonerException e) {
			throw new ElasticSearchException(e);
		}
		if (types.length > 0) {
			typesToObserve = new HashSet<String>();
			Collections.addAll(typesToObserve, types);
		}
	}

	public void observeBulkIndex(int fromLine, int toLine, String indexName, String docTypeName) throws ElasticSearchException {
		if (shouldObserveType(docTypeName)) {
			onBulkIndex(fromLine, toLine, indexName, docTypeName);
		}
	}

	public void observeBeforePUT(URL url, String json) throws ElasticSearchException {
		if (shouldObserveURL(url)) {
			beforePUT(url, json);
		}
	}

	public void observeAfterPUT(URL url, String json) throws ElasticSearchException {
		if (shouldObserveURL(url)) {
			afterPUT(url, json);
		}
	}

	public void observeBeforePOST(URL url, String json) throws ElasticSearchException {
		if (shouldObserveURL(url)) {
			beforePOST(url, json);
		}
	}

	public void observeAfterPOST(URL url, String json) throws ElasticSearchException {
		if (shouldObserveURL(url)) {
			afterPOST(url, json);
		}
	}

	public void observeBeforeGET(URL url) throws ElasticSearchException {
		if (shouldObserveURL(url)) {
			beforeGET(url);
		}
	}

	public void observeAfterGET(URL url) throws ElasticSearchException {
		if (shouldObserveURL(url)) {
			afterGET(url);
		}
	}

	public void observeBeforeDELETE(URL url, String json) throws ElasticSearchException {
		if (shouldObserveURL(url)) {
			beforeDELETE(url, json);
		}
	}

	public void observeAfterDELETE(URL url, String json) throws ElasticSearchException {
		if (shouldObserveURL(url)) {
			afterDELETE(url, json);
		}
	}

	protected boolean shouldObserveType(String type) {
		boolean shouldObserve = true;
		if (typesToObserve != null) {
			shouldObserve = (typesToObserve.contains(type));
		}
		return shouldObserve;
	}

	private boolean shouldObserveURL(URL url) {
		boolean shouldObserve = true;
		if (typesToObserve != null) {
			shouldObserve = false;
			String type = type4URL(url);
			shouldObserve = shouldObserveType(type);
		}

		return shouldObserve;
	}

	protected String type4URL(URL url) {
		String path = url.getPath();
		String[] pathParts = path.split("/");
		String type = null;
		if (pathParts.length > 2) {
			type = pathParts[2];
		}
		if (type.equals("_doc")) {
			type = null;
			String lastPart = pathParts[pathParts.length-1];
			Matcher matcher = Pattern.compile("^(.*):").matcher(lastPart);
			if (matcher.find()) {
				type = matcher.group(1);
			}
		}
		return type;
	}
}
