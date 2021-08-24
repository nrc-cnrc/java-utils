package ca.nrc.testing;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;

public class AssertSetTest {

	@Test
	public void test__isSubsetOf__GotIsASubset__RaisesNoFailure() {
		Set<String> expSuperset = new HashSet<String>();
		Collections.addAll(expSuperset,
			new String[] {"hello", "greetings", "salutations"});

		Set<String> gotSubset = new HashSet<String>();
		Collections.addAll(gotSubset,
			new String[] {"hello", "salutations"});

		AssertSet.isSubsetOf("", expSuperset, gotSubset);
	}

	@Test
	public void test__isSubsetOf__GotNOTaSubset__RaisesFailure() {
		Set<String> expSuperset = new HashSet<String>();
		Collections.addAll(expSuperset,
		new String[] {"hello", "greetings", "salutations"});

		Set<String> gotSubset = new HashSet<String>();
		Collections.addAll(gotSubset,
			new String[] {"bonjour", "hello", "greetings", "salutations"});

		Assertions.assertThrows(AssertionError.class, () -> {
			AssertSet.isSubsetOf("", expSuperset, gotSubset);
		});
	}

	@Test
	public void test__isSubsetOf__GotIsNull__RaisesFailure() {
		Set<String> expSuperset = new HashSet<String>();
		Collections.addAll(expSuperset,
			new String[] {"hello", "greetings", "salutations"});

		Set<String> gotSubset = null;

		Assertions.assertThrows(AssertionError.class, () -> {
			AssertSet.isSubsetOf("", expSuperset, gotSubset);
		});
	}

	@Test
	public void test__isSubsetOf__ExpSupersetIsNull__RaisesFailure() {
		Set<String> expSuperset = null;

		Set<String> gotSubset = new HashSet<String>();
		Collections.addAll(gotSubset,
			new String[] {"hello"});

		Assertions.assertThrows(AssertionError.class, () -> {
			AssertSet.isSubsetOf("", expSuperset, gotSubset);
		});
	}


	@Test
	public void test__isSubsetOf__GotEmptyWith__MayRaisesFailure() {
		Set<String> expSuperset = new HashSet<String>();
		Collections.addAll(expSuperset,
			new String[] {"hello", "greetings", "salutations"});

		Set<String> gotSubset = new HashSet<String>();


		Assertions.assertThrows(AssertionError.class, () -> {
			// By default, isSubseOf requires the subset to be
			// non-empty...
			AssertSet.isSubsetOf("", expSuperset, gotSubset);
		});

		// Empty sets can be allowed by passing emptySetIsOK=true
		// argument
		AssertSet.isSubsetOf("", expSuperset, gotSubset, true);
	}

	@Test
	public void test__isSubsetOf__GotSetIsKeySet() {
		// The inner class HashMap.KeySet cannot be cloned with Cloner class,
		// and the latter is used by AssertSet.isSubsetOf
		//
		Set<String> expSuperset = new HashSet<String>();
		Collections.addAll(expSuperset,
			new String[] {"hello", "greetings", "salutations"});

		Map<String,String> map = new HashMap<String,String>();
		map.put("hello", "world");
		Set<String> gotSubset = map.keySet();

		AssertSet.isSubsetOf("", expSuperset, gotSubset);
	}

}
