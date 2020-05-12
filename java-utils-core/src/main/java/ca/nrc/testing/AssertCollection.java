package ca.nrc.testing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;

public class AssertCollection {

	public static <T> void assertContains(String mess, 
			T expItem, Collection<T> gotCollection, Boolean compareAsStrings) {
		if (compareAsStrings == null) {
			compareAsStrings = false;
		}
		boolean found = false;
		mess += "\nItem not found in collection:\n\n  "+expItem+"\n\n"+
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
			mess += "   "+gotItem.toString()+"\n";
		}
		
		Assert.assertTrue(mess, found);
	}

	public static <T> void assertContains(String mess, 
			T expItem, T[] gotArray) {
		assertContains(mess, expItem, gotArray, false);
	}
	
	public static <T> void assertContains(String mess, 
			T expItem, T[] gotArray, Boolean compareAsStrings) {
		List<T> gotList = new ArrayList<T>();
		for (T elt: gotArray) {
			gotList.add(elt);
		}
		assertContains(mess, expItem, gotList, compareAsStrings);
	}
}
