package ca.nrc.testing;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.Consumer;

import ca.nrc.testing.RunOnCases.*;
import org.opentest4j.AssertionFailedError;

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

	@Test
	public void test__isSubsetOf__VariousCases() throws Exception {
		Case[] cases = new Case[]{
			new Case("strict-subset",
				new String[]{"a", "c"},
				new String[]{"a", "b", "c"},
				true),
			new Case("NOT-subset",
				new String[]{"a", "z"},
				new String[]{"a", "b", "c"},
				false),
			new Case("equal-sets",
				new String[]{"a", "b"},
				new String[]{"a", "b"},
				true),
			new Case("completely-disjoint",
				new String[]{"a", "b"},
				new String[]{"c", "d"},
				false),
			new Case("first-set-is-the-superset",
				new String[]{"a", "b", "c"},
				new String[]{"b"},
				false),
			new Case("first-set-null",
				null,
				new String[]{"a", "b", "c"},
				false),
			new Case("second-set-null",
				new String[]{"a", "b", "c"},
				null,
				false),
			new Case("both-sets-null",
				null,
				null,
				false),
		};

		Consumer<Case> runner = (aCase) -> {
			try {
				String[] set1Arr = (String[]) aCase.data[0];
				Set<String> set1 = null;
				if (set1Arr != null) {
					set1 = new HashSet<String>();
					Collections.addAll(set1, set1Arr);
				}

				String[] set2Arr = (String[]) aCase.data[1];
				Set<String> set2 = null;
				if (set2Arr != null) {
					set2 = new HashSet<String>();
					Collections.addAll(set2, set2Arr);
				}

				Boolean expSuccess = (Boolean) aCase.data[2];
				Class<AssertionFailedError> expException = null;
				if (!expSuccess) {
					expException = AssertionFailedError.class;
				}


				if (expException == null) {
					new AssertSet(set1, aCase.descr).isSubsetOf(set2Arr);
				} else {
					final Set<String> set1clone = set1;
					Assertions.assertThrows(expException, () -> {
						new AssertSet(set1clone, aCase.descr).isSubsetOf(set2Arr);
					});
				}

			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		};

		new RunOnCases(cases, runner)
//			.onlyCaseNums(6)
			.run();
	}

	@Test
	public void test__isSupersetOf__VariousCases() throws Exception {
		Case[] cases = new Case[]{
			new Case("strict-superset",
				new String[]{"a", "b", "c"},
				new String[]{"a", "c"},
				true),
			new Case("NOT-superset",
				new String[]{"a", "b", "c"},
				new String[]{"a", "z"},
				false),
			new Case("equal-sets",
				new String[]{"a", "b"},
				new String[]{"a", "b"},
				true),
			new Case("completely-disjoint",
				new String[]{"a", "b"},
				new String[]{"c", "d"},
				false),
			new Case("second-set-is-the-superset",
				new String[]{"b"},
				new String[]{"a", "b", "c"},
				false),
			new Case("first-set-null",
				null,
				new String[]{"a", "b", "c"},
				false),
			new Case("second-set-null",
				new String[]{"a", "b", "c"},
				null,
				false),
			new Case("both-sets-null",
				null,
				null,
				false),
		};

		Consumer<Case> runner = (aCase) -> {
			try {
				String[] set1Arr = (String[]) aCase.data[0];
				Set<String> set1 = null;
				if (set1Arr != null) {
					set1 = new HashSet<String>();
					Collections.addAll(set1, set1Arr);
				}

				String[] set2Arr = (String[]) aCase.data[1];
				Set<String> set2 = null;
				if (set2Arr != null) {
					set2 = new HashSet<String>();
					Collections.addAll(set2, set2Arr);
				}

				Boolean expSuccess = (Boolean) aCase.data[2];
				Class<AssertionFailedError> expException = null;
				if (!expSuccess) {
					expException = AssertionFailedError.class;
				}


				if (expException == null) {
					new AssertSet(set1, aCase.descr).isSupersetOf(set2Arr);
				} else {
					final Set<String> set1clone = set1;
					Assertions.assertThrows(expException, () -> {
						new AssertSet(set1clone, aCase.descr).isSupersetOf(set2Arr);
					});
				}

			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		};

		new RunOnCases(cases, runner)
//			.onlyCaseNums(6)
			.run();
	}


}
