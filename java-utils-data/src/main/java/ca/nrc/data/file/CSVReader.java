package ca.nrc.data.file;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CSVReader {

	public static List<List<String>> read(String csvFile) throws IOException {

		List<List<String>> lines = new ArrayList<List<String>>();

		String line = "";
		String cvsSplitBy = ",";

		BufferedReader br = new BufferedReader(new FileReader(csvFile));

		while ((line = br.readLine()) != null) {
			String[] fields = line.split(cvsSplitBy);
			List<String> currLine = new ArrayList<String>(Arrays.asList(fields));
			lines.add(currLine);
		}
		
		br.close();

		return lines;

	}

}
