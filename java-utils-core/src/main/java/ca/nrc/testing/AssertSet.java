package ca.nrc.testing;

import ca.nrc.datastructure.Cloner;
import ca.nrc.json.PrettyPrinter;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.*;

public class AssertSet {

    public static <T> void assertEquals(String mess, T[] expElts, T[] gotElts) throws IOException {
        Set<T> expSet = new HashSet<T>();
        Collections.addAll(expSet, expElts);
        Set<T> gotSet = new HashSet<T>();
        Collections.addAll(gotSet, gotElts);
        AssertObject.assertDeepEquals(mess, expSet, gotSet);
    }

    public static <T> void assertEquals(String mess, T[] expElts, Set<T> gotSet) throws IOException {
        Set<T> expSet = new HashSet<T>();
        Collections.addAll(expSet, expElts);
        AssertObject.assertDeepEquals(mess, expSet, gotSet);
    }

    public static <T> void assertEquals(String mess, Set<T> expSet, Set<T> gotSet) throws IOException {
        AssertObject.assertDeepEquals(mess, expSet, gotSet);
    }

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

        Assertions.assertTrue(found, mess);
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

    public static <T> void assertContainsAll(String mess,
                                             T[] expItemsArr, T[] gotItemsArr) {
        Set<T> expItems = new HashSet<T>();
        Collections.addAll(expItems, expItemsArr);
        Set<T> gotItems = new HashSet<T>();
        Collections.addAll(gotItems, gotItemsArr);
        assertContainsAll(mess, expItems, gotItems);
    }

    public static <T> void assertContainsAll(String mess,
                                             T[] expItemsArr, Set<T> gotItems) {
        Set<T> expItems = new HashSet<T>();
        Collections.addAll(expItems, expItemsArr);
        assertContainsAll(mess, expItems, gotItems);
    }

    public static <T> void assertContainsAll(String mess,
                                             Set<T> expItems, Set<T> gotItems) {
        Set<T> itemsNotFound = new HashSet<T>();
        for (T item: expItems) {
            if (! gotItems.contains(item)) {
                itemsNotFound.add(item);
            }
        }
        if (!itemsNotFound.isEmpty()) {
            mess +=
                    "\nThe following expected items were not contained in the actual collection:\n"+
                            PrettyPrinter.print(itemsNotFound)+"\n"+
                            "Actual collection was:\n"+PrettyPrinter.print(gotItems);
            Assertions.fail(mess);
        }
    }

	public static <T> void isSubsetOf(String mess,
 		Set<T> expSuperset, Set<T> gotSet) {
		isSubsetOf(mess, expSuperset, gotSet, (Boolean)null);
	}


    public static <T> void isSubsetOf(String mess,
		Set<T> expSuperset, Set<T> gotSet, Boolean emptySetIsOK) {
		if (emptySetIsOK == null) {
			emptySetIsOK = false;
		}
		if (gotSet == null) {
			Assertions.fail(mess+"\nSet should not have been null");
		}
		if (expSuperset == null) {
			Assertions.fail(mess+"\nExpected superset should not be null");
		}
		if (!emptySetIsOK && gotSet.size() == 0 && expSuperset.size() > 0) {
			Assertions.fail(mess+"\nSet should not have been empty");
		}
		assertContainsAll(
			mess+"\nThe actual set was not a subset of the expected superset",
			gotSet, expSuperset
		);
	 }
}
