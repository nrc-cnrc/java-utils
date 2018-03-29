package ca.nrc.ui.commandline;

import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import ca.nrc.ui.commandline.SubCommand.Verbosity;

public abstract class SubCommand {
	
	public static enum Verbosity {Levelnull, Level0, Level1, Level2, Level3, Level4, Level5};
	protected static Verbosity verbosity = Verbosity.Level0;
	
	public static final String OPT_VERBOSITY = "verbosity";	
	
	private String name = null;
		public String getName() {return name;}
		
	private String usageOneLiner = null;
	
	private static int currentIndentation = 0;
	public static final Map<Integer,String> indentation = new HashMap<Integer,String>();
	{
		indentation.put(0, "");
		indentation.put(1, "  ");
		indentation.put(2, "    ");
		indentation.put(3, "      ");		
		indentation.put(4, "          ");		
		indentation.put(5, "            ");		
		indentation.put(6, "              ");
		indentation.put(7, "                ");		
		indentation.put(8, "                  ");
		indentation.put(9, "                    ");		
		indentation.put(10, "                      ");		
		
	}
	
	public Options options = new Options();

	protected CommandLine cmdLine;
	protected static boolean sysexitOnBadUsage = true;
		public void setSysexitOnBadUsage(boolean flag) {sysexitOnBadUsage = flag;}
		
	public SubCommand(String _cmdName) {
		initialize(_cmdName, true);
	}
	
	public SubCommand(String _cmdName, boolean _sysexitOnBadUsage) {
		initialize(_cmdName, _sysexitOnBadUsage);
	}
	
	public SubCommand() {
	}

	private void initialize(String _cmdName, boolean _sysexitOnBadUsage) {
		this.name = _cmdName;
		this.sysexitOnBadUsage = _sysexitOnBadUsage;
	}

	public abstract void execute() throws Exception;
	public abstract String getUsageOverview();

	public SubCommand addOption(Option opt) {
		options.addOption(opt);
		return this;
	}
	
	public String getOptionValue(String optName) {
		return getOptionValue(optName, true);
	}
	
	public String getOptionValue(String optName, boolean failIfAbsent) {
		String optValue = cmdLine.getOptionValue(optName);
		if (optValue == null && failIfAbsent) {
			usageMissingOption(optName);
		}
		
		return optValue;
	}

	
	public void run(CommandLine _cmdLine, String commandName) throws Exception {
		echo("Executing sub-command "+commandName);
		this.cmdLine = _cmdLine;
		this.execute();
	}
	
	public void setCommandLine(CommandLine _cmdLine) {
		this.cmdLine = _cmdLine;
	}
	
	public static void setCmdLineOptions(SubCommand cmd, Map<String,String> options) {
		MockCommandLine mockCmdLine = new MockCommandLine(new String[] {}, options);
		cmd.setCommandLine(mockCmdLine);
	}
	
	public static void setCmdLineOptions(SubCommand cmd, String... options) {
		if ((options.length % 2) != 0) {
			throw new IllegalArgumentException("List of options should contain an even number of elements. It was: "+String.join(", ", options));
		}
		Map<String,String> optHash = new HashMap<String,String>();
		for (int ii = 0; ii < options.length / 2; ii++) {
			optHash.put(options[ii], options[ii+1]);
		}
		setCmdLineOptions(cmd, optHash);
	}	
	
	protected void usage(String errMessage) {
		error(errMessage);

	}	
	
	public static void usage(String errMessage, String usageMessage, Options cmdLineOptions) {
		usageMessage += "\n\nAVAILABLE OPTIONS:\n\n";
		errMessage = "\n\n*****************\n* ERROR: "+errMessage+"\n*****************\n\n";
		
		HelpFormatter formatter = new HelpFormatter();
		formatter.setDescPadding(0);
		String header = "\nOPTION DETAILS:\n";
		formatter.printHelp(usageMessage, header, cmdLineOptions, errMessage, true);
		System.exit(1);
	}
	
	public static void echo() {
		echo("");
	}

	public static void echo(int indentLevelChange) {
		if (indentLevelChange > 0) currentIndentation += 2;
		if (currentIndentation > 10) currentIndentation = 10;
		if (indentLevelChange < 0) currentIndentation -= 2;
		if (currentIndentation < 0) currentIndentation = 0;
	}
	
