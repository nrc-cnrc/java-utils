package ca.nrc.dtrc.elasticsearch.es7;

import ca.nrc.dtrc.elasticsearch.ESFactory;
import ca.nrc.dtrc.elasticsearch.ElasticSearchException;
import ca.nrc.dtrc.elasticsearch.cluster.ClusterAPI;

public class ClusterAPI_v7 extends ClusterAPI {
	public ClusterAPI_v7(ESFactory _esFactory) throws ElasticSearchException {
		super(_esFactory);
	}
}
