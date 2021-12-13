package ca.nrc.data.bin;

import ca.nrc.data.harvesting.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.io.FileUtils.cleanDirectory;

public class CmdHarvestQuery extends DataCmd {

	PageHarvester harvester = new PageHarvester_HtmlCleaner();

	String webQuery = null;
	String bingKey = null;
	Path outputDir = null;
	int maxHits = 100;
	boolean fullText = false;

	int hitCounter = 0;

	@Override
	public String getUsageOverview() {
		return "Harvest one or more web pages.";
	}

	public CmdHarvestQuery(String name) {
		super(name);
	}


	@Override
	public void execute() throws Exception {
		webQuery = getOptWebQuery();
		bingKey = getOptBingKey();
		outputDir = getOptOutputDir();
		maxHits = getOptMaxHits();
		fullText = getOptHTMLFullText();

		if (webQuery == null) {
			usageMissingOption(DataCmd.OPT_WEB_QUERY);
		}
		if (bingKey == null) {
			usageMissingOption(DataCmd.OPT_BING_KEY);
		}

		echo("Harvesting hits form a web query");
		echo(1);
		{
			echo("Query     : "+webQuery);
			echo("Max hits  : "+maxHits);
			echo("Output dir :"+ outputDir);
		}
		echo(-1);
		echo();

		this.harvest_web_query();

		System.out.println("DONE");
	}

	void harvest_web_query()
		throws DataCLIException {

		initiatlizeWebsearchOutputDirectory();
		
		BingSearchEngine sEngine;
		try {
			sEngine = new BingSearchEngine(bingKey);
		} catch (SearchEngine.SearchEngineException e) {
			throw new DataCLIException("Could not instantiate the Bing search engine", e);
		}
		SearchEngine.Query query = new SearchEngine.Query(webQuery);
		query.setType(SearchEngine.Type.NEWS);
		query.setMaxHits(maxHits);

		SearchResults results;
		try {
			results = sEngine.search(query);
		} catch (SearchEngine.SearchEngineException e) {
			throw new DataCLIException(e);
		}

		echo("Found "+results.retrievedHits.size()+" hits.");
		{
			hitCounter = 1;
			for (SearchEngine.Hit aHit: results.retrievedHits) {
				echo("Saving hit #"+hitCounter);
				downloadAndSaveHit(aHit, outputDir, fullText);
			}
		}

	}

	private void initiatlizeWebsearchOutputDirectory() throws DataCLIException {
		File outputDirFile = outputDir.toFile();
		if (! outputDirFile.exists()) {
			outputDirFile.mkdirs();
		}

		try {
			cleanDirectory(outputDirFile);
		} catch (IOException e) {
			throw new DataCLIException(
				"Could not clear the output directory: "+outputDirFile.toString(), e);
		}

		Map<String,Object> params = new HashMap<String,Object>();
		params.put("webQuery", webQuery);
		params.put("maxHits", maxHits);
		File paramsFile = new File(outputDirFile, "_params.json");
		try {
			new ObjectMapper().writeValue(paramsFile, params);
		} catch (IOException e) {
			throw new DataCLIException(
				"Could not clear the parameters file: "+paramsFile.toString(), e);
		}


	}

	private void downloadAndSaveHit(SearchEngine.Hit aHit, Path outputDir,
		Boolean fullText) throws DataCLIException {

			try {
			harvester.harvestSinglePage(aHit.url);
		} catch (PageHarvesterException e) {
			System.out.println("Could not download hit: "+aHit.url+"\nException was:\n"+e.getMessage());
		}
		String pageText = null;
		try {
			pageText = harvester.getMainText();
		} catch (PageHarvesterException e) {
			throw new DataCLIException(e);
		}

		saveHit(hitCounter, aHit, pageText);
		hitCounter++;
	}

	private void saveHit(int hitCounter, SearchEngine.Hit  aHit, String pageText)
		throws DataCLIException {
		String hitFileName;

		File hitFile = getHitFile(hitCounter, aHit, "txt");
		hitFileName = hitFile.getName().toString();

		String hitContent = "------------------------------------------------\n\nTitle: "+aHit.title+"\n\nURL: "+aHit.url.toString()+"\n\n------------------------------------------------\n\n" + pageText;

		try {
			Files.write(hitFile.toPath(), hitContent.getBytes());
		} catch (IOException e) {
			throw new DataCLIException(e);
		}
	}

	private File getHitFile(int hitNum, SearchEngine.Hit aHit, String extension) {

		String shortTitle = aHit.title;
		if (shortTitle.length() > 20) {
			shortTitle = shortTitle.substring(0, 20);
		}
		shortTitle = shortTitle.replaceAll("[\\W]+", "-");

		String hitNumStr = String.format("%04d-", hitNum);
		File hitFile = new File(
			outputDir.toString(),
			hitNumStr+shortTitle + "." + extension);

		return hitFile;
	}

}
