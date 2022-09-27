package ca.nrc.ui.commandline;

import static org.junit.Assert.*;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public abstract class ProgressMonitorTest {
	
	protected abstract ProgressMonitor makeProgressMonitor(int numSteps, String message);
	
	ProgressMonitor monitor = null;
	
	@Before
	public void setUp() {
		this.monitor = makeProgressMonitor(10, "Test monitor");
	}

	@Test
	public void test__ProgressMonitor__Synopsis() {
		//
		// Use this class to show progress of a long
		// process
		//
		
		// Let's say the process involves 100 steps, each step being
		// somewhat equivalent in length (ex: 100 files to process)
		//
		int numSteps = 100;
		String message = "Doing something";
		
		// Note: For the purpose of this abstract test, we use the 
		//   factory method makeProgressMonitor (because ProgressMonitor
		//   is an abstract class that cannot be instantiated)
		//
		ProgressMonitor monitor = makeProgressMonitor(numSteps, message);

		// By default, the monitor will refresh the progress display every
		// 60 secs. Howewever, you can change it as follows. For example,
		// this will refresh the progress display every second.
		monitor.refreshEveryNSecs = 1;

		// Everytime you complete a step, you invoke stepCompmleted().
		// This will automatically show the progress
		//
		monitor.stepCompleted();
	}
	
	@Test
	public void test__percentProgress__HappyPath() {
		double progress = 0.123;
		int gotPercent = monitor.percentProgress(progress);
		int expPercent = 13;
		Assert.assertEquals(expPercent, gotPercent);
	}
	
	@Test
	public void test__formatTimelapse__HappyPath() {
		long etaSecs = 2*3600 + 3*60 + 15;
		long etaMsecs = etaSecs * 1000 + 343;
		String gotFormatted = monitor.formatTimelapse(etaMsecs);
		String expFormatted = "2h 3m 15s";
		Assert.assertEquals(expFormatted, gotFormatted);
	}
}
