package ca.nrc.ui.commandline;

import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/*
 * Allows a class to 'listen' to echo (aka 'print') messages sent by another class.
 * 
 * This is useful when you have a CLI program that invokes a class X, and that class wants
 * to print something to the CLI's console. This console could be STDOUT, or it could be something else.
 * By passing an EchoListener to the class X, the CLI program can control where the messages go
 * without the X class having to care.
 */

public class UserIO {
	
	public static enum Verbosity {
		Levelnull, Level0, Level1, Level2, Level3, Level4, Level5,
		LevelMax
	};

	// By default, only printe messages whose level is lower than Level1
	protected Verbosity verbosity = Verbosity.Level1;
	
	
	public UserIO() {
		initialize(null);
	}
	
	
	public UserIO(Verbosity _verbosity) {
		initialize(_verbosity);
	}

	public void initialize(Verbosity _verbosity) {
		if (_verbosity != null) {
			this.verbosity = _verbosity;
		}
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

	public void echo() {
		echo("");
	}

	public void echo(int indentLevelChange) {
		if (indentLevelChange > 0) currentIndentation++;
		if (indentLevelChange < 0) currentIndentation--;
		if (currentIndentation < 0) {
			currentIndentation = 0;
		}
		if (currentIndentation > 5) {
			currentIndentation = 5;
		}
	}
	
	public void echo(String message, Boolean newline) {
		echo(message, (Verbosity)null, newline);
	}
	
//	public void echo(String message, Verbosity level, Boolean newline) {
//		echo(message, 0, level, newline);
//	}

	
	public void echo(String message) {
		echo(message, Verbosity.Level0, (Boolean)null);
	}
	
	public void echo(String message, Verbosity level) {
		echo(message, level, (Boolean)null);
	}

	public void echo(String message, Verbosity messageLevel,
		Boolean newline) {

		Logger tLogger = Logger.getLogger("ca.nrc.ui.commandline.UserIO.echo");

		if (messageLevel == null) {
			messageLevel = Verbosity.Level0;
		}

		if (newline == null) newline = true;
		if (verbosityLevelIsMet(messageLevel)) {
			String indentPadding = indentation.get(currentIndentation);

			message = message.replaceAll("\n", "\n"+indentPadding);
			
			message = indentPadding + message;
			tLogger.trace("Printing the message");
			System.out.print(message);
			
			if (newline) {
				System.out.println();
			}
		}
	}
	
	public boolean verbosityLevelIsMet(Verbosity messageLevel) {
		Logger tLogger =
			Logger.getLogger("ca.nrc.ui.commandline.UserIO.verbosityLevelIsMet");
		tLogger.trace("messageLevel="+messageLevel+", verbosity="+verbosity);
		boolean answer = false;
		Integer messageLevelInt = verbosityToInt(messageLevel);
		Integer verbosityInt = verbosityToInt(verbosity);
		if (verbosityInt != null &&
			messageLevelInt != null &&
			verbosityInt >= messageLevelInt) {
			answer = true;
		}
		tLogger.trace("returning answer="+answer);
		return answer;
	}

	public static Integer verbosityToInt(Verbosity level) {
		Integer levelNum = null;
		if (level != null) {
			if (level == Verbosity.LevelMax) {
				levelNum = Integer.MAX_VALUE;
			} else {
				String minLevelStr = level.toString();

				Pattern pattern = Pattern.compile("^Level([\\d]+|null)$");
				Matcher matcher = pattern.matcher(minLevelStr);
				boolean matches = matcher.matches();
				String levelNumStr = matcher.group(1);

				if (!levelNumStr.equals("null")) {
					levelNum = Integer.parseInt(levelNumStr);
				}
			}
		}
		return levelNum;
	}
	
	
	public static Verbosity verbosityLevel(Integer levelNum) {
		Verbosity level = null;
		if (levelNum != null) {
			if (levelNum > 5) levelNum = 5;
			if (levelNum < 0) levelNum = 0;
			String levelStr = "Level" + levelNum;
			level = Verbosity.valueOf(levelStr);
		}
		return level;
	}
	
	public UserIO setVerbosity(Integer level) {
		this.verbosity = verbosityLevel(level);
		return this;
	}

	public UserIO setVerbosity(Verbosity level) {
		this.verbosity = level;
		return this;
	}
	
	public Boolean prompt_yes_or_no(String mess) {
		Logger tLogger = Logger.getLogger("ca.nrc.ui.commandline.UserIO.prompt_yes_or_no");
		tLogger.trace("invoked with mess="+mess);
		Pattern patt = Pattern.compile("^\\s*([yn])");
		boolean answer = false;
		while (true) {
			tLogger.trace("echoing the prompt");
			echo("\n"+mess+" (y/n)\n> ", Verbosity.Level0, false);
			Scanner input = new Scanner(System.in);
			String yn = input.nextLine();
			Matcher matcher = patt.matcher(yn);
			if (matcher.matches()) {
				answer = false;
				String ynGroup = matcher.group(1);
				if (ynGroup.equals("y")) {
					answer = true;
				}
				break;
			}
		}
		return answer;
	}

	public void abort() {
		echo("Aborting command");
	}	
	
}
