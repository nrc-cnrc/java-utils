package ca.nrc.string.diff;

import static org.junit.Assert.*;

import org.junit.Test;

import ca.nrc.datastructure.Pair;
import ca.nrc.testing.AssertObject;

public class DiffResultTest {

	///////////////////////
	// VERIFICATION TESTS
	///////////////////////

	@Test
	public void test__charsBefore__HappyPath() throws Exception {
		String[] chars1 = new String[] {"a", "b", "c"};
		String[] chars2 = new String[] {"a", "b", "1", "c"};
		DiffResult diff = new TextualDiff().diffResult(chars1, chars2);
		
		Pair<String,String> gotBefore = diff.strBeforeTransfNum(0);
		Pair<String,String> expBefore = Pair.of("ab", "ab");
		AssertObject.assertDeepEquals("Strings before first transformation was not as expected", 
				expBefore, gotBefore);
	}
}
