package ca.nrc.dtrc.elasticsearch.es5;

import ca.nrc.dtrc.elasticsearch.ESFactory;
import ca.nrc.dtrc.elasticsearch.ElasticSearchException;
import ca.nrc.dtrc.elasticsearch.StreamlinedClient;
import ca.nrc.dtrc.elasticsearch.cluster.ClusterAPI;
import ca.nrc.dtrc.elasticsearch.crud.CrudAPI;
import ca.nrc.dtrc.elasticsearch.index.IndexAPI;
import ca.nrc.dtrc.elasticsearch.search.SearchAPI;

public class ES5Factory extends ESFactory {

	public ES5Factory() throws ElasticSearchException {
		super();
	}

	public ES5Factory(String _indexName) throws ElasticSearchException {
		super(_indexName);
	}

	@Override
	public int version() {
		return 5;
	}

	@Override
	public StreamlinedClient client() throws ElasticSearchException {
		return new StreamlinedClient_v5(this);
	}

	@Override
	public IndexAPI indexAPI() throws ElasticSearchException {
		return new IndexAPI_v5(this);
	}

	@Override
	public CrudAPI crudAPI() throws ElasticSearchException {
		return new CrudAPI_v5(this);
	}

	@Override
	public SearchAPI searchAPI() throws ElasticSearchException {
		return new SearchAPI_v5(this);
	}

	@Override
	public ClusterAPI clusterAPI() throws ElasticSearchException {
		return new ClusterAPI_v5(this);
	}
}
