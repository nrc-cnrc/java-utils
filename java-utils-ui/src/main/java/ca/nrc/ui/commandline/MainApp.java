package ca.nrc.ui.commandline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import ca.nrc.datastructure.Pair;


/*
 * Streamlined skeleton for a class with a main() method.
 */

public abstract class MainApp {
	
	public static final Map<Integer,String> indentation = new HashMap<Integer,String>();
	{
		indentation.put(0, "");
		indentation.put(1, "  ");
		indentation.put(2, "    ");
		indentation.put(3, "      ");		
		indentation.put(4, "        ");		
	}
	
	private static int currentIndentation = 0;

	abstract protected String configFromCmdLine(CommandLine cmdLine);
	abstract protected void run() throws Exception;
	abstract protected Pair<String,Options> defineCommandLineSpecs();

	protected static Options options = new Options();
	protected static String usageMessage = "";
	protected static CommandLine cmdLine = null;

	protected static MainApp makeMainApp() throws Exception {
		if (1 - 1 == 0) throw new Exception("You need to override makeMainApp in your MainApp subclass");
		return null;
	}

	
	public static void configAndRun(MainApp app, String[] args) throws Exception {
		
		Pair<String,Options> argSpecs = app.defineCommandLineSpecs();
		Options options = argSpecs.getSecond();		
		String usageMessage = argSpecs.getFirst();

		String errMess = null;
		try {
			cmdLine = new DefaultParser().parse(options, args);
		} catch (ParseException e) {
			errMess = "Bad command line options\n";
		}
		
		if (errMess == null) {
			errMess = app.configFromCmdLine(cmdLine);
		}
		
		if (errMess != null) {
			usage(errMess, usageMessage, options);
		}
		
		echo("Starting");
		app.options = options;
		app.usageMessage = usageMessage;
		app.run();
		echo("Done");
	}
			
	protected static void usage(String errMessage) {
		usage(errMessage, usageMessage, options);
	}	
	
	public static void usage(String errMessage, String usageMessage, Options cmdLineOptions) {
		usageMessage += "\n\nOPTIONS are:\n\n";
		errMessage = "\n\n*****************\n* ERROR: "+errMessage+"\n*****************\n\n";
		
		HelpFormatter formatter = new HelpFormatter();
		formatter.setDescPadding(0);
		String header = "\nDetails";
		formatter.printHelp(usageMessage, header, cmdLineOptions, errMessage, true);
		System.exit(1);
	}

	public static void echo(int indentLevelChange) {
		if (indentLevelChange > 0) currentIndentation += 2;
		if (currentIndentation > 4) currentIndentation = 4;
		if (indentLevelChange < 0) currentIndentation -= 2;
		if (currentIndentation < 0) currentIndentation = 0;
	}
	
	public static void echo(String message) {
		echo(message, 0);
	}
	
	public static void echo(String message, int indentLevelChange) {
		if (indentLevelChange > 0) currentIndentation += 2;
		if (currentIndentation > 4) currentIndentation = 4;
		
		message = indentation.get(currentIndentation) + message;
		System.out.println(message);

		if (indentLevelChange < 0) currentIndentation -= 2;
		if (currentIndentation < 0) currentIndentation = 0;
		
	}
	

	public static void echo2(String message) {
		echo2(message, 0);
	}

	public static void echo2(String message, int indentLevelChange) {
		if (indentLevelChange > 0) currentIndentation += 2;
		if (currentIndentation > 4) currentIndentation = 4;
		
		message = indentation.get(currentIndentation) + message;
		System.out.println(message);

		if (indentLevelChange < 0) currentIndentation -= 2;
		if (currentIndentation < 0) currentIndentation = 0;
	}
	public static void usageMissingOption(String optionName) {
		usageMissingOption(optionName, "");
	}
	
	public static void usageMissingOption(String optionName, String message) {
		usageMissingOption(optionName, message, null);
	}

	public static void usageMissingOption(String optionName, String message, String command) {
		message = messageMissingOption(optionName, message, command);
		usage(message);
	}
	
	public static String messageMissingOption(String optionName, String message, String command) {
		if (optionName != null) {
			message = message+"\nAction "+command+" requires a value for the '"+optionName+"' option.";
		}
		return message;
	}
	

}
