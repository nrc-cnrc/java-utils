package ca.nrc.string;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import ca.nrc.datastructure.Pair;
import ca.nrc.testing.AssertObject;
import ca.nrc.testing.AssertString;

public class StringUtilsTest {

	@Test
	public void test__splitWithDelimiters__HappyPath() throws Exception {
		String text = "hello, world.";
		String regexp = "(\\p{Punct}|\\s)+";
		List<Pair<String,Boolean>> gotPieces = StringUtils.splitWithDelimiters(regexp, text);
		List<Pair<String,Boolean>> expPieces = new ArrayList<Pair<String,Boolean>>();
		{
			expPieces.add(Pair.of("hello", false));
			expPieces.add(Pair.of(", ", true));
			expPieces.add(Pair.of("world", false));
			expPieces.add(Pair.of(".", true));
		}
		
		AssertObject.assertDeepEquals("String was not split properly", expPieces, gotPieces);
	}

	@Test
	public void test__tokenizeNaively__HappyPath() throws Exception {
		String text = "hello, world.";
		List<Pair<String,Boolean>> gotTokens = StringUtils.tokenizeNaively(text);
		List<Pair<String,Boolean>> expTokens = new ArrayList<Pair<String,Boolean>>();
		{
			expTokens.add(Pair.of("hello", false));
			expTokens.add(Pair.of(", ", true));
			expTokens.add(Pair.of("world", false));
			expTokens.add(Pair.of(".", true));
		}
		
		AssertObject.assertDeepEquals("String was not split properly", expTokens, gotTokens);
	}
	
	@Test
	public void test__join__Iterator() {
		List<String> strs = new ArrayList<String>();
		{
			strs.add("Hello"); strs.add("world");
		}
		String gotJoined = StringUtils.join(strs.iterator(), "|");
		String expJoined = "Hello|world";
		AssertString.assertStringEquals(expJoined, gotJoined);
	}
	@Test
	public void test__join__Array() {
		String[] chars = new String[] {"hello", "world"};
		String gotJoined = StringUtils.join(chars, "|");
		String expJoined = "hello|world";
		AssertString.assertStringEquals(expJoined, gotJoined);

	}
}
