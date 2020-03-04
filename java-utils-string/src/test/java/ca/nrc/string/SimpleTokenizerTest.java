package ca.nrc.string;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import ca.nrc.datastructure.Pair;
import ca.nrc.testing.AssertObject;

public class SimpleTokenizerTest {
	
	///////////////////////////
	// DOCUMENTATION TESTS
	///////////////////////////
	
	@SuppressWarnings("unused")
	@Test
	public void test__SimpleTokenizer__Synopsis() {
		//
		// Use this class to tokenize a string into words and word separators, 
		// using a fairly naive approach.
		//
		
		String text = "Hello world. Take me to your leader.";
		
		// This only returns tokens that are considered words.
		//
		String[] wordsOnly = SimpleTokenizer.tokenize(text);
		
		// This returns all tokens, including word separators
		//
		String[] wordsAndSeparators = SimpleTokenizer.tokenize(text, true);

		// This returns all tokens in the form pairs, where the second 
		// element of the pair is a boolean indicating if the token was
		// a seaparator
		//
		List<Pair<String,Boolean>> tokensAsPair = SimpleTokenizer.tokenize_asPairs(text);
		for (Pair<String,Boolean> tok: tokensAsPair) {
			String tokText = tok.getFirst();
			Boolean isSeparator = tok.getSecond();
		}

	}
	
	///////////////////////////
	// VERIFICATION  TESTS
	///////////////////////////

	@Test
	public void test__tokenize__ExcludeSeparators() throws Exception {
		String text = "Hello world. Take me to your leader.";
		String[] gotTokens = SimpleTokenizer.tokenize(text);
		String[] expTokens = new String[] {
			"Hello", "world", "Take", "me", 
			"to", "your", "leader"
		};
		AssertObject.assertDeepEquals("Tokens not as expected", 
				expTokens, gotTokens);
	}
	
	
	@Test
	public void test__tokenize__IncludeSeparators() throws Exception {
		String text = "Hello world. Take me to your leader.";
		String[] gotTokens = SimpleTokenizer.tokenize(text, true);
		String[] expTokens = new String[] {
			"Hello", " ","world", ". ", "Take", " ", "me", 
			" ", "to", " ", "your", " ", "leader", "."	
		};
		AssertObject.assertDeepEquals("Tokens not as expected", 
				expTokens, gotTokens);
	}

	@Test
	public void test__tokenize_asPairs__HappyPath() throws Exception {
		String text = "Hello world.";
		List<Pair<String,Boolean>> gotTokens = SimpleTokenizer.tokenize_asPairs(text);
		Object[][] expTokens = new Object[][] {
			new Object[] {"Hello", false},
			new Object[] {" ", true},
			new Object[] {"world", false},
			new Object[] {".", true}
		};
		assertPairTokensEqual("Tokens not as expected", 
				expTokens, gotTokens);
	}
	
	@Test
	public void test__tokenStrings__SpacesInAllPossiblePositons() throws Exception {
		String text = 
				"  Hello    World   ";
		
		String[] gotTokens = SimpleTokenizer.tokenStrings(text);
		String[] expTokens = new String[] {"  ", "Hello", "    ", "World", "   "};

		AssertObject.assertDeepEquals("Tokens not as expected", expTokens, gotTokens);
	}
	
	///////////////////////////
	// TEST HELPERS
	///////////////////////////

	private void assertPairTokensEqual(String mess, 
			Object[][] expTokens, 
			List<Pair<String, Boolean>> gotTokensPairs) throws IOException {
		
		List<Object[]> gotTokens = new ArrayList<Object[]>();
		for (Pair<String,Boolean> tok: gotTokensPairs) {
			gotTokens.add(new Object[] {tok.getFirst(), tok.getSecond()});
		}
		
		AssertObject.assertDeepEquals(mess, expTokens, gotTokens);
	}
}
