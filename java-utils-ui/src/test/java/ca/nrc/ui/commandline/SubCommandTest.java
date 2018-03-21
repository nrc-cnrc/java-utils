package ca.nrc.ui.commandline;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class SubCommandTest {

	@Test
	public void test_verbosityToInt() {
		Integer gotInt = SubCommand.verbosityToInt(SubCommand.Verbosity.Level2);
		assertEquals(new Integer(2), gotInt);
		
		gotInt = SubCommand.verbosityToInt(SubCommand.Verbosity.Levelnull);
		assertEquals(null, gotInt);
		
	}
	
	/**************************************
	 * TEST HELPERS
	 **************************************/
	
	private void setCmdLineOptions(SubCommand cmd, Map<String,String> options) {
		MockCommandLine mockCmdLine = new MockCommandLine(new String[] {}, options);
		cmd.setCommandLine(mockCmdLine);
	}
	
	protected void setCmdLineOptions(SubCommand cmd, String... options) {
		if ((options.length % 2) != 0) {
			throw new IllegalArgumentException("List of options should contain an even number of elements. It was: "+String.join(", ", options));
		}
		Map<String,String> optHash = new HashMap<String,String>();
		for (int ii = 0; ii < options.length / 2; ii++) {
			optHash.put(options[ii], options[ii+1]);
		}
		setCmdLineOptions(cmd, optHash);
	}

	
	
}
