package ca.nrc.ui.commandline;

import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ca.nrc.ui.commandline.SubCommand.Verbosity;

/*
 * Allows a class to 'listen' to echo (aka 'print') messages sent by another class.
 * 
 * This is useful when you have a CLI program that invokes a class X, and that class wants
 * to print something to the CLI's console. This console could be STDOUT, or it could be something else.
 * By passing an EchoListener to the class X, the CLI program can control where the messages go
 * without the X class having to care.
 */

public class UserIO {
	
	public static enum Verbosity {Levelnull, Level0, Level1, Level2, Level3, Level4, Level5};
	protected static Verbosity verbosity = Verbosity.Level1;
	
	
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
		echo(message, 0, Verbosity.Level1);
	}
	
	public static void echo(String message, Verbosity level) {
		echo(message, 0, level);
	}
	
	public static void echo(String message, int indentLevelChange) {
		echo(message, indentLevelChange, Verbosity.Level1);
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
	
	
	public static Verbosity verbosityLevel(Integer levelNum) {
		Verbosity level = Verbosity.Level0;
		if (levelNum > 5) levelNum = 5;
		if (levelNum < 0) levelNum = 0;
		String levelStr = "Level"+levelNum;
		level = Verbosity.valueOf(levelStr);
		return level;
	}
	
	public Boolean prompt_yes_or_no(String mess) {
		Pattern patt = Pattern.compile("^\\s*([yn])");
		boolean answer = false;
		while (true) {
			echo("\n"+mess+" (y/n)\n> ", false);	
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
