package ca.nrc.data.harvesting;

import java.io.IOException;
import java.util.List;

import com.optimaize.langdetect.LanguageDetector;
import com.optimaize.langdetect.LanguageDetectorBuilder;
import com.optimaize.langdetect.i18n.LdLocale;
import com.optimaize.langdetect.ngram.NgramExtractors;
import com.optimaize.langdetect.profiles.LanguageProfile;
import com.optimaize.langdetect.profiles.LanguageProfileReader;
import com.optimaize.langdetect.text.CommonTextObjectFactories;
import com.optimaize.langdetect.text.TextObject;
import com.optimaize.langdetect.text.TextObjectFactory;
import com.google.common.base.Optional;

public class LanguageGuesser {
	
	static LanguageDetector detector = null;
	private static LanguageDetector getDetector() throws IOException {
		if (detector == null) {
			List<LanguageProfile> languageProfiles = 
					new LanguageProfileReader().readAllBuiltIn();
	
			//build language detector:
			detector = LanguageDetectorBuilder.create(NgramExtractors.standard())
			        .withProfiles(languageProfiles)
			        .build();
		}
		
		return detector;
	}

	static TextObjectFactory textFactory = null;
	private static TextObjectFactory getTextFactory() {
		if (textFactory == null) {
			textFactory = 
					CommonTextObjectFactories.forDetectingOnLargeText();			
		}
		return textFactory;
	}
	
	/**
	 * Detect language that a text is written in.
	 */
	public static String detect(String text) throws IOException {
		TextObject textObject = getTextFactory().forText(text);
		Optional<LdLocale> lang = getDetector().detect(textObject);		
		
		String langCode = null;
		if (lang.isPresent()) {
			langCode = lang.get().getLanguage();
		}
		
		if (langCode == null) {
			langCode = checkForInuktut(text);
		}
		
		return langCode;
	}

	private static String checkForInuktut(String text) {
	
		double iuRatio;
		// Compute the ratio of Inuktut to non-Inuktut characters
		{
			int totalChars = 0;
			int iuChars = 0;
			for (int ii = 0; ii < text.length(); ii++){
			    char c = text.charAt(ii); 
			    if (Character.isWhitespace(c)) {
			    	continue;
			    }
			    totalChars++;
			    // Check if the character is in the Inuktitut syllabic 
			    // UTF range
			    {
				    int utfCode = (int)c;
				    if (utfCode >= 5120 && utfCode <= 5759) {
				    	iuChars++;
				    }
			    }
			}
			iuRatio = 1.0 * iuChars / totalChars;
		}
		
		String lang = null;
		if (iuRatio > 0.8) {
			lang = "iu";
		}

		return lang;
	}
}
