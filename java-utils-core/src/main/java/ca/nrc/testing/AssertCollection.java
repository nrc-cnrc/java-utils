package ca.nrc.testing;

import java.util.Collection;

import org.junit.Assert;

public class AssertCollection {

	public static <T> void assertContains(String mess, 
			T expItem, Collection<T> gotCollection, Boolean compareAsStrings) {
		if (compareAsStrings == null) {
			compareAsStrings = false;
		}
		boolean found = false;
		mess += "\nItem "+expItem+" not found in collection.\n"+
				"Collection contained following items:\n";
		for (T gotItem: gotCollection) {
			boolean same = false;
			if (compareAsStrings) {
				same = (gotCollection.toString().equals(expItem.toString()));
			} else {
				same = (gotCollection.equals(expItem));
			}
			if (same) {
				found = true;
				break;
			}
			mess += "   "+gotItem+"\n";
		}
		
		Assert.assertTrue(mess, found);
	}
}
