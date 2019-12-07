package ca.nrc.data.file;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectStreamException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import ca.nrc.datastructure.Pair;
import ca.nrc.debug.ExceptionHelpers;

public class ObjectStreamReader implements Closeable {
	
	public static enum OnError {LOG_ERROR, IMMEDIATE_EXCEPTION, EXCEPTION_AT_CLOSING};
	public OnError onError = OnError.EXCEPTION_AT_CLOSING;
	
	public boolean verbose = true;
	
	public boolean currentObjWasMalformed = true;
	
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
		bodyEndMarkerChoices.put("NEW_LINE", "^\\s*\\S+[\\s\\S]*$"); // Non-blank line
	}
	String regexBodyLast = bodyEndMarkerChoices.get("BLANK_LINE");
	
	// Non blank lines are considered to be part of the body
	// unless they are comment lines
	String regexBodyFirst = "^\\s*\\S+[\\s\\S]*$";	
	
	// Line that defines the class of objects being read from file
	Pattern regexClass = Pattern.compile("^\\s*class=(.+)$");
	
	List<String> errorMessages = new ArrayList<String>();

	public ObjectStreamReader(File file) throws FileNotFoundException {
		FileReader fileReader = new FileReader(file);
		initialize(fileReader);
	}

	public ObjectStreamReader(String streamContent)  {
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
	
	protected void finalize() throws Throwable {
		close();
	}

	@JsonIgnore
	public ObjectStreamReader setEndOfBodyMarker(String markerType) throws ObjectStreamReaderException {
		if (!bodyEndMarkerChoices.containsKey(markerType)) {
			throw new ObjectStreamReaderException("Unknown end of body marker: "+markerType);
		}
		regexBodyLast = bodyEndMarkerChoices.get(markerType);
		return this;
	}

	@JsonIgnore
	public ObjectStreamReader setObjectClass(Class type)  {
		currentObjClass = type;
		return this;
	}

	public Object readObject() throws IOException, ClassNotFoundException, ObjectStreamReaderException {		
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
				continue;
			} else if (lineType.equals("bodyFirstAndLast") && !currentLine.isEmpty()) {
				object = onObjectBodyFirstAndLast(currentLine);
				if (! currentObjWasMalformed) break;
			} else if (lineType.equals("objectBody")) {
				onObjectBodyMiddleLine(currentLine);
				continue;
			} else if (lineType.equals("objectBodyLast")) {
				object = onObjectBodyEnd(currentLine);
				if (! currentObjWasMalformed) break;
			} else if (lineType.equals("eof")) {
				if (insideOfBody) {
					object = onObjectBodyEnd("");
					break;
				} else {
					close(); // close the reader when you reach the end
					object = null;
					break;
				}
			} else if (lineType.equals("class")) {
				onClassLine(lineParameters);
				continue;
			} else if (lineType.equals("bodyLast")) {
				if (insideOfBody) {
					// bodyEnd line while processing body of an object means
					// we reached the end of the object's body.
					object = onObjectBodyEnd(currentLine);
					if (! currentObjWasMalformed) break;
				} else {
					// Skip blank lines before start of a class body
					continue;
				}
			}			
		}
		
		return object;
	}

	private void onBodyEndMarkerDefinition(Map<String, String> lineParameters) throws ObjectStreamReaderException {
		String markerType = lineParameters.get("markerType");
		if (markerType.equals("NEW_LINE") || markerType.equals("BLANK_LINE")) {
			regexBodyLast = bodyEndMarkerChoices.get(markerType);
		} else {
			error("Unknown bodyEndMarker: "+markerType);
		}
		
	}

	private Object onObjectBodyFirstAndLast(String line) throws JsonParseException, IOException, ObjectStreamReaderException {
		Logger tLogger = LogManager.getLogger("ca.nrc.json.ObjectStreamReader.onObjectBodyFirstAndLast");
		tLogger.trace("invoked");		
		onObjectBodyMiddleLine(line);
		Object obj = onObjectBodyEnd(line);
		return obj;
	}

	private Object onObjectBodyEnd(String currentLine) throws JsonParseException, IOException, ObjectStreamReaderException {
		Logger tLogger = LogManager.getLogger("ca.nrc.json.ObjectStreamReader.onObjectBodyEnd");
		tLogger.trace("invoked");
		currentObjWasMalformed = false;
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
			currentObjWasMalformed = true;
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

	private void onObjectBodyMiddleLine(String line) throws ObjectStreamReaderException {
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

	private void onClassLine(Map<String, String> parameters) throws ObjectStreamReaderException {
		String className = parameters.get("class");
		try {
			if (className.equals("Map")) {
				Map<String,Object> proto = new HashMap<String,Object>();
				currentObjClass = proto.getClass();
			} else {
				currentObjClass = Class.forName(className);
			}
		} catch (ClassNotFoundException exc) {
			error("No class found for object of type: "+className);
		}
		
	}

	private void errorMalformedJSON(String message, Exception exc) throws ObjectStreamReaderException {
		currentObjWasMalformed = true;
		error(message, exc);
	}
	
	private void error(String message) throws ObjectStreamReaderException {
		error(message, null);
	}

	private void error(String errMess, Exception exc) throws ObjectStreamReaderException  {
		errMess = "Error at line "+lineCount + ":\n   " + currentLine + "\n" + errMess;
		if (exc != null) {
			errMess += "\n\n" + exc.getMessage()+"\n"+ExceptionHelpers.printExceptionCauses(exc);
		}
		errorMessages.add(errMess);
		if (onError != OnError.IMMEDIATE_EXCEPTION) {
			System.out.println(errMess);
		} else  {
			throw new ObjectStreamReaderException(errMess);
		}
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
		if (onError == OnError.EXCEPTION_AT_CLOSING) {
			if (errorMessages.size() > 0) {
				System.out.println("Some errors were raised while reading JSON object stream.\nErrors were:\n+");
				for (String err: errorMessages) {
					System.out.println("\n"+err);
				}
				throw new IOException("Some errors were raised while reading JSON object stream.");
			}
		}
	}

	private ObjectMapper getMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
		mapper.configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);
		
		return mapper;
	}
}
