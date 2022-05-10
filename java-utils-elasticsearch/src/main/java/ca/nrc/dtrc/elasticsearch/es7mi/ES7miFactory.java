package ca.nrc.dtrc.elasticsearch.es7mi;

import ca.nrc.dtrc.elasticsearch.ESFactory;
import ca.nrc.dtrc.elasticsearch.ESUrlBuilder;
import ca.nrc.dtrc.elasticsearch.ElasticSearchException;
import ca.nrc.dtrc.elasticsearch.StreamlinedClient;
import ca.nrc.dtrc.elasticsearch.cluster.ClusterAPI;
import ca.nrc.dtrc.elasticsearch.crud.CrudAPI;
import ca.nrc.dtrc.elasticsearch.search.SearchAPI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ES7miFactory extends ESFactory {

	protected static Map<String,Set<String>> typeIndices4baseName =
		new HashMap<String,Set<String>>();

	public ES7miFactory() throws ElasticSearchException {
		super();
		init__ES7miFactory();
	}

	public ES7miFactory(String _indexName) throws ElasticSearchException {
		super(_indexName);
		init__ES7miFactory();
	}

	private void init__ES7miFactory() {
	}

	@Override
	public int version() {
		return 7;
	}

	@Override
	public StreamlinedClient client() throws ElasticSearchException {
		return new StreamlinedClient_v7mi(this);
	}

	@Override
	public IndexAPI_v7mi indexAPI() throws ElasticSearchException {
		return new IndexAPI_v7mi(this);
	}

	@Override
	public CrudAPI crudAPI() throws ElasticSearchException {
		return new CrudAPI_v7mi(this);
	}

	@Override
	public SearchAPI searchAPI() throws ElasticSearchException {
		return new SearchAPI_v7mi(this);
	}

	@Override
	public ClusterAPI clusterAPI() throws ElasticSearchException {
		return new ClusterAPI_v7mi(this);
	}

	public String index4type(String type) throws ElasticSearchException {
		type = type.toLowerCase();
		String index = this.indexName + "__" + type;
		return index;
	}

	public ESUrlBuilder urlBuilder(String type) throws ElasticSearchException {
		String typeIndex = index4type(type);
		return new ESUrlBuilder(typeIndex, "localhost", port);
	}
}
