package ca.nrc.ui.commandline;

public class ProgressMonitor_TerminalTest extends ProgressMonitorTest {

	@Override
	protected ProgressMonitor makeProgressMonitor(int numSteps, String message) {
		ProgressMonitor_Terminal monitor = new ProgressMonitor_Terminal(numSteps, message);
		return monitor;
	}

}
