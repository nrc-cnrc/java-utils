package ca.nrc.data.bin;

import org.apache.commons.cli.Option;

import ca.nrc.ui.commandline.CommandLineException;
import ca.nrc.ui.commandline.MainCommand;
import ca.nrc.ui.commandline.SubCommand;;

public class DataCLI {
	
	protected static MainCommand defineMainCommand() throws CommandLineException {
		MainCommand mainCmd = new MainCommand("Command line console for iutools.");

		Option optWebQuery = Option.builder(null).longOpt(DataCmd.OPT_WEB_QUERY)
				.desc("A web engine query to use for retrieving documents from the web.").hasArg().argName("WEB_QUERY")
				.build();

		Option optBingKey = Option.builder(null).longOpt(DataCmd.OPT_BING_KEY)
			.desc("Subscription key for the Bing search engine API.").hasArg()
			.argName(DataCmd.OPT_BING_KEY.toUpperCase())
			.build();

		Option optOutputDir = Option.builder(null).longOpt(DataCmd.OPT_OUTPUT_DIR)
				.desc("Path of output directory").hasArg()
				.argName(DataCmd.OPT_OUTPUT_DIR.toUpperCase()).build();

		Option optMaxHits = Option.builder(null).longOpt(DataCmd.OPT_MAX_HITS)
				.desc("Max number of hits to retrieve.").hasArg().argName("NNN").build();

		Option optPipeline = Option.builder(null).longOpt(DataCmd.OPT_PIPELINE)
				.desc("Pipeline to use.").hasArg().argName("NNN").build();

		Option optHTMLFullText = Option.builder(null).longOpt(DataCmd.OPT_HTML_FULL_TEXT)
				.desc("Process the full text of an HTML file without attempting to filter 'container' words like navigation menus, banners, etc....")
				.build();

		// Harvest some web pages and save them to a directory
		SubCommand addCmd = new CmdHarvest("harvest")
				.addOption(optWebQuery)
				.addOption(optBingKey)
				.addOption(optOutputDir)
				.addOption(optMaxHits)
				.addOption(optHTMLFullText);
		mainCmd.addSubCommand(addCmd);
		

		return mainCmd;
	}

	public static void main(String[] args) throws Exception {
		MainCommand mainCmd = defineMainCommand();
		mainCmd.run(args);
	}
}
