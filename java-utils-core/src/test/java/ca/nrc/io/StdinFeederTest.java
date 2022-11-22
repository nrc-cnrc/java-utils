package ca.nrc.io;

import ca.nrc.io.StdinFeeder;
import ca.nrc.testing.AssertString;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.BufferedInputStream;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class StdinFeederTest {

	@AfterEach
	public void tearDown() throws Exception {
		// Make sure we restore STDIN to what it was before we may have tied to
		// a String
		StdinFeeder.stopFeeding();
	}

	/*********************************
	 * DOCUMENTATION TESTS
	 *********************************/

	@Test
	public void test__StdinFeeder__Synopsis() throws Exception {
		//
		// Use StdinFeeder to temporarily tie System.in to a string
		//
		StdinFeeder.feedString("Hello\nWorld\nGreetings universe");
		Scanner scanner = new Scanner(System.in);

		// This reads "Hello", which is the first line in the fed string
		String line = scanner.nextLine();

		// This reads "World", which is the second line in the fed string
		line = scanner.nextLine();

		// You can stop the feeding at any point, even if parts of the fed
		// string have not been read.
		// When you stop capturing Stdout, you get a string with everything
		// that was printed to it while you were capturing
		//
		StdinFeeder.stopFeeding();
	}

	/*********************************
	 * VERIFICATION TESTS
	 *********************************/

	@Test
	public void test__StdinFeeder__HappyPath() {
		StdinFeeder.feedString("Hello\nWorld");
		Scanner scanner = new Scanner(System.in);

		String gotLine = scanner.nextLine();
		AssertString.assertStringEquals("1st line not as expected",
			"Hello", gotLine);

		gotLine = scanner.nextLine();
		AssertString.assertStringEquals("2nd line not as expected",
			"World", gotLine);

		// Trying to read passed end of string should raise an exception
		Assertions.assertThrows(NoSuchElementException.class, () ->
			scanner.nextLine()
		);
	}

	@Test
	public void test__stopFeeding__WhenWeWereNotFeedingInTheFirstPlace() throws Exception {
		StdinFeeder.stopFeeding();
		Assertions.assertTrue(System.in != null,
			"stopFeeding set System.in to null!");

		Assertions.assertEquals(BufferedInputStream.class, System.in.getClass(),
			"stopFeeding set System.in to an instance of the wrong class!");
	}
}
