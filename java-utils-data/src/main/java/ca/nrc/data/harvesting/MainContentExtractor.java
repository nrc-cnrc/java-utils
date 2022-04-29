package ca.nrc.data.harvesting;

import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.extractors.ArticleExtractor;
import de.l3s.boilerpipe.extractors.DefaultExtractor;
import de.l3s.boilerpipe.extractors.KeepEverythingExtractor;
import de.l3s.boilerpipe.extractors.LargestContentExtractor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MainContentExtractor {

	/** Type of BoilerPipe extractor to use */
	public static enum ExtractionType {ARTICLE, DEFAULT, LARGEST_CONTENT, KEEP_EVERYTHING}

	public String extract(String html) throws PageHarvesterException {
		return extract(html, ExtractionType.DEFAULT);
	}

	public String extract(String html, String extractorTypeStr) throws PageHarvesterException {
		ExtractionType extractorType = null;
		if (extractorTypeStr != null) {
			extractorType = ExtractionType.valueOf(extractorTypeStr);
		}
		return extract(html, extractorType);
	}

	public String extract(String html, ExtractionType extractorType) throws PageHarvesterException {

		if (extractorType == null) {
			extractorType = ExtractionType.DEFAULT;
		}
		String mainText = null;

		if (html != null) {
			try {
				if (extractorType == ExtractionType.ARTICLE) {
					mainText = ArticleExtractor.INSTANCE.getText(html);
				} else if (extractorType == ExtractionType.DEFAULT) {
					mainText = DefaultExtractor.INSTANCE.getText(html);
				} else if (extractorType == ExtractionType.LARGEST_CONTENT) {
					mainText = LargestContentExtractor.INSTANCE.getText(html);
				} else if (extractorType == ExtractionType.KEEP_EVERYTHING) {
					mainText = KeepEverythingExtractor.INSTANCE.getText(html);
				}
			} catch (BoilerpipeProcessingException e) {
				throw new PageHarvesterException(e, "Failed to get the main content of page");
			}
		}

		return mainText;
	}

	public static void main(String[] args) throws Exception {
//		Path file = Paths.get("/Users/desilets/Desktop/giro.html");
//		Path file = Paths.get("/Users/desilets/Desktop/purdue.html");
		Path file = Paths.get("/Users/desilets/Desktop/mettech.html");


		String html = new String ( Files.readAllBytes(file) );
		String text = new MainContentExtractor().extract(html, ExtractionType.ARTICLE);
		System.out.println(text);
	}

}
