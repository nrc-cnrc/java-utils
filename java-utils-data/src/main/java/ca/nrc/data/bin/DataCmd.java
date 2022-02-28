package ca.nrc.data.bin;

import ca.nrc.ui.commandline.SubCommand;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class DataCmd extends SubCommand {

	public static final String OPT_WEB_QUERY = "web-query";
	public static final String OPT_BING_KEY = "bing-key";

	public static final String OPT_OUTPUT_DIR = "output-dir";
	static final String OPT_MAX_HITS = "max-hits";
	public static final String OPT_HTML_FULL_TEXT = "html-full-text";
	public static final String OPT_PIPELINE = "pipeline";

	public static final String OPT_URL = "url";

	public DataCmd(String name) {
		super(name);
	}

	protected String getOptWebQuery() {
		String query = getOptionValue(DataCmd.OPT_WEB_QUERY, false);
		return query;
	}

	protected String getOptBingKey() {
		String key = getOptionValue(DataCmd.OPT_BING_KEY, false);
		return key;
	}

	protected Path getOptOutputDir() {
		String outDirStr = getOptionValue(DataCmd.OPT_OUTPUT_DIR, true);
		return Paths.get(outDirStr);
	}

	protected Integer getOptMaxHits() {
		String maxHitsStr = getOptionValue(DataCmd.OPT_MAX_HITS, false);
		Integer maxHits = 100;
		try {
			if (maxHitsStr != null) maxHits = Integer.parseInt(maxHitsStr);
		} catch (Exception e) {
			usageBadOption(DataCmd.OPT_MAX_HITS, "Should have been an integer");
		}
		return maxHits;
	}

	protected boolean getOptHTMLFullText() {
		boolean fullText = hasOption(DataCmd.OPT_HTML_FULL_TEXT);
		return fullText;
	}

	protected boolean getOptPipeline() {
		boolean pipeline = hasOption(DataCmd.OPT_PIPELINE);
		return pipeline;
	}

	protected URL getOptURL() {
		URL url = null;
		String urlStr = getOptionValue(DataCmd.OPT_URL, false);
		if (urlStr != null) {
			try {
				url = new URL(urlStr);
			} catch (MalformedURLException e) {
				usage("--url value was not a valid URL");
			}
		}

		return url;
	}
}
