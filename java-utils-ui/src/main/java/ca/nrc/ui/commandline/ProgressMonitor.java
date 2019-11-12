package ca.nrc.ui.commandline;

import java.text.SimpleDateFormat;
import java.util.Date;

public abstract class ProgressMonitor {

	protected String message = null;
	private Long numSteps = null;
	private double refreshEvery = 0.1;	
	
	private long stepsSoFar = 0;
	private double progress = 0.0;
	private double nextRefresh = refreshEvery;
	private Long eta = null;
	private Long startMsecs = null;
	private Long msecsPerStep = null;
	
	protected abstract void displayProgress(double _progress, long _eta, long _msecsPerStep);
	
	public ProgressMonitor(long _numSteps, String _message) {
		initialize_ProgressMonitor(_numSteps, _message, null);
	}

	public ProgressMonitor(long _numSteps, String _message, Double _refreshEvery) {
		initialize_ProgressMonitor(_numSteps, _message, _refreshEvery);
	}

	private void initialize_ProgressMonitor(long _numSteps, String _message, Double _refreshEvery) {
		this.numSteps = _numSteps;
		this.message = _message;
		startMsecs = System.currentTimeMillis();
		
		if (_refreshEvery != null) this.refreshEvery = _refreshEvery;
		
		nextRefresh = refreshEvery;
	}
	

	public void stepCompleted() {
		stepsSoFar++;
		updateStats();
		refreshProgress();
	}

	private void refreshProgress() {
		if (progress >= nextRefresh) {
			nextRefresh += refreshEvery;
			displayProgress(progress, eta, msecsPerStep);
		}
	}

	private void updateStats() {
		Long elapsed = System.currentTimeMillis() - startMsecs;
		progress = 1.0 * stepsSoFar / numSteps;
		eta = elapsed * (numSteps - stepsSoFar) / stepsSoFar;
		msecsPerStep = elapsed / stepsSoFar;
//		System.out.println(
//				"-- ProgressMonitor.updateStats: stepsSoFar="+stepsSoFar+
//				", progress="+progress+", eta="+eta+
//				", formatted eta="+formatTimelapse(eta));
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
