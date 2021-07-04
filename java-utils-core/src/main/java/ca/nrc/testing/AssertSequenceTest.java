package ca.nrc.testing;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

public class AssertSequenceTest {

	@Test
	public void test__startsWith__OrderSensitive__SeqStartsWithExpectedHeadInRightOrder__SUCCEEDS()
		throws Exception {
		String[] sequence = new String[] {"hello", "world", "hi"};
		new AssertSequence<String>(sequence)
			.startsWith("hello", "world");
	}

	@Test
	public void test__startsWith__OrderSensitive__SeqStartsWithExpectedHeadButDifferentOrder__FAILS()
		throws Exception {
		String[] sequence = new String[] {"world", "hello"};
		Assertions.assertThrows(AssertionFailedError.class, () -> {
			new AssertSequence<String>(sequence)
			.startsWith("hello", "world");
		});
	}


	@Test
	public void test__startsWith__OrderSensitive__SeqShorterThanExpHead() throws Exception {
		String[] sequence = new String[] {"hello"};

		Assertions.assertThrows(AssertionFailedError.class, () -> {
			new AssertSequence<String>(sequence)
				.startsWith("Hhello", "world");
		});
	}

	@Test
	public void test__startsWith__OrderSensitive__SeqHasWrongHead() throws Exception {
		String[] sequence = new String[] {"hello", "universe"};

		Assertions.assertThrows(AssertionFailedError.class, () -> {
			new AssertSequence<String>(sequence)
				.startsWith("hello", "world");
		});
	}

////////////////////

	@Test
	public void test__startsWith__AnyOrder__SeqStartsWithExpectedHeadInRightOrder__SUCCEEDS()
		throws Exception {
		String[] sequence = new String[] {"hello", "world", "hi"};
		new AssertSequence<String>(sequence)
			.startsWith(true, "hello", "world");
	}

	@Test
	public void test__startsWith__AnyOrder__SeqStartsWithExpectedHeadButDifferentOrder__SUCCEDS()
		throws Exception {
		String[] sequence = new String[] {"world", "hello"};
		new AssertSequence<String>(sequence)
			.startsWith(true, "hello", "world");
	}

	@Test
	public void test__startsWith__AnyOrder__SeqShorterThanExpHead__FAILS() throws Exception {
		String[] sequence = new String[] {"hello"};

		Assertions.assertThrows(AssertionFailedError.class, () -> {
			new AssertSequence<String>(sequence)
				.startsWith(true, "hello", "world");
		});
	}

	@Test
	public void test__startsWith__AnyOrder__SeqHasWrongHead__FAILS() throws Exception {
		String[] sequence = new String[] {"hello", "universe"};

		Assertions.assertThrows(AssertionFailedError.class, () -> {
			new AssertSequence<String>(sequence)
				.startsWith(true, "hello", "world");
		});
	}

}
