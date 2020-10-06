package ca.nrc.testing;

import ca.nrc.json.PrettyPrinter;
import org.junit.Assert;

import java.io.IOException;
import java.util.*;

public class AssertIterator {

    public static <T> void assertElementsEquals(String mess,
        T[] expElements, Iterator<T> gotIterator) throws IOException {
        assertElementsEquals(mess, expElements, gotIterator, null);
    }

    public static <T> void assertElementsEquals(String mess,
        T[] expElements, Iterator<T> gotIterator, Boolean anyOrder) throws IOException {
        if (anyOrder == null) {
            anyOrder = false;
        }

        Collection<T> expElementsColl = new ArrayList<T>();
        Collection<T> gotElementsColl = new ArrayList<T>();

        if (anyOrder) {
            expElementsColl = new HashSet<T>();
            gotElementsColl = new HashSet<T>();
        }

        for (T elt: expElements) {
            expElementsColl.add(elt);
        }
        gotIterator.forEachRemaining(gotElementsColl::add);
        AssertObject.assertDeepEquals(
                mess + "\nIterator did not contain the expected objects",
                expElementsColl, gotElementsColl);
    }

    public static <T> void assertContainsAll(String mess,
        T[] expElements, Iterator<T> gotIterator) {
        Set<T> gotElements = new HashSet<T>();
        gotIterator.forEachRemaining(gotElements::add);
        AssertCollection.assertContainsAll(
                mess+"\nIterator did not contain the expected objects",
                expElements, gotElements);
    }

    public static <T> void assertContainsNoneOf(String mess,
        T[] unexpected, Iterator<T> gotIterator) {
        Set<T> unexpectedItems = new HashSet<T>();
        Collections.addAll(unexpectedItems, unexpected);

        Set<T> gotItems = new HashSet<T>();
        gotIterator.forEachRemaining(gotItems::add);

        Set<T> unexpectedItemsFound = new HashSet<T>();
        for (T item : unexpected) {
            if (gotItems.contains(item)) {
                unexpectedItemsFound.add(item);
            }
        }
        if (!unexpectedItemsFound.isEmpty()) {
            mess +=
                    "\nThe following unexpected items were found in the actual collection:\n" +
                            PrettyPrinter.print(unexpectedItemsFound) + "\n" +
                            "Actual collection was:\n" + PrettyPrinter.print(gotItems);
            Assert.fail(mess);
        }
    }
}
