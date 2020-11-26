package ca.nrc.testing;

import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class AssertIteratorTest {

    @Test
    public void test__assertElementsEquals__InAnyOrder() throws Exception {
        String[] expElements = new String[] {"hello", "world"};
        String[] gotElementsArr = new String[] {"world", "hello"};
        List<String> gotElements = new ArrayList<String>();
        Collections.addAll(gotElements, gotElementsArr);
        Iterator<String> gotElementsIter = gotElements.iterator();
        boolean anyOrder = true;
        AssertIterator.assertElementsEquals("", expElements, gotElementsIter, anyOrder);
    }
}
