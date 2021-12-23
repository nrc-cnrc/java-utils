package ca.nrc.dtrc.elasticsearch;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ES_API {
	protected ESFactory esFactory;

	protected ResponseMapper respMapper = new ResponseMapper((String)null);

	protected ObjectMapper objectMapper = new ObjectMapper();

	public ES_API(ESFactory _esFactory) throws ElasticSearchException {
		if (_esFactory == null) {
			throw new ElasticSearchException("ESFactory API should not be null");
		}
		this.esFactory = _esFactory;
	}

	public ESUrlBuilder urlBuilder() {
		return esFactory.urlBuilder();
	}

	public String indexName() {
		return esFactory.indexName;
	}

	public Transport transport() {
		Transport transp = esFactory.transport();
		return transp;
	}

	public void sleep() {
		esFactory.sleep();
	}

}
