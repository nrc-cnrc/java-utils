package ca.nrc.data.bin;

import ca.nrc.data.harvesting.MainContentExtractor;
import ca.nrc.data.harvesting.PageHarvesterException;
import ca.nrc.data.harvesting.PageHarvester_HtmlCleaner;
import ca.nrc.debug.ExceptionHelpers;
import ca.nrc.ui.commandline.CommandLineException;
import org.json.JSONObject;

import java.net.URL;
import java.util.Scanner;

public class CmdHarvestPage extends DataCmd {

	PageHarvester_HtmlCleaner harvester = new PageHarvester_HtmlCleaner();

	public CmdHarvestPage(String name) {
		super(name);
	}

	@Override
	public String getUsageOverview() {
		return "Harvest text of a single web page";
	}

	@Override
	public void execute() throws Exception {
		boolean pipeline = getOptPipeline();
		URL url = getOptURL();
		if (pipeline && url != null) {
			usage("Options --pipeline and --url are mutually exclusive");
		}
		if (! pipeline && url == null) {
			usage("You need to provide at least one of the following options: --pipeline, --url");
		}

		if (pipeline) {
			runPipelineMode();
		} else {
			runSinglePageMode(url);
		}
	}

	private void runSinglePageMode(URL url) throws CommandLineException {
		try {
			harvester.harvestSinglePage(url);
		} catch (PageHarvesterException e) {
			throw new CommandLineException(e);
		}
		System.out.println(
			"MAIN text of: "+url+"\n" +
			"-------\n"+
			harvester.getMainText()+"\n"+
			"-------"
		);
	}

	private void runPipelineMode() throws BadJsonInputException {
		while (true) {
			JSONObject input = null;
			JSONObject output = new JSONObject();
			try {
				input = readInputFromSTDIN();
				if (input == null) {
					break;
				}
			} catch (BadJsonInputException e) {
				output = new JSONObject()
					.put("error", e.getMessage());
			}
			if (output != null) {
				try {
					output = harvestPage(input);
				} catch (Exception e) {
					output = new JSONObject()
					.put("error", ExceptionHelpers.printExceptionCauses(e));
				}
			}
			System.out.println(output.toString());
		}
	}

	private JSONObject harvestPage(JSONObject input) throws PageHarvesterException, PageHarvesterException {
		JSONObject output = new JSONObject();
		if (! input.has("url") && ! input.has("html")) {
			throw new MissingURLOrHtmlException(input);
		}
		if (input.has("url") && input.has("html")) {
			throw new URLandHtmlMutuallyExclusiveException(input);
		}
		if (input.has("url")) {
			String url = input.getString("url");
			harvester.harvestSinglePage(url);
			output = new JSONObject()
				.put("content", harvester.getMainText());
		} else {
			String html = input.getString("html");
			String extractorType = null;
			if (input.has("extractor_type")) {
				extractorType = input.getString("extractor_type");
			}
			String text = new MainContentExtractor().extract(html, extractorType);
			output = new JSONObject()
				.put("content", text);
		}

		return output;
	}


	private JSONObject readInputFromSTDIN() throws BadJsonInputException {
		JSONObject json = null;
		Scanner scanner = new Scanner(System.in);
		String line = scanner.nextLine();
		if (!line.isEmpty()) {
			try {
				json = new JSONObject(line);
			} catch (Exception e) {
				throw new BadJsonInputException(e, line);
			}
		}
		return json;
	}
}
