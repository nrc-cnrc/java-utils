package ca.nrc.dtrc.elasticsearch.es6;

public class MissingESPluginException extends ElasticSearchException {
	public MissingESPluginException(String plugin) {
		super("Missing plugin "+plugin);
	}
}
