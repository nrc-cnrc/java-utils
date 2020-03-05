package ca.nrc.string.diff;

import ca.nrc.datastructure.Pair;
import ca.nrc.string.SimpleTokenizer;
import ca.nrc.string.diff.TextualDiff.DiffersBy;
import ca.nrc.testing.AssertObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TextualDiffTest {
	
	private TextualDiff txtDiff;

	@Before
	public void setUp() {
		this.txtDiff = new TextualDiff();
	}

	///////////////////////////
	// DOCUMENTATION TESTS
	///////////////////////////
	
	@Test
	public void test__StringDiff__Synopsis() throws Exception {
		//
		// Use a StringDiff to compute the diff between
		// two strings of text.
		//
		String text1 = "Hello world.";
		String text2 = "Hello universe.";
		List<StringTransformation> diff = new TextualDiff().diffTransformations(text1, text2);
	}

	///////////////////////////
	// VERIFICATION TESTS
	///////////////////////////
	
	@SuppressWarnings("rawtypes")
	@Test
	public void test__diff__HappyPath() throws Exception {
		String[] tokens1 = TextualDiff.tokenize("Hello world. Take me to your leader.");
		String[] tokens2 = TextualDiff.tokenize("Greetings universe. Take me to your leader.");
				
		List<StringTransformation> gotTransf = new TextualDiff().diffTransformations(tokens1, tokens2);
		Object[][] expTransf = {
				new Object[] {
							0, new String[] {"Hello", " ", "world"}, 
							0, new String[] {"Greetings", " ", "universe"}
						}
		};
		
		assertTransformationsWere("Transformations were not as expected", expTransf, gotTransf);
	}
	
	@SuppressWarnings("rawtypes")
	@Test
	public void test__diff__SomeDiffInvolveSpacesOnly__ShouldNotBeIncludedAsADiff() throws Exception {
		String[] tokens1 = TextualDiff.tokenize("   Hello world. Take me to your leader.");
		String[] tokens2 = TextualDiff.tokenize("Greetings    universe. Take me to your leader.");
				
		List<StringTransformation> gotTransf = new TextualDiff().diffTransformations(tokens1, tokens2);
		Object[][] expTransf = {
				new Object[] {
							0, new String[] {"   ", "Hello", " ", "world"}, 
							0, new String[] {"Greetings", "    ", "universe"}
						}
		};
		
		assertTransformationsWere("Transformations were not as expected", expTransf, gotTransf);
	}

	
	@Test
	public void test__markupStrings__HappyPath() throws Exception {
		String text1 ="Hello world! Take me to your leader.";
		String text2 = "Hello universe. Take me immediatly to your boss.";
		Pair<String,String> gotMarkup = doMarkupStrings(text1, text2);
		Pair<String,String> expMarkup = Pair.of(
				"Hello <orig>world! </orig>Take me <orig></orig>to your <orig>leader</orig>.",
				"Hello <revised>universe. </revised>Take me <revised>immediatly </revised>to your <revised>boss</revised>."
		);
		AssertObject.assertDeepEquals("Marked up fragments were not as expected", 
				expMarkup, gotMarkup);
	}
	
	@Test
	public void test__markupStrings__TextWithLeadingSpaces() throws Exception {
		String doc1Content = "   Hello\n   world";	
		
		String doc2Content = "Hello world";
		Pair<String,String> gotMarkup = doMarkupStrings(doc1Content, doc2Content);
		Pair<String,String> expMarkup = Pair.of(
				"   Hello\n   world",
				"Hello world"
		);
		AssertObject.assertDeepEquals("Marked up fragments were not as expected", 
				expMarkup, gotMarkup);		
	}
	
	@Test
	public void test__markupStrings__FirstOrigWordModified() throws Exception {
		String text1 = "Hello world!";
		String text2 = "Greetings world!";
		Pair<String,String> gotMarkup = doMarkupStrings(text1, text2);
		Pair<String,String> expMarkup = Pair.of(
				"<orig>Hello</orig> world!",
				"<revised>Greetings</revised> world!"
		);
		AssertObject.assertDeepEquals("Marked up fragments were not as expected", 
				expMarkup, gotMarkup);
	}
	
	@Test
	public void test__markupStrings__LastOrigWordModified() throws Exception {
		String text1 = "Hello world";
		String text2 = "Hello universe";
		Pair<String,String> gotMarkup = doMarkupStrings(text1, text2);
		Pair<String,String> expMarkup = Pair.of(
				"Hello <orig>world</orig>",
				"Hello <revised>universe</revised>"
		);
		AssertObject.assertDeepEquals("Marked up fragments were not as expected", 
				expMarkup, gotMarkup);
	}
		
	@Test
	public void test__markupStrings__FirstOrigWordDeleted() throws Exception {
		String text1 = "Hello world!";
		String text2 = "world!";
		Pair<String,String> gotMarkup = doMarkupStrings(text1, text2);
		Pair<String,String> expMarkup = Pair.of(
				"<orig>Hello </orig>world!",
				"<revised></revised>world!"
		);
		AssertObject.assertDeepEquals("Marked up fragments were not as expected", 
				expMarkup, gotMarkup);
	}
	
	@Test
	public void test__markupStrings__LastOrigWordDeleted() throws Exception {
		String text1 = "Hello world";
		String text2 = "Hello";
		Pair<String,String> gotMarkup = doMarkupStrings(text1, text2);
		Pair<String,String> expMarkup = Pair.of(
				"Hello<orig> world</orig>",
				"Hello<revised></revised>"
		);
		AssertObject.assertDeepEquals("Marked up fragments were not as expected", 
				expMarkup, gotMarkup);
	}

	@Test
	public void test__markupStrings__FirstRevWordAdded() throws Exception {
		String text1 = "world";
		String text2 = "Hello world";
		Pair<String,String> gotMarkup = doMarkupStrings(text1, text2);
		Pair<String,String> expMarkup = Pair.of(
				"<orig></orig>world",
				"<revised>Hello </revised>world"
		);
		AssertObject.assertDeepEquals("Marked up fragments were not as expected", 
				expMarkup, gotMarkup);
	}
	
	@Test
	public void test__markupStrings__LastRevWordAdded() throws Exception {
		String text1 = "Hello";
		String text2 = "Hello world";
		Pair<String,String> gotMarkup = doMarkupStrings(text1, text2);
		Pair<String,String> expMarkup = Pair.of(
				"Hello<orig></orig>",
				"Hello<revised> world</revised>"
		);
		AssertObject.assertDeepEquals("Marked up fragments were not as expected", 
				expMarkup, gotMarkup);
	}

	@Test
	public void test__markupStrings__ConsecutiveWordsModified() throws Exception {
		String text1 = "... a professional engineer with knowledge of standard XYZ 1.2 ...";
		String text2 = "... a registered engineering professional with knowledge of standard XYZ 2.5 ...";
		Pair<String,String> gotMarkup = doMarkupStrings(text1, text2);
		Pair<String,String> expMarkup = Pair.of(
				"... a <orig>professional engineer </orig>with knowledge of standard XYZ <orig>1.2</orig> ...",
				"... a <revised>registered engineering professional </revised>with knowledge of standard XYZ <revised>2.5</revised> ..."
		);
		AssertObject.assertDeepEquals("Marked up fragments were not as expected", 
				expMarkup, gotMarkup);
	}
	
	
	@Test
	public void test__markupStrings__DifferOnlyInSpaces__NoDiffShouldBeMarkedUp() throws Exception {
		String text1 = "Hello world. Take me to your leader. Immediatly!";
		String text2 = "Hello    world.\n Take me to your leader.\tImmediatly!";
		Pair<String,String> gotMarkup = doMarkupStrings(text1, text2);
		Pair<String,String> expMarkup = Pair.of(
				"Hello world. Take me to your leader. Immediatly!",
				"Hello    world.\n Take me to your leader.\tImmediatly!"
		);
		AssertObject.assertDeepEquals("Marked up fragments were not as expected", 
				expMarkup, gotMarkup);
	}	

	@Test
	public void test__areIdentical__HappyPath() {
		String text1 = "Hello, world";
		String text2 = "Hello\nworld";
		Boolean gotAreIdentical = txtDiff.areIdentical(text1, text2);
		Assert.assertTrue(gotAreIdentical);		
	}
	
	@Test
	public void test__fragmentsDiffersBy__FragmentsExactlyIdentical() throws Exception {
		String text1 = "Hello world.";
		String text2 = "Hello world.";
		DiffersBy gotDiffersBy = txtDiff.differBy(text1, text2);
		
		Assert.assertEquals("differsBy value was not as expected.", 
				DiffersBy.NOTHING, gotDiffersBy);
	}
	
	@Test
	public void test__fragmentsDiffersBy__FragmentsDifferBySpacesOnly() throws Exception {
		String text1 = "Hello \t\nworld.";
		String text2 = "Hello world.";
		DiffersBy gotDiffersBy = txtDiff.differBy(text1, text2);
		
		Assert.assertEquals("differsBy value was not as expected.", 
				DiffersBy.SPACES, gotDiffersBy);
	}


	@Test
	public void test__fragmentsDiffersBy__FragmentsDifferBySpacesAndPuncutuation() throws Exception {
		String text1 = "Hello \t\nworld.";
		String text2 = "Hello, world.";
		DiffersBy gotDiffersBy = txtDiff.differBy(text1, text2);
		
		Assert.assertEquals("differsBy value was not as expected.", 
				DiffersBy.SPACES_AND_PUNCT, gotDiffersBy);
	}

	@Test
	public void test__fragmentsDiffersBy__FragmentsDifferByMoreThanSpacesAndPunctuation() throws Exception {
		String text1 = "Hello \t\n dear world.";
		String text2 = "Hello, world.";
		DiffersBy gotDiffersBy = txtDiff.differBy(text1, text2);
		
		Assert.assertEquals("differsBy value was not as expected.", 
				DiffersBy.OTHER, gotDiffersBy);
	}	
		
	@Test
	public void test__numTokensToTrim__LeadingAndTailingTrimmableTokens() throws Exception {
		String[] tokens = new String[] {" ", "hello", " ", "world", " "};
		Pair<Integer,Integer> gotNums = txtDiff.numTokensToTrim(tokens, SimpleTokenizer.NON_WORD);
		Pair<Integer,Integer> expNums = Pair.of(1, 1);
		AssertObject.assertDeepEquals("Number of leading and tailing blank tokens was not as expected", 
				expNums, gotNums);
	}

	@Test
	public void test__numTokensToTrim__NoLeadingNorTailingTrimmableTokens() throws Exception {
		String[] tokens = new String[] {"hello", " ", "world"};
		Pair<Integer,Integer> gotNums = txtDiff.numTokensToTrim(tokens, SimpleTokenizer.NON_WORD);
		Pair<Integer,Integer> expNums = Pair.of(0, 0);
		AssertObject.assertDeepEquals("Number of leading and tailing blank tokens was not as expected", 
				expNums, gotNums);
	}		

	@Test
	public void test__numTokensToTrim__WholeDiffIsMadeUpOfTokensToTrim() throws Exception {
		String[] tokens = new String[] {" ", " "};
		Pair<Integer,Integer> gotNums = txtDiff.numTokensToTrim(tokens, SimpleTokenizer.NON_WORD);
		Pair<Integer,Integer> expNums = Pair.of(2, 0);
		AssertObject.assertDeepEquals("Number of leading and tailing blank tokens was not as expected", 
				expNums, gotNums);
	}		
	
	@Test
	public void test__trimTransformation__EmptyTokensInOrigLeadingBlankInRev() throws Exception {
		String[] origTokens = new String[] {""};
		String[] revTokens = new String[] {" ", "immediatly"};
		
		StringTransformation transf = new StringTransformation(1, origTokens, 1, revTokens);

		StringTransformation gotTrimmedTransf = txtDiff.trimTransformation(transf, SimpleTokenizer.SPACES);
		StringTransformation expTrimmedTransf = 
				new StringTransformation(1, new String[] {""}, 1, new String[] {" ", "immediatly"});
		AssertObject.assertDeepEquals("Transformation not trimmed properly", 
				expTrimmedTransf, gotTrimmedTransf);
	}
	
	@Test
	public void test__collapseTransformations__TransfToBeKeptFollowsAllSpacesTransf() throws Exception {
		
		String[] tokens1 = new String[] {
						"hello",
						// This token will be subject of an all-spaces transformation
						"\n", 
						"world", ". ", 
						// This token will be subject of a NON all-spaces transformation
						"Hi"
					};
		String[] tokens2 = new String[] {
						"hello",
						// This token will be subject of an all-spaces transformation
						" ", 
						"world", ". ", 
						// This token will be subject of a NON all-spaces transformation
						"Greetings"
					};
		
		List<StringTransformation> origTransf = new ArrayList<StringTransformation>();
		{
			origTransf.add(new StringTransformation(1, new String[] {"\n"}, 1, new String[] {" "}));
			origTransf.add(new StringTransformation(4, new String[] {"Hi"}, 4, new String[] {"Greetings"}));
		}

		List<StringTransformation> gotCollapsed = 
					txtDiff.collapseTransformations(origTransf, tokens1, tokens2);
		List<StringTransformation> expCollapsed = new ArrayList<StringTransformation>();
		{
			expCollapsed.add(new StringTransformation(4, new String[] {"Hi"}, 4, new String[] {"Greetings"}));
		}
		
		AssertObject.assertDeepEquals("Transformations were not properly collapsed", 
				expCollapsed, gotCollapsed);
	}
	
	@Test
	public void test__differsBy__Spaces() {
		String text1 = "Hello    John Doe";
		String text2 = "Hello JohnDoe";
		TextualDiff tdiff = new TextualDiff();
		DiffersBy gotDiffType = tdiff.differBy(text1, text2);
		Assert.assertEquals("Diff type not as expeceted", DiffersBy.SPACES, gotDiffType);
	}
	
	@Test
	public void test__differsBy__Nothing() {
		String text1 = "Hello world";
		String text2 = "Hello world";
		TextualDiff tdiff = new TextualDiff();
		DiffersBy gotDiffType = tdiff.differBy(text1, text2);
		Assert.assertEquals("Diff type not as expeceted", DiffersBy.NOTHING, gotDiffType);
	}
	
	@Test
	public void test__differsBy__Punctuation() {
		String text1 = "Hello, world";
		String text2 = "Hello world";
		TextualDiff tdiff = new TextualDiff();
		DiffersBy gotDiffType = tdiff.differBy(text1, text2);
		Assert.assertEquals("Diff type not as expeceted", DiffersBy.SPACES_AND_PUNCT, gotDiffType);
	}

	@Test
	public void test__differsBy__OTHER() {
		String text1 = "Hello universe";
		String text2 = "Hello world";
		TextualDiff tdiff = new TextualDiff();
		DiffersBy gotDiffType = tdiff.differBy(text1, text2);
		Assert.assertEquals("Diff type not as expeceted", DiffersBy.OTHER, gotDiffType);
	}

	///////////////////////////
	// HELPER METHODS
	///////////////////////////

	public static void assertTransformationsWere(Object[][] expTransf, List<StringTransformation> gotTransf) throws IOException {
		assertTransformationsWere("", expTransf, gotTransf);
	}
	
	public static void assertTransformationsWere(String mess, Object[][] expTransf, List<StringTransformation> gotTransf) throws IOException {
		Object[] gotTransfPairs = new Object[gotTransf.size()];
		for (int ii=0; ii < gotTransfPairs.length; ii++) {
			StringTransformation delta_ii = gotTransf.get(ii);
			Object[] transf_ii = new Object[] {
					delta_ii.origTokenPos, delta_ii.origTokens,
					delta_ii.revisedTokenPos, delta_ii.revisedTokens
			};
			gotTransfPairs[ii] = transf_ii;
		}
		
		AssertObject.assertDeepEquals(mess, expTransf, gotTransfPairs);
	}

	protected Pair<String, String> doMarkupStrings(String text1, String text2) throws Exception {
		String[] tokens1 = TextualDiff.tokenize(text1);
		String[] tokens2 = TextualDiff.tokenize(text2);
		List<StringTransformation> transformations = txtDiff.diffTransformations(tokens1, tokens2);
		
		Pair<String,String> markedup = txtDiff.markupStrings(tokens1, tokens2, transformations);
		
		return markedup;
	}
}
