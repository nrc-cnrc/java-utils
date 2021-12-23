package ca.nrc.dtrc.elasticsearch;

import ca.nrc.dtrc.elasticsearch.ESUrlBuilder;
import ca.nrc.dtrc.elasticsearch.ElasticSearchException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.json.JSONObject;

import java.net.URL;

/**
 * Use this class to check the version of ESFactory server.
 */
public class ESVersionChecker {

	private String host = null;
	private Integer port = null;
	private static String cachedVersion = null;
	Transport transport = new Transport(null, null);

	public ESVersionChecker(String _host, int _port) {
		host = _host;
		port = _port;
	}

	@JsonIgnore
	public String getRunningVersion(Boolean refreshCache) throws ElasticSearchException {
		if (refreshCache == null) {
			refreshCache = false;
		}
		if (refreshCache || cachedVersion == null) {
			cachedVersion = null;
			URL url = new ESUrlBuilder(null, host, port)
//				.noDocKeyword()
				.build();
			try {
				JSONObject jsonResponse = new JSONObject(transport.get(url));
				cachedVersion = jsonResponse.getJSONObject("version").getString("number");
			} catch (Exception e) {
				// If we got an exception, it means the server is not running.
				// So leave the version number at null.
			}
		}
		return cachedVersion;
	}

	public boolean isRunningVersion(int expVersion) throws ElasticSearchException {
		String actualVersion = getRunningVersion(false);
		boolean ok = matchesVersion(expVersion, actualVersion);
		if (!ok) {
			// Try again with cache refresh, in case we restarted ESFactory since
			// the time we cached the version number
			actualVersion = getRunningVersion(true);
			ok = matchesVersion(expVersion, actualVersion);
		}
		return ok;
	}

	private boolean matchesVersion(int expVersion, String actualVersion) {
		boolean matches = false;
		if (actualVersion != null) {
			matches = actualVersion.matches("^" + expVersion + ".*$");
		}
		return matches;
	}
}
