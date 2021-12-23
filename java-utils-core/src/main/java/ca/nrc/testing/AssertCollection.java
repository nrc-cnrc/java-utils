package ca.nrc.testing;

import ca.nrc.json.PrettyPrinter;
import org.junit.jupiter.api.Assertions;

import java.util.*;

public class AssertCollection {

	public static <T> void assertContains(String mess,
		T expItem, Collection<T> gotCollection, Boolean compareAsStrings) {
		if (compareAsStrings == null) {
			compareAsStrings = false;
		}
		boolean found = false;
		mess += "\nItem not found in collection:\n\n  " + expItem + "\n\n" +
				"Collection contained following items:\n";
		for (T gotItem : gotCollection) {
			boolean same = false;
			if (compareAsStrings) {
				same = (gotItem.toString().equals(expItem.toString()));
			} else {
				same = (gotItem.equals(expItem));
			}
			if (same) {
				found = true;
				break;
			}
			mess += "   " + gotItem.toString() + "\n";
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
		for (T elt : gotArray) {
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
		Set<T> expSet, Set<T> gotItems) {
		Set<T> expItemsNotFound = new HashSet<T>();
		for (T item : expSet) {
			if (!gotItems.contains(item)) {
				expItemsNotFound.add(item);
			}
		}
		if (!expItemsNotFound.isEmpty()) {
			mess +=
			"\nThe following expected items were not found in the actual collection:\n" +
				PrettyPrinter.print(expItemsNotFound) + "\n" +
				"Actual collection was:\n" + PrettyPrinter.print(gotItems);
			Assertions.fail(mess);
		}
	}


	public static <T> void assertContainsNoneOf(String mess,
		T[] unexpectedItemsArr, T[] gotItemsArr) {
		Set<T> unexpectedItems = new HashSet<T>();
		Collections.addAll(unexpectedItems, unexpectedItemsArr);
		Set<T> gotItems = new HashSet<T>();
		Collections.addAll(gotItems, gotItemsArr);
		assertContainsNoneOf(mess, unexpectedItems, gotItems);
	}

	public static <T> void assertContainsNoneOf(String mess,
 		T[] unexpectedItemsArr, Set<T> gotItems) {
		Set<T> unexpectedItems = new HashSet<T>();
		Collections.addAll(unexpectedItems, unexpectedItemsArr);
		assertContainsNoneOf(mess, unexpectedItems, gotItems);
	}

	public static <T> void assertContainsNoneOf(String mess,
		Set<T> unexpectedSet, Set<T> gotItems) {
		Set<T> unexpectedItemsFound = new HashSet<T>();
		for (T item : unexpectedSet) {
			if (gotItems.contains(item)) {
				unexpectedItemsFound.add(item);
			}
		}
		if (!unexpectedItemsFound.isEmpty()) {
			mess +=
			"\nThe following unexpected items were found in the actual collection:\n" +
				PrettyPrinter.print(unexpectedItemsFound) + "\n" +
				"Actual collection was:\n" + PrettyPrinter.print(gotItems);
			Assertions.fail(mess);
		}
	}
}
