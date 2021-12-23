package ca.nrc.dtrc.elasticsearch;

import ca.nrc.dtrc.elasticsearch.cluster.ClusterAPI;
import ca.nrc.dtrc.elasticsearch.crud.CrudAPI;
import ca.nrc.dtrc.elasticsearch.index.IndexAPI;
import ca.nrc.dtrc.elasticsearch.search.SearchAPI;
import org.junit.jupiter.api.Test;

public abstract class ESFactoryTest {

	protected abstract ESFactory makeESFactory(String indexName) throws ElasticSearchException;

	protected ESFactory makeESFactory() throws ElasticSearchException {
		return makeESFactory("test-index");
	}

	@Test
	public void test__ES__Synopsis() throws Exception {

		// ESFactory is the class you use to interact with ElasticSearch
		// The class is abstract, and you must instantiate a concrete
		// subclass that is designed to interact with a specific version
		// of ESFactory
		ESFactory esFactory = makeESFactory();

		// By default, the server is on:
		//
		//   Server : localhost
		//   Port   : 9205
		//
		// But you can change those like this:
		//
		esFactory
			.setServer("www.somewhere.com")
			.setPort(9400);


		// You can then use different 'components' of the ESFactory class to
		// carry out different types of ESFactory requests.
		//

		// Use this component to create, configure and delete indices.
		// See ESIndexTest for details.
		IndexAPI index = esFactory.indexAPI();

		// Use this component to create/read/update/delete a document
		// See CRUDTest for details;
		CrudAPI crud = esFactory.crudAPI();

		// Use this component to search for documents
		// See ESSearchTest for details;
		SearchAPI search = esFactory.searchAPI();


		// Use this component to cluster documents
		// See ESClusterTest for details;
		ClusterAPI cluster = esFactory.clusterAPI();

		// There is also a deprecated, somewhat bloated class that allows
		// you to carry out the operations of any of the above APIs
		// See StreamlinedClientTest for details.
		StreamlinedClient esClient = esFactory.client();
	}
}
