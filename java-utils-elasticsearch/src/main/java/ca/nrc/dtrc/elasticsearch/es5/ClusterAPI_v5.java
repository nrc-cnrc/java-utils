package ca.nrc.dtrc.elasticsearch.es5;

import ca.nrc.dtrc.elasticsearch.ESFactory;
import ca.nrc.dtrc.elasticsearch.ElasticSearchException;
import ca.nrc.dtrc.elasticsearch.cluster.ClusterAPI;

public class ClusterAPI_v5 extends ClusterAPI {
	public ClusterAPI_v5(ESFactory _esFactory) throws ElasticSearchException {
		super(_esFactory);
	}
}
