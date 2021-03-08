package ca.nrc.ui.commandline;

import ca.nrc.testing.outputcapture.StdoutCapture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;
import static ca.nrc.ui.commandline.UserIO.Verbosity;

public class UserIOTest {

	UserIO userIO = null;

	@BeforeEach
	public void setUp() {
		userIO = new UserIO();
	}

	@AfterEach
	public void tearDown() {
		StdoutCapture.stopCapturing();
	}

	///////////////////////////////////
	// DOCUMENTATION TESTS
	///////////////////////////////////

	@Test
	public void test__UserIO__Synopsis() {
		// Use this class to interact with the user through the command line
		// terminal
		//
		UserIO userIO = new UserIO();

		// For example, print a line of text
		//
		userIO.echo("hello world");

		// If you don't want a newline at the end of the message
		userIO.echo("This...", false);
		userIO.echo("and this should appear on the same line");

		// A message may or may not be printed depending on.
		// A message willl be printed only if the message's level is lower or
		// equal to the UserIO's verbosity level.
		//
		// By default, a UserIO is configured at Level1, meaning that all message
		// with level <= Level1 will be printed.
		//
		// By default, a message is issued with Level0, but you can specify a
		// specific level in the echo() call.
		//
		// So, for example....
		//
		// This UserIO starts out with verbosity = Level1
		//
		userIO = new UserIO();

		// As a result, the following messages get printed because they
		// have a level < Level1
		//
		// No level specified, so defaults to 0
		userIO.echo("SOULD be printed");
		// Message explicitly issued at Level0 < Level1
		userIO.echo("SHOULD be printed", UserIO.Verbosity.Level0);
		// Message explicitly issued at Level1 <= Level1
		userIO.echo("SHOULD be printed", UserIO.Verbosity.Level1);
		// Message explicitly issued at Level2 > Level1
		userIO.echo("Should NO be printed", UserIO.Verbosity.Level2);


		// Here, we set the UserIO's verbosity at Level 2, which means messages
		// issued at level up to Level2 will be printed
		userIO.setVerbosity(UserIO.Verbosity.Level2);
		userIO.echo("SHOULD be printed", UserIO.Verbosity.Level2);
		userIO.echo("Should NOT be printed", UserIO.Verbosity.Level3);

		// By default, messages are issued with level 0
		userIO.echo("SHOULD be printed");

		// Messages can be indented to a current indentation level.
		// You can increase/decrease the current indentation level with echo(int).
		// Note: It's a good idea to use the style:
		//
		//  userIO.echo(+n);
		//  {
		//      ... do some stuff
		//  }
		//  userIO.echo(-n)
		//
		// To ensure that the echo(+n) and echo(-n) are "balanced".
		//
		userIO.echo(1);
		{
			userIO.echo("This should be indented by one level");
			userIO.echo(1);
			{
				userIO.echo("This should be indented by TWO levels");
			}
			userIO.echo(-1);
			userIO.echo("Back to one level");
		}
		userIO.echo(-1);
		userIO.echo("Back to no indentation.");


		// But you can specify the message's level
		userIO.echo("SHOULD also be printed", UserIO.Verbosity.Level0);
		userIO.echo("Should NOT be printed", UserIO.Verbosity.Level2);

		// You can prompt the user for a y/n answer
		// Note: Normally you can call prompt_yes_or_no() without having
		//   to resort to a lambda. But in the context of this test we
		//   have to do it that way so that the test will not block and
		//   wait for user inputs.
		//
		Supplier<Boolean> lambda = () -> {
			String mess = "Do you like chocolate?";
			boolean answer = new UserIO().prompt_yes_or_no(mess);
			return answer;
		};
		RunWithStdinInputs.run(lambda, "y\n");
	}

	////////////////////////////////
	// VERFICATION TESTS
	////////////////////////////////

	@Test
	public void test__echo__VerbosityLeftAtDefault() {
		AssertUserIO asserter = new AssertUserIO(userIO);

		String descr = "userIO left at default configuration; ";
		// By default, messages with lever <= 1 are printed

		asserter.messageIsPrinted(descr, null);
		asserter.messageIsPrinted(descr, Verbosity.Level0);
		asserter.messageIsPrinted(descr, Verbosity.Level1);

		//... but messages with level > 1 are not
		asserter.messageNotPrinted(descr, Verbosity.Level2);
		asserter.messageNotPrinted(descr, Verbosity.Level3);
	}

	@Test
	public void test__echo__VerbositySetAtLevel2() {
		AssertUserIO asserter = new AssertUserIO(userIO);

		userIO.setVerbosity(Verbosity.Level2);
		String descr = "With userIO at verbosity=Level2... ";

		// When we set the userIO's verbosity level to 2, then all
		// messages with level <= 2 get printed
		asserter.messageIsPrinted(descr, null);
		asserter.messageIsPrinted(descr, Verbosity.Level0);
		asserter.messageIsPrinted(descr, Verbosity.Level1);
		asserter.messageIsPrinted(descr, Verbosity.Level2);

		// But messages with level > 2 are not
		asserter.messageNotPrinted(descr, Verbosity.Level3);
		asserter.messageNotPrinted(descr, Verbosity.Level4);
	}

	@Test
	public void test__echo__IndentationLevels() {
		new AssertUserIO(userIO)
			.echoProduces("Level 0", "Level 0")

			// Increase to Level1
			.echoProduces("  Level 1",
				"Level 1", 1)

			// Increase to Level 2
			.echoProduces("    Level 2",
			"Level 2", 1)

			// Decrease back to Level 1
			.echoProduces("  Back to Level 1",
			"Back to Level 1", -1)

			// Decrease back to Level 0
			.echoProduces("Back to Level 0",
			"Back to Level 0", -1)

		;
	}


	@Test
	public void test__verbosityToInt() {
		Integer gotLevel = userIO.verbosityToInt(UserIO.Verbosity.Level0);
		Assertions.assertEquals(0, gotLevel, "Bad level for Level0");

		gotLevel = userIO.verbosityToInt(UserIO.Verbosity.Level1);
		Assertions.assertEquals(1, gotLevel, "Bad level for Level1");

		gotLevel = userIO.verbosityToInt(UserIO.Verbosity.Levelnull);
		Assertions.assertEquals(null, gotLevel, "Bad level for Levelnull");
	}
}
