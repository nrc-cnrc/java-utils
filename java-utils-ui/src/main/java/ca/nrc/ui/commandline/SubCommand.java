package ca.nrc.ui.commandline;

import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;


public abstract class SubCommand {
		
//	public static enum Verbosity {Levelnull, Level0, Level1, Level2, Level3, Level4, Level5};
//	protected static UserIO.Verbosity verbosity = UserIO.Verbosity.Level1;
	
	public static final String OPT_VERBOSITY = "verbosity";	
	
	private String name = null;
		public String getName() {return name;}
		
	@SuppressWarnings("unused")
	private String usageOneLiner = null;
	
//	private UserIO user_io = new UserIO();
	protected UserIO user_io = null;
		protected UserIO getUserIO() {
			if (user_io == null) {
				UserIO.Verbosity verbosity = getVerbosity();
				user_io = new UserIO(verbosity);
			}
			return user_io;
		}
	
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
	
	public boolean hasOption(String optName) {
		boolean answer = cmdLine.hasOption(optName);
		return answer;
	}
	
	
	public void run(CommandLine _cmdLine, String commandName) throws Exception {
		this.cmdLine = _cmdLine;
		
		UserIO.Verbosity currVerbosity = getUserIO().verbosity;
		if (currVerbosity != UserIO.Verbosity.Levelnull && currVerbosity != UserIO.Verbosity.Level0) {
			echo("Executing sub-command "+commandName);
		}
		
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
		usage(errMessage, getUsageOverview(), options);
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
	
	public void echo() {
		getUserIO().echo();
	}

	public void echo(int indentLevelChange) {
		getUserIO().echo(indentLevelChange);
	}
	
	public void echo(String message, boolean newline) {
		getUserIO().echo(message, newline);
	}
	
	public void echo(String message, UserIO.Verbosity level, boolean newline) {
		getUserIO().echo(message, level, newline);
	}

	
	public  void echo(String message) {
		getUserIO().echo(message);
	}
	
	public void echo(String message, UserIO.Verbosity level) {
		getUserIO().echo(message, level);
	}
	
	public  void echo(String message, int indentLevelChange) {
		getUserIO().echo(message, indentLevelChange);
	}

	public void echo(String message, int indentLevelChange, UserIO.Verbosity level) {
		getUserIO().echo(message, indentLevelChange, level);
	}
	
	public void echo(String message, int indentLevelChange, UserIO.Verbosity level, Boolean newline) {
		getUserIO().echo(message, indentLevelChange, level, newline);
	}
	
	protected boolean verbosityLevelIsMet(UserIO.Verbosity minLevel) {
		return getUserIO().verbosityLevelIsMet(minLevel);
	}

	protected  Integer verbosityToInt(UserIO.Verbosity level) {
		return getUserIO().verbosityToInt(level);
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

	public void error(String message) {
		echo("*********************************************************************", UserIO.Verbosity.Level0);
		echo("* ERROR: "+message, UserIO.Verbosity.Level0);
		echo("*********************************************************************", UserIO.Verbosity.Level0);
		
		if (sysexitOnBadUsage) {
			System.exit(1);
		} else {
			throw new InvalidParameterException(message);
		}
	}
	
	public static UserIO.Verbosity verbosityLevel(Integer levelNum) {
		UserIO.Verbosity level = UserIO.Verbosity.Level0;
		if (levelNum > 5) levelNum = 5;
		if (levelNum < 0) levelNum = 0;
		String levelStr = "Level"+levelNum;
		level = UserIO.Verbosity.valueOf(levelStr);
		return level;
	}
	
	protected  UserIO.Verbosity getVerbosity() {
		UserIO.Verbosity verbLevel = UserIO.Verbosity.Level0;
		String verbOptionStr = getOptionValue(OPT_VERBOSITY, false);
		if (verbOptionStr == null) verbOptionStr = "0";
			try {
				if (verbOptionStr.matches("^\\s*null\\s*$")) {
					verbLevel = null;
				} else {
					Integer verbLevelInt = Integer.parseInt(verbOptionStr);
					String verbLevelStr = "Level"+verbLevelInt;
					verbLevel = UserIO.Verbosity.valueOf(verbLevelStr);
				}
			} catch (Exception e) {
				usageBadOption(OPT_VERBOSITY, "should have benn an integer");
			}
		
		return verbLevel;
	}
	
//	protected Boolean prompt_yes_or_no(String mess) {
//		Pattern patt = Pattern.compile("^\\s*([yn])");
//		boolean answer = false;
//		while (true) {
//			echo("\n"+mess+" (y/n)\n> ", false);	
//			Scanner input = new Scanner(System.in);
//			String yn = input.nextLine();
//			Matcher matcher = patt.matcher(yn);
//			if (matcher.matches()) {
//				answer = false;
//				String ynGroup = matcher.group(1);
//				if (ynGroup.equals("y")) {
//					answer = true;
//				}
//				break;
//			}
//		}
//		return answer;
//	}
	

}
