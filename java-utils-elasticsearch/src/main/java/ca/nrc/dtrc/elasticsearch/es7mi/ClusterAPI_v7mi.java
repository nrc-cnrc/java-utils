package ca.nrc.dtrc.elasticsearch.es7mi;

import ca.nrc.dtrc.elasticsearch.ESFactory;
import ca.nrc.dtrc.elasticsearch.ElasticSearchException;
import ca.nrc.dtrc.elasticsearch.cluster.ClusterAPI;

public class ClusterAPI_v7mi extends ClusterAPI {
	public ClusterAPI_v7mi(ESFactory _esFactory) throws ElasticSearchException {
		super(_esFactory);
	}
}