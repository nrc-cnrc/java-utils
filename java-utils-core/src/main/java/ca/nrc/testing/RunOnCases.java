package ca.nrc.testing;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is an alternative to Parametrized junit tests.
 *
 * It has two advantages:
 *
 * Case data can be object
 *   With parametrized tests, the case data must all be strings.
 *   With RunOnCases, the case data is an array objects of any types.
 *
 * Case filtering
 *   With parametrized tests, there is currently no way to run the test
 *   on specific case data. This is possible with RunOnCases.
 *   Also, when the test is run on just one case, the test will remind you
 *   to deactivate the case filter.
 *
 */
public class RunOnCases {
	private Case[] cases;
	private Consumer<Case> runner;
	private Integer[] testNumsToRun;
	private Integer[] testNumsToExcl;
	private String testDescrRegex;

	public RunOnCases(Case[] _cases, Consumer<Case> _runner) {
		this.cases = _cases;
		this.runner = _runner;
	}

	public void run() throws Exception {
		Set<Integer> casesRun = new HashSet<Integer>();
		int caseNum = 0;
		for (Case aCase: cases) {
			caseNum++;
			if (inactive(caseNum, aCase)) {
				continue;
			}
			casesRun.add(caseNum);
			System.out.println("Running "+aCase.fullDescription(caseNum));
			runner.accept(aCase);
		}

		if (casesRun.size() < cases.length) {
			throw new SkippedCasesException();
		} else if (casesRun.isEmpty()) {
			throw new AllCasesSkippedException();
		}
	}

	private boolean inactive(Integer caseNum, Case aCase) {
		boolean inactive = false;
		if (this.testNumsToRun != null &&
			!Arrays.asList(testNumsToRun).contains(caseNum)) {
			inactive = true;
		} else if (this.testNumsToExcl !=null &&
			Arrays.asList(testNumsToExcl).contains(caseNum)) {
			inactive = true;
		} else {
			if (testDescrRegex != null) {
				Matcher matcher =
					Pattern.compile(testDescrRegex)
					.matcher(aCase.descr);
				if (!matcher.find()) {
					inactive = true;
				}
			}
		}
		return inactive;
	}

	public RunOnCases onlyCaseNums(Integer... _testNumsToRun) {
		if (_testNumsToRun.length == 0) {
			_testNumsToRun = null;
		}
		this.testNumsToRun = _testNumsToRun;
		this.testNumsToExcl = null;
		this.testDescrRegex = null;
		return this;
	}

	public RunOnCases allButCaseNums(Integer... _testNumsToExcl) {
		if (_testNumsToExcl.length == 0) {
			_testNumsToExcl = null;
		}
		this.testNumsToExcl = _testNumsToExcl;
		this.testNumsToRun = null;
		this.testDescrRegex = null;
		return this;
	}

	public RunOnCases onlyCasesWithDescr(String _testDescrRegex) {
		this.testDescrRegex = _testDescrRegex;
		this.testNumsToRun = null;
		this.testNumsToExcl = null;
		return this;
	}

	public static class Case {
		public  String descr = "";
		public Object[] data = new Object[0];

		public Case(String _descr, Object... _data) {
			this.descr = _descr;
			this.data = _data;
		}

		public String fullDescription(int caseNum) {
			String fullDescr = "Case #"+caseNum;
			if (descr != null && !descr.isEmpty()) {
				fullDescr += ": "+descr;
			}
			return fullDescr;
		}
	}

	public class SkippedCasesException extends Exception {
		public SkippedCasesException() {
			super("Some cases were skipped. Remember to re-rerun the tests with all case filters off");
		}
	}

	public class AllCasesSkippedException extends Exception {
		public AllCasesSkippedException() {
			super("All cases were skipped. There may be a bug in your case filters");
		}
	}
}
