package ca.nrc.dtrc.elasticsearch.es7mi;

import ca.nrc.dtrc.elasticsearch.ESFactory;
import ca.nrc.dtrc.elasticsearch.ElasticSearchException;
import ca.nrc.dtrc.elasticsearch.StreamlinedClient;

public class StreamlinedClient_v7mi extends StreamlinedClient {

	public StreamlinedClient_v7mi(ESFactory _esFactory) throws ElasticSearchException {
		super(_esFactory);
	}

	@Override
	protected ESFactory makeESFactory(String _indexName, Double _sleepSecs, ESFactory.ESOptions[] options) throws ElasticSearchException {
		ESFactory factory = new ES7miFactory(_indexName);
		factory.sleepSecs = _sleepSecs;
		return factory;
	}
}