package ca.nrc.data.harvesting;

import org.junit.Assert;
import org.junit.Test;


public class LanguageGuesserTest {

	////////////////////////
	// DOCUMENTATION TESTS
	////////////////////////

	@Test
	public void test__LanguageGuesser__Synopsis() throws Exception {
		// Use this class to figure out what language a piece 
		// of text is written in.
		//
		
		// English example
		String text = "Joy to the world";
		String lang = LanguageGuesser.detect(text);
		Assert.assertEquals("en", lang);		

		// French example
		text = "Joie pour tout le monde";
		lang = LanguageGuesser.detect(text);
		Assert.assertEquals("fr", lang);		
	
	}

}
