package ca.nrc.ui.commandline;

import static org.junit.Assert.*;

import org.junit.Test;

public class SubCommandTest {

	@Test
	public void test_verbosityToInt() {
		Integer gotInt = SubCommand.verbosityToInt(SubCommand.Verbosity.Level2);
		assertEquals(new Integer(2), gotInt);
		
		gotInt = SubCommand.verbosityToInt(SubCommand.Verbosity.Levelnull);
		assertEquals(null, gotInt);
		
	}

}
