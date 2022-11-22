package ca.nrc.ui.commandline;

import ca.nrc.testing.AssertString;
import ca.nrc.io.StdoutCapture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MainCommandTest {

    protected MainCommand console = null;

    @Before
    public void setUp() throws Exception {
        StdoutCapture.startCapturing();

        console = new MainCommand("Test CLI");
        console.addSubCommand(
            new TestCommand("hello", "Print hello."));
        console.addSubCommand(
            new TestCommand("greetings", "Print greetings."));
    }

    @After
    public void tearDown() {
        StdoutCapture.stopCapturing();
    }

    @Test
    public void test__Help() throws Exception {

        try {
            console.run(new String[0]);
        } catch (Exception e) {
            // We expect an exception to be raised after help was printed.
        }
        String gotHelp = StdoutCapture.stopCapturing();
        AssertString.assertStringContains(
            "Help was not as expected",
            gotHelp, "Usage: [greetings | hello] <OPTION>?");
    }

    public static class TestCommand extends SubCommand {

        private String overview = null;
        private String name = null;

        public TestCommand(String name, String overview) {
            super(name, false);
            this.name = name;
            this.overview = overview;
        }

        @Override
        public void execute() throws Exception {

        }

        @Override
        public String getUsageOverview() {
            return null;
        }
    }
}
