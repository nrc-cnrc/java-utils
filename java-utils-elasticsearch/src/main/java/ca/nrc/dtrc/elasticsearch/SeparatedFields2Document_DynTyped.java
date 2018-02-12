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
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;


public class SeparatedFields2Document_DynTyped {
	
	char separator = ',';
	Integer maxLines = null;
	String[] headers = null;
	String idFieldName = null;
	String idGeneratorPrefix = null;
	int counter = 0;
	int badLines = 0;
	Integer verbosity = null;
	
	public SeparatedFields2Document_DynTyped setSeparator(char _sep) {
		this.separator = _sep;
		return this;
	}

	public SeparatedFields2Document_DynTyped setMaxLines(int _maxLines) {
		this.maxLines = _maxLines;
		return this;
	}

	public SeparatedFields2Document_DynTyped setVerbose() {
		this.verbosity = 1;
		return this;
	}

	public void convert(File inputFile, File outputFile) throws IOException, ElasticSearchException {
		convert(inputFile, outputFile, null);
	}
	
	public void convert(File inputFile, File outputFile, Integer maxJobs) throws IOException, ElasticSearchException {
		FileReader input = new FileReader(inputFile);
		FileWriter output = new FileWriter(outputFile);
		convert(input, output);
	}

	private static String escapeFieldName(String nameOrig) {
		
		String nameEscaped = nameOrig;
		
		nameEscaped = nameEscaped.replaceAll("[^a-zA-Z0-9]+", "_");
		nameEscaped = nameEscaped.replaceAll("^_+", "");
		nameEscaped = nameEscaped.replaceAll("_+$", "");

		return nameEscaped;
	}

	public void convert(Reader input, Writer output) throws IOException, ElasticSearchException {
		counter = 0;
		badLines = 0;
		@SuppressWarnings("deprecation")
		CSVReader reader = new CSVReader(input, this.separator);
		headers = reader.readNext();
		String idField = findIDFieldName(headers);
		String[] record = reader.readNext();
		
		
		ObjectMapper mapper = new ObjectMapper();
		
		String[] prevRecord = new String[]{};
		while (record != null)  {
			counter++;
			echo("Converting record number: "+counter, 0);
			
			if (record.length != headers.length) {
				System.out.println(
					  "Line "+counter+" has wrong number of fields!\n"
					+ "  Num fields: "+record.length+"\n"
					+ "  Fields were:\n---"+String.join("\n---\n", record)
					+ "\nPrevious line was had fields:\n---"+String.join("\n---\n", prevRecord)
					);
				badLines++;
			} else {		
				Map<String,Object> fields = new HashMap<String,Object>();
				for (int ii=0; ii < record.length; ii++) {
					String escFieldName = escapeFieldName(headers[ii]);
					String fieldValue = record[ii];
					if (fieldValue == null) fieldValue = "";
					fields.put(escFieldName, fieldValue);
				}
				
				if (idGeneratorPrefix != null) {
					// We need to generate the ID, instead of getting it
					// from the list of fields
					fields.put(idGeneratorPrefix, idGeneratorPrefix+"_"+counter);
				}
				
				Document_DynTyped doc = documentForLine(idField, fields);
				String json = mapper.writeValueAsString(doc);
				output.write(json+"\n");
			}
			prevRecord = record.clone();
			record = reader.readNext();	
			if (maxLines != null && maxLines < counter) break;
		}
		reader.close();
		
		System.out.println("Found "+badLines+" bad lines out of "+counter);
	}


	private void echo(String message, int level) {
		if (verbosity != null && verbosity > level) {
			System.out.println(message);
		}
		
	}

	public Document_DynTyped documentForLine(String idField, Map<String,Object> fields) throws ElasticSearchException {
		Document_DynTyped doc = new Document_DynTyped(idField, fields);
		
		return doc;
	}

	public String findIDFieldName(String[] headers) throws ElasticSearchException {
		
		if (idFieldName != null && idGeneratorPrefix != null) {
			throw new ElasticSearchException("You cannot set 'idFieldName' and 'idGenerator' at the same time");
		}
		String idField = null;
		if (idGeneratorPrefix != null) {
			idField = idGeneratorPrefix;
		} else {
			idField = this.idFieldName;
			if (idField != null) {
				boolean found = false;
				for (int ii=0; ii < headers.length; ii++) {
					if (headers[ii].equals(idField)) {
						found = true;
						break;
					}
				}
				if (!found) {
					throw new ElasticSearchException("ID field name: "+idField+" was not found in the list of headers.\nHeaders were: "+String.join(", ", headers));
				}
			} else {
				idField = headers[0];
			}
		}
		
		idField = escapeFieldName(idField);
		return idField;
	}

	public SeparatedFields2Document_DynTyped setIDFieldName(String _idFieldName) {
		this.idFieldName = _idFieldName;
		return this;
	}

	public SeparatedFields2Document_DynTyped setIDGenerator(String prefix) {
		this.idGeneratorPrefix = prefix;
		return this;
	}

}
