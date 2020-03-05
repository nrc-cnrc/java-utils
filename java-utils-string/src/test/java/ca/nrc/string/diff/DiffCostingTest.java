package ca.nrc.string.diff;


import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import ca.nrc.string.SimpleTokenizer;

public class DiffCostingTest {

	String[] helloWorld = SimpleTokenizer.tokenize("Hello world.");
	String[] helloUniverse = SimpleTokenizer.tokenize("Hello universe.");
	String[] helloFriends = SimpleTokenizer.tokenize("Hello friends.");
	
	@Test
	public void test__DiffCosting__Synopsis() throws Exception {
		//
		// Say you have done a world level diff the string:
		//
		//   "Hello world"
		//
		// and the following two strings
		//
		//   "Hello universe"
		//   "Hello friends"
		//
		TextualDiff tDiff = new TextualDiff();
		DiffResult diffUniverse = tDiff.diffResult(helloWorld, helloUniverse);
		DiffResult diffFriends = tDiff.diffResult(helloWorld, helloFriends);
		
		//
		// You now want to use this diff to assess to what extent the "hello world" 
		// string is similar to the other other two, by assigning a "cost" to the two
		// diffs (where low cost means very similar).
		//
		// You can do this using a concrete subclass of the abstract class 
		// DiffCosting. The simplest costing  model is to compute the total 
		// number of tokens affected.
		//
		
		DiffCosting_Default naiveCosting = new DiffCosting_Default();
		double universeCost = naiveCosting.cost(diffUniverse);
		double friendsCost = naiveCosting.cost(diffFriends);
		
		// This simple costing method may work well in many cases, but 
		// it is somewhat brittle. For example in this case it assigns 
		// the same cost to both strings, eventhough "Hello universe" is 
		// semantically much closer to "Hello world" than "Hello friends"
		//
		Assert.assertEquals("", universeCost, friendsCost, 0.0);
		
		//
		// For that reason, you can use other, more sophisticated 
		// costing models.
		//
		// For example, this model assumes lower cost to substitution 
		// of words that are synonyms.
		//
		DiffCosting_SynSets synCosting = new DiffCosting_SynSets();
		synCosting.addSynSet(new String[] {"world", "universe"});
		universeCost = synCosting.cost(diffUniverse);
		friendsCost = synCosting.cost(diffFriends);
		Assert.assertNotEquals(universeCost, friendsCost);
	}

}
