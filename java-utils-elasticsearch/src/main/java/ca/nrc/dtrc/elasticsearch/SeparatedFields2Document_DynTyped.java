package ca.nrc.dtrc.elasticsearch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;

public class SeparatedFields2Document_DynTyped {
	
	String separator = ",";
	Integer maxLines = null;
	List<String> headers = null;
	
	public SeparatedFields2Document_DynTyped setSeparator(String _sep) {
		return this;
	}

	public SeparatedFields2Document_DynTyped setMaxLines(int _maxLines) {
		this.maxLines = _maxLines;
		return this;
	}

	private void convert(File inputFile, File outputFile, Integer maxJobs) throws IOException {
		FileReader input = new FileReader(inputFile);
		FileWriter output = new FileWriter(outputFile);
		convert(input, output);
	}

	private static Map<String, Object> readJob(BufferedReader reader, String[] fieldNames) throws IOException {
		Map<String,Object> jobFields = null;
		String jobLine = reader.readLine();
		if (jobLine != null) {
			String[] fieldValues = jobLine.split("\\t");
			
			jobFields = new HashMap<String,Object>();
			jobFields.put("Document_DynTyped.idFieldName", fieldNames[0]);
			for (int ii=0; ii < fieldValues.length; ii++) {
				jobFields.put(fieldNames[ii], fieldValues[ii]);
			}
		}		
		return jobFields;
	}

	private static String[] parseFieldNames(BufferedReader reader) throws IOException {
		String headerLine = reader.readLine();
		String[] headers = headerLine.split("\\t");
		String[] fieldNames = new String[headers.length];
		for (int ii=0; ii < headers.length; ii++) {
			fieldNames[ii] = escapeFieldName(headers[ii]);
		}
		
		return fieldNames;
	}

	private static String escapeFieldName(String nameOrig) {
		
		String nameEscaped = nameOrig;
		
		nameEscaped = nameEscaped.replaceAll("[^a-zA-Z0-9]+", "_");
		nameEscaped = nameEscaped.replaceAll("^_+", "");
		nameEscaped = nameEscaped.replaceAll("_+$", "");

		return nameEscaped;
	}

	private static void usage(String errMess) {
		
		String message = "";
		if (errMess != null) {
			errMess += "** ERROR: "+errMess+"\n";
		}
		message += 
			  "Usage: NRCNeo4j2ElasticSearchBulk inputFile maxJobs?\n"
			+ "\n"
			+ "  Take a list of Indeed jobs from the NRC neo4j database,\n"
			+ "  and convert it to an ElasticSearch _bulk script. The _bulk\n"
			+ "  script is output to stdout."
			+ ""
			+ "ARGUMENTS"
			+ "  "
			+ "  inputFile: path of the neo4j dump file."
			+ ""
			+ "  maxJobs: optional argument specifying the maximum number of "
			+ "    jobs to read from the file. Useful when debugging, to avoid"
			+ "    reading through the whole file."
			+ ""
			;
				
		System.out.println(message);
		System.exit(0);
	}

	public void convert(Reader input, Writer output) throws IOException {
		convert(input, output, ",");
		
	}

	public void convert(Reader input, Writer output, String separator) throws IOException {
		CSVReader reader = new CSVReader(input);
		String[] headers = reader.readNext();
		String idHeader = headers[0];
		String[] record = reader.readNext();
		while (record != null)  {
			record = reader.readNext();
		}
	}

}
