package ca.nrc.dtrc.elasticsearch.engine;

import ca.nrc.dtrc.elasticsearch.ElasticSearchException;

public class MissingESPluginException extends ElasticSearchException {
	public MissingESPluginException(String plugin) {
		super("Missing plugin "+plugin);
	}
}
