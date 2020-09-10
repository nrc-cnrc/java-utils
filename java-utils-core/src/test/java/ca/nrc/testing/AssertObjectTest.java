package ca.nrc.testing;

import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class AssertObjectTest {

    @Test
    public void test__assertDeepEquals__Array2Iterator() throws Exception {
        Set<String> words = new HashSet<String>();
        String[] wordsArr = new String[] {"hello", "world"};
        Collections.addAll(words, wordsArr);
        Iterator<String> wordsIter = words.iterator();

        AssertObject.assertDeepEquals(
    "Array should not have had the same elements as the iterator",
            wordsArr, wordsIter);
    }
}
