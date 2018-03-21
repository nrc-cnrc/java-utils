package ca.nrc.data.file;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import ca.nrc.datastructure.Pair;

public class ObjectStreamReader implements Closeable {
	
	public boolean verbose = true;
	
	BufferedReader buffReader = null;
	public int lineCount = 1;
	public int startLine = 1;
	public int endLine = 1;
	public String currentLine = null;
	
	private Class currentObjClass = Object.class;
	private boolean insideOfBody = false;
	private StringBuilder objectBodyBuilder = null;
	
	// Comment lines
	String regexComment = "^\\s*//[\\s\\S]*$";
	
	// Line to define the end of body marker
	Pattern regexEndOfBodyMarker = Pattern.compile("^\\s*bodyEndMarker=(NEW_LINE|BLANK_LINE)\\s*$");
	
	// Blank lines signal end of current body
	Map<String,String> bodyEndMarkerChoices = new HashMap<String,String>();
	{
		bodyEndMarkerChoices.put("BLANK_LINE", "^\\s*$");
		bodyEndMarkerChoices.put("NEW_LINE", "^[\\s\\S]*$");
	}
	String regexBodyLast = bodyEndMarkerChoices.get("BLANK_LINE");
	
	// Non blank lines are considered to be part of the body
	// unless they are comment lines
	String regexBodyFirst = "^\\s*\\S+[\\s\\S]*$";	
	
	// Line that defines the class of objects being read from file
	Pattern regexClass = Pattern.compile("^\\s*class=(.+)$");

	public ObjectStreamReader(File file) throws FileNotFoundException {
		FileReader fileReader = new FileReader(file);
		initialize(fileReader);
	}

	public ObjectStreamReader(String streamContent) throws FileNotFoundException {
		StringReader strReader = new StringReader(streamContent);
		initialize(strReader);
	}
	
	public ObjectStreamReader(Reader _reader) {
		initialize(_reader);
	}
	
	public ObjectStreamReader(InputStream _stream) {
		initialize(new InputStreamReader(_stream));
	}


	private void initialize(Reader _reader) {
		this.buffReader = new BufferedReader(_reader);
	}

	public Object readObject() throws IOException, ClassNotFoundException {		
		Logger tLogger = LogManager.getLogger("ca.nrc.json.ObjectStreamReader.readObject");
		Object object = null;
		int startLine = lineCount;
		
		objectBodyBuilder = new StringBuilder();
		insideOfBody = false;
		while (true) {
			if (objectBodyBuilder != null && tLogger.isTraceEnabled()) tLogger.trace("body so far:\n"+objectBodyBuilder.toString());
			currentLine = readLine();
			Pair<String,Map<String,String>> lineInfo = parseLineInfo(currentLine);
			String lineType = lineInfo.getFirst();
			tLogger.trace("["+lineType+"]: "+currentLine);
			Map<String,String> lineParameters = lineInfo.getSecond();
			if (lineType.equals("comment")) {
				// Skip comment lines
				continue;
			} else if (lineType.equals("bodyEndMarker")) {
				onBodyEndMarkerDefinition(lineParameters);
			} else if (lineType.equals("bodyFirstAndLast") && !currentLine.isEmpty()) {
				return onObjectBodyFirstAndLast(currentLine);
			} else if (lineType.equals("objectBody")) {
				onObjectBodyMiddleLine(currentLine);
			} else if (lineType.equals("objectBodyLast")) {
				return onObjectBodyEnd(currentLine);
			} else if (lineType.equals("eof")) {
				if (insideOfBody) {
					return onObjectBodyEnd("");
				} else {
					return null;
				}
			} else if (lineType.equals("class")) {
				onClassLine(lineParameters);
			} else if (lineType.equals("bodyLast")) {
				if (insideOfBody) {
					// bodyEnd line while processing body of an object means
					// we reached the end of the object's body.
					return onObjectBodyEnd(currentLine);
				} else {
					// Skip blank lines before start of a class body
					continue;
				}
			}			
		}
	}

	private void onBodyEndMarkerDefinition(Map<String, String> lineParameters) {
		String markerType = lineParameters.get("markerType");
		if (markerType.equals("NEW_LINE") || markerType.equals("BLANK_LINE")) {
			regexBodyLast = bodyEndMarkerChoices.get(markerType);
		} else {
			error("Unknown bodyEndMarker: "+markerType);
		}
		
	}

