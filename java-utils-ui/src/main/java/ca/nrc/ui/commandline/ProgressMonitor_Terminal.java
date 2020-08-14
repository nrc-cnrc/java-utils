package ca.nrc.ui.commandline;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ProgressMonitor_Terminal extends ProgressMonitor {
	
	protected UserIO userIO = null;

	public ProgressMonitor_Terminal(long numSteps, String message) {
		super(numSteps, message);
		initialize_ProgressMonitor_Terminal(null);
	}
	
	public ProgressMonitor_Terminal(long numSteps, String message, UserIO _userIO) {
		super(numSteps, message, null);
		initialize_ProgressMonitor_Terminal(_userIO);
	}

	public ProgressMonitor_Terminal(long numSteps, String message, Integer _refreshEveryNSecs) {
		super(numSteps, message, _refreshEveryNSecs);
		initialize_ProgressMonitor_Terminal(null);
	}

	public ProgressMonitor_Terminal(long numSteps, String message, Integer _refreshEveryNSecs, UserIO _useIO) {
		super(numSteps, message, _refreshEveryNSecs);
		initialize_ProgressMonitor_Terminal(null);
	}
	
	
	private void initialize_ProgressMonitor_Terminal(UserIO _userIO) {
		if (_userIO == null) {
			_userIO = new UserIO().setVerbosity(1);	
		}
		this.userIO = _userIO;
	}

	@Override
	protected void displayProgress(double _progress, long _eta, long _msecsPerStep) {
		int percentage = percentProgress(_progress);
		String etaStr = formatTimelapse(_eta);
		String msecsStepsStr = formatTimelapse(_msecsPerStep, true);
		String currMess = message + "\n" +
			percentage+"% ; ETA in "+etaStr+"; Avg step: "+msecsStepsStr+
			"\n  Elapsed (secs): "+(elapsedMSecs / 1000)+
			"\n  Completed "+stepsSoFar+" steps of "+numSteps+" ("+stepsSinceLastRefresh+" since last Refresh)";
		userIO.echo(currMess);
	}

}
