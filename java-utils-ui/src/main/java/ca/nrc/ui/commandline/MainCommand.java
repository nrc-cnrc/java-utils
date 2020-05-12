package ca.nrc.ui.commandline;

import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import ca.nrc.datastructure.Pair;

/*******************************************************************************
 * 
 * This class helps write command line application that follow the pattern:
 * 
 *    command subCommand args options
 *   
 * For example, git and svn follow this pattern.
 *******************************************************************************/

public class MainCommand {
	
	// This class exists only so MainCommand can invoke the error() method of
	//   SubCommand (which cannot be instantiated because it is abstract).
	//
	protected class SubCommandNull extends SubCommand {
		@Override
		public void execute() throws Exception {}
		@Override
		public String getUsageOverview() {return "";}
	}
	
	private String usageOneLiner = null;
	private Option optVerbosity = null;
	private Option optInteractive = null;
	Options allCommandsOptions = new Options();
	Map<String,SubCommand> subCommands = new HashMap<String,SubCommand>();
	String subCommandName = null;
	
	
	
	public MainCommand(String _usageOneLiner) {
		initialize(_usageOneLiner);
	}
	
	private void initialize(String _usageOneLiner) {
		this.usageOneLiner = _usageOneLiner;
		
		optVerbosity = Option.builder(null).longOpt(SubCommand.OPT_VERBOSITY)
			.desc("Verbosity level.").hasArg().argName("VERBOSITY_LEVEL")
			.build();
		allCommandsOptions.addOption(optVerbosity);
		
		optInteractive = Option.builder(null).longOpt(SubCommand.OPT_INTERACTIVE)
			.desc("If set, command will keep prompting the user for inputs.")
			.argName("INTERACTIVE")
			.build();
		allCommandsOptions.addOption(optInteractive);
		
	}

	public void addSubCommand(SubCommand subCommand) throws CommandLineException {
		subCommand.addOption(optVerbosity);
		for (Option anOpt: subCommand.options.getOptions()) {
			allCommandsOptions.addOption(anOpt);
		}
		String subCmdName = subCommand.getName();
		if (subCommands.containsKey(subCmdName)) {
			throw new CommandLineException("This MainCommand already contains a sub-command with name: "+subCmdName);
		}
		subCommands.put(subCmdName, subCommand);
	}

	public void run(String[] args) throws Exception {
		Pair<SubCommand, CommandLine> cmdAndConfig = makeSubCommandAndConfig(args);
		SubCommand command = cmdAndConfig.getFirst();
		CommandLine cmdLine = cmdAndConfig.getSecond();
				
		command.run(cmdLine, subCommandName);
	}
	
	public Pair<SubCommand, CommandLine> makeSubCommandAndConfig(String[] args) {
		CommandLine cmdLine = null;
		try {
			cmdLine = new DefaultParser().parse(allCommandsOptions, args);
		} catch (Exception e) {
			usageBadCommandLine(e.getMessage());
		}
		
		if (cmdLine.getArgs().length == 0) {
			usageBadSubCommand();
		}
		
		subCommandName = cmdLine.getArgs()[0];
		if (!subCommands.keySet().contains(subCommandName)) {
			usageBadSubCommand();
		}

		SubCommand command = getSubCommandWithName(subCommandName);
		
		List<String> argsList = cmdLine.getArgList();
		if (argsList.size() > 1) {
			usageBadCommandLine("Too many arguments. The command should have only one argument.\nArguments were: "+String.join(", ", argsList));
		}
		
		
		
		
		return Pair.of(command, cmdLine);
	}
	
	public SubCommand getSubCommandWithName(String cmdName) {
		SubCommand cmd = subCommands.get(cmdName);
		return cmd;
		
	}
	
	private void usageBadSubCommand() {
		String overview = "Usage: ["+String.join(" | ", subCommands.keySet())+"] <OPTION>?";
		echo(overview);
		echo("");
		echo(1);
		echo(usageOneLiner);
		echo(-1);
		echo("");
		error("First argument must be one of the following commands: "+String.join(", ", subCommands.keySet()));		
	}
	
	private void usageBadCommandLine(String message) {
		String overview = "Usage: ["+String.join(" | ", subCommands.keySet())+"] <OPTION>?";
		echo(overview);
		echo("");
		echo(1);
		echo(usageOneLiner);
		echo(-1);
		echo("");
		error(message);		
	}
	
	private void usage(String errMessage) {
		SubCommand.usage(errMessage, usageOneLiner, allCommandsOptions);

	}
	
	private void echo(String message) {
		new UserIO().echo(message);
	}
	
	private void echo(int indentLevelChange) {
		new UserIO().echo(indentLevelChange);
	}
	
	public void error(String message) {
		System.out.println("*********************************************************************");
		System.out.println("* ERROR: "+message);
		System.out.println("*********************************************************************");
		
		if (SubCommand.sysexitOnBadUsage) {
			System.exit(1);
		} else {
			throw new InvalidParameterException(message);
		}
	}
	
}
