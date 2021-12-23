package ca.nrc.dtrc.elasticsearch.es5;


import ca.nrc.dtrc.elasticsearch.*;
import static ca.nrc.dtrc.elasticsearch.ESFactory.*;

/**
 * Client for carrying out ALL ES5 operations
 * @deprecated
 * The functionality of StreamlinedClient is now split in the following classes:
 * IndexAPI_v5, CrudAPI_v5, SearchAPI_v5 and ClusterAPI_v5.
 */
@Deprecated
public class StreamlinedClient_v5 extends StreamlinedClient {

	@Override
	protected ESFactory makeESFactory(String _indexName, Double _sleepSecs, ESOptions[] options) throws ElasticSearchException {
		ESFactory factory = new ES5Factory(_indexName);
		factory.sleepSecs = _sleepSecs;
		return factory;
	}

	public StreamlinedClient_v5(ESFactory _esFactory) throws ElasticSearchException {
		super(_esFactory);
	}
}