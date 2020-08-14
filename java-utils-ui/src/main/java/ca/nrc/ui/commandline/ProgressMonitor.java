package ca.nrc.ui.commandline;

public abstract class ProgressMonitor {

	protected String message = null;
	public Long numSteps = null;
	private int refreshEveryNSecs = 60;

	protected long stepsSoFar = 0;
	protected long startMSecs = 0;
	protected long elapsedMSecs = 0;
	private Long lastRefreshTimeMsecs = null;
	private long msecsSinceLastRefresh = 0;
	private long msecsSinceBeginning = 0;
	protected long stepsSinceLastRefresh = 0;
	private long msecsPerStep = 0;
	private double progress = 0.0;
	private Long eta = null;
	private Long lastIterationStartMSecs = null;

	protected abstract void displayProgress(double _progress, long _eta, long _msecsPerStep);
	
	public ProgressMonitor(long _numSteps, String _message) {
		initialize_ProgressMonitor(_numSteps, _message, null);
	}

	public ProgressMonitor(long _numSteps, String _message, Integer _refreshEveryNSecs) {
		initialize_ProgressMonitor(_numSteps, _message, _refreshEveryNSecs);
	}

	private void initialize_ProgressMonitor(long _numSteps, String _message, Integer _refreshEveryNSecs) {
		this.numSteps = _numSteps;
		this.message = _message;
		startMSecs = System.currentTimeMillis();
		lastRefreshTimeMsecs = startMSecs;

		if (_refreshEveryNSecs != null) {
			this.refreshEveryNSecs = _refreshEveryNSecs;
		}
	}

	public void stepCompleted() {
		stepCompleted(1);
	}

	public void stepCompleted(long steps) {
		stepsSoFar += steps;
		stepsSinceLastRefresh += steps;
		updateStats();
		refreshProgress();
	}

	private void refreshProgress() {
		if (msecsSinceLastRefresh > 1000 * refreshEveryNSecs) {
			displayProgress(progress, eta, msecsPerStep);
			lastRefreshTimeMsecs = System.currentTimeMillis();
			stepsSinceLastRefresh = 0;
		}
	}

	private void updateStats() {
		long nowMSecs = System.currentTimeMillis();
		elapsedMSecs = nowMSecs - startMSecs;
		msecsSinceLastRefresh = nowMSecs - lastRefreshTimeMsecs;
		msecsPerStep = msecsSinceLastRefresh / stepsSinceLastRefresh;
		long remainingSteps = numSteps - stepsSoFar;
		eta = remainingSteps * msecsPerStep;
		progress = 1.0 * stepsSoFar / numSteps;
//		System.out.println("--** updateStats: numSteps="+numSteps+", stepsSoFar="+stepsSoFar+", remaningSteps="+remainingSteps+", msecsPerStep="+msecsPerStep+", eta="+eta+", progress="+progress);
	}
	
	protected int percentProgress(double _progress) {
		int percentage = (int) Math.ceil((_progress) * 100);
		return percentage;
	}

	protected String formatTimelapse(long _eta) {
		return formatTimelapse(_eta, null);
	}
	
	protected String formatTimelapse(long _eta, Boolean includeMsecs) {
		if (includeMsecs == null) {
			includeMsecs = false;
		}
		
		long msecs = _eta; 
		
		long hours = msecs / (3600 * 1000);
		msecs = msecs - hours * (3600 * 1000);
		
		long mins = msecs / (60  * 1000);
		msecs = msecs - mins * (60 * 1000);
		
		long secs = msecs / 1000;
		msecs = msecs - secs * 1000;
		
		String etaStr = "";
		if (includeMsecs) {
			etaStr = " " + msecs + "ms";
		}
		
		etaStr = secs + "s" + etaStr;
		if (mins > 0 || hours > 0) {
			etaStr = mins + "m " + etaStr;
		}
		if (hours > 0) {
			etaStr = hours + "h " + etaStr;
		}
		
		return etaStr;
	}

}
