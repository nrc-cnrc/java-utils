package ca.nrc.dtrc.elasticsearch.engine;

import ca.nrc.dtrc.elasticsearch.ESFactory;
import ca.nrc.dtrc.elasticsearch.ES_API;
import ca.nrc.dtrc.elasticsearch.ElasticSearchException;
import org.json.JSONArray;

import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * API for configuring the ES engine. For configuring individual indices, use
 * the IndexAPI.
 */
public class EngineAPI extends ES_API {

	/** List of installed ESFactory plugins */
	private static Set<String> installedPlugins = null;

	public EngineAPI(ESFactory _esFactory) throws ElasticSearchException {
		super(_esFactory);
	}

	public void ensurePluginInstalled(String plugin) throws ElasticSearchException {
		if (installedPlugins == null) {
			installedPlugins = new HashSet<String>();
			URL url = urlBuilder().cat("plugins").build();
			String jsonResponse = transport().get(url);
			JSONArray plugins = new JSONArray(jsonResponse);
			for (Object aPlugin: plugins.toList()) {
				String compName = (String)((Map)aPlugin).get("component");
				installedPlugins.add(compName);
			}
		}
		if (!installedPlugins.contains(plugin)) {
			throw new MissingESPluginException(plugin);
		}
	}
}