	public static void echo(String message, boolean newline) {
		echo(message, 0, Verbosity.Level0, newline);
	}
	
	public static void echo(String message, Verbosity level, boolean newline) {
		echo(message, 0, level, newline);
	}

	
	public static void echo(String message) {
		echo(message, 0, Verbosity.Level0);
	}
	
	public static void echo(String message, Verbosity level) {
		echo(message, 0, level);
	}
	
	public static void echo(String message, int indentLevelChange) {
		echo(message, indentLevelChange, Verbosity.Level0);
	}

	public static void echo(String message, int indentLevelChange, Verbosity level) {
		echo(message, indentLevelChange, level, null);
	}
	
	public static void echo(String message, int indentLevelChange, Verbosity level, Boolean newline) {
		if (newline == null) newline = true;
		if (verbosityLevelIsMet(level)) {
			
			String indentPadding = indentation.get(currentIndentation);
		
			if (indentLevelChange > 0) currentIndentation += 1;
			if (currentIndentation > 10) currentIndentation = 10;
			
			message = message.replaceAll("\n", "\n"+indentPadding);
			
			message = indentPadding + message;
			System.out.print(message);
			
			if (newline) {
				System.out.println();
				if (indentLevelChange < 0) currentIndentation -= 1;
				if (currentIndentation < 0) currentIndentation = 0;
			}
	
		}
	}
	
	protected static boolean verbosityLevelIsMet(Verbosity minLevel) {
		boolean answer = false;
		Integer minLevelInt = verbosityToInt(minLevel);
		Integer verbosityInt = verbosityToInt(verbosity);
		if (verbosityInt != null && minLevelInt != null && verbosityInt >= minLevelInt) answer = true;
		return answer;
	}

	protected static Integer verbosityToInt(Verbosity level) {
		Integer levelNum = null;
		if (level != null) {
			String minLevelStr = level.toString();
			
			Pattern pattern = Pattern.compile("^Level([\\d]+|null)$");
	        Matcher matcher = pattern.matcher(minLevelStr);
	        boolean matches = matcher.matches();
			String levelNumStr = matcher.group(1);
			
			if (!levelNumStr.equals("null")) {
				levelNum = Integer.parseInt(levelNumStr);
			}
		}
		return levelNum;
	}

	public void usageMissingOption(String optionName) {
		usage("Sub-command '"+name+"' requires a value for the '"+optionName+"' option.");
	}

	public void usageMissingOption(String[] optionNames) {
		String optionNamesStr = "";
		for (int ii=0; ii < optionNames.length; ii++) {
			optionNamesStr += optionNames[ii]+", ";
		}
		usage("Sub-command '"+name+"' requires a value for at least one of the following options: "+optionNamesStr);
	}
	
	public void usageBadOption(String optionName, String reason) {
		usage("Bad value of option '"+optionName+"' : "+reason);
	}

	public static void error(String message) {
		echo("*********************************************************************");
		echo("* ERROR: "+message);
		echo("*********************************************************************");
		
		if (sysexitOnBadUsage) {
			System.exit(1);
		} else {
			throw new InvalidParameterException(message);
		}
	}
	
	public static Verbosity verbosityLevel(Integer levelNum) {
		Verbosity level = Verbosity.Level0;
		if (levelNum > 5) levelNum = 5;
		if (levelNum < 0) levelNum = 0;
		String levelStr = "Level"+levelNum;
		level = Verbosity.valueOf(levelStr);
		return level;
	}
	
	protected  Verbosity getVerbosity() {
		Verbosity verbLevel = Verbosity.Level0;
		String verbOptionStr = getOptionValue(OPT_VERBOSITY, false);
		if (verbOptionStr == null) verbOptionStr = "0";
			try {
				if (verbOptionStr.matches("^\\s*null\\s*$")) {
					verbLevel = null;
				} else {
					Integer verbLevelInt = Integer.parseInt(verbOptionStr);
					String verbLevelStr = "Level"+verbLevelInt;
					verbLevel = Verbosity.valueOf(verbLevelStr);
				}
			} catch (Exception e) {
				usageBadOption(OPT_VERBOSITY, "should have benn an integer");
			}
		
		return verbLevel;
	}
	

}
