package ca.nrc.dtrc.elasticsearch.es7;

import ca.nrc.dtrc.elasticsearch.ESFactory;
import ca.nrc.dtrc.elasticsearch.ElasticSearchException;
import ca.nrc.dtrc.elasticsearch.StreamlinedClient;

public class StreamlinedClient_v7 extends StreamlinedClient {

	public StreamlinedClient_v7(ESFactory _esFactory) throws ElasticSearchException {
		super(_esFactory);
	}

	@Override
	protected ESFactory makeESFactory(String _indexName, Double _sleepSecs, ESFactory.ESOptions[] options) throws ElasticSearchException {
		ESFactory factory = new ES7Factory(_indexName);
		factory.sleepSecs = _sleepSecs;
		return factory;
	}
}