	private Object onObjectBodyFirstAndLast(String line) throws JsonParseException, IOException {
		Logger tLogger = LogManager.getLogger("ca.nrc.json.ObjectStreamReader.onObjectBodyFirstAndLast");
		tLogger.trace("invoked");		
		onObjectBodyMiddleLine(line);
		Object obj = onObjectBodyEnd(line);
		return obj;
	}

	private Object onObjectBodyEnd(String currentLine) throws JsonParseException, IOException {
		Logger tLogger = LogManager.getLogger("ca.nrc.json.ObjectStreamReader.onObjectBodyEnd");
		tLogger.trace("invoked");
		objectBodyBuilder.append(currentLine);
		
		Object object = null;
		
		String badJsonMess =
				"Error in object between lines: "+startLine+" and "+endLine;
				
		String objectJson = objectBodyBuilder.toString();
		objectBodyBuilder = new StringBuilder();
		if (objectJson == null || objectJson.matches("^\\s*$")) {
			return null;
		}
		
		try {
			object = getMapper().readValue(objectJson, currentObjClass);
		} catch (Exception exc) {
			String mess = badJsonMess+"\nCould not map object to an instance of "+currentObjClass;
			Exception excToInclude = null;
			if (verbose) {
				mess += "\nBody of object was:\n"+objectJson;
				excToInclude = exc;
			}
			error(mess, excToInclude);
		}
		
				
		return object;
		
	}

	private void onObjectBodyMiddleLine(String line) {
		Logger tLogger = LogManager.getLogger("ca.nrc.json.ObjectStreamReader.onObjectBodyMiddleLine");
		tLogger.trace("invoked");
		if (currentObjClass == null) {
			error("No 'class=' line found for this object");
		}
		if (!insideOfBody) {
			startLine = lineCount;
			insideOfBody = true;				
		}
		objectBodyBuilder.append(line+"\n");
	}

	private void onClassLine(Map<String, String> parameters) {
		String className = parameters.get("class");
		try {
			currentObjClass = Class.forName(className);
		} catch (ClassNotFoundException exc) {
			error("No class found for object of type: "+className);
		}
		
	}

	private void error(String message) {
		error(message, null);
	}

	private void error(String message, Exception exc) {
		message = "Error at line "+lineCount + ":\n   " + currentLine + "\n" + message;
		if (exc != null) {
			message += "\n\n" + exc.getMessage();
		}
		throw new InputMismatchException(message);
	}

	private Pair<String, Map<String, String>> parseLineInfo(String line) {
		
		Pair<String, Map<String, String>> lineInfo = Pair.of(null, new HashMap<String, String>());
		Map<String,String> parameters = new HashMap<String,String>();
		lineInfo.setSecond(parameters);
		
		if (line == null) {
			lineInfo.setFirst("eof");
		} else if (line.matches(regexComment)) {
			lineInfo.setFirst("comment");
		} else if (regexEndOfBodyMarker.matcher(line).matches()) {
			lineInfo.setFirst("bodyEndMarker");			
			Matcher matcher = regexEndOfBodyMarker.matcher(line);
			matcher.matches();
			parameters.put("markerType", matcher.group(1));
		} else if (regexClass.matcher(line).matches()) {
			lineInfo.setFirst("class");
			Matcher matcher = regexClass.matcher(line);
			matcher.matches();
			parameters.put("class", matcher.group(1));
		} else if (line.matches(regexBodyLast)) {
			if (!insideOfBody) {
				lineInfo.setFirst("bodyFirstAndLast");
			} else {
				lineInfo.setFirst("bodyLast");
			}
		} else if (line.matches(regexBodyLast)) {
			if (insideOfBody) {
				lineInfo.setFirst("objectBodyLast");
			} else {
				// This blank line is outside of an object body
				lineInfo.setFirst("blank");
			}
		} else {
			lineInfo.setFirst("objectBody");
		}
		
		return lineInfo;
	}

	private String readLine() throws IOException {
		String line = buffReader.readLine();
		lineCount++;
		endLine = lineCount;
		
		return line;
	}

	public void close() throws IOException {
		if (buffReader != null) buffReader.close();
		
	}

	private ObjectMapper getMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
		mapper.configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);
		
		return mapper;
	}
}
