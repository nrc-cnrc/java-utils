package ca.nrc.ui.commandline;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.function.Supplier;

/**
 * Use this class to run a Runnable, feeding its System.in inputs through
 * a string.
 *
 * Very useful for testing methods that read inputs from System.in.
 *
 */
public class RunWithStdinInputs {
	public static void run(Runnable task, String inputs) {
		InputStream oldStdin = System.in;
		try {
			InputStream inputStream =
			new ByteArrayInputStream(inputs.getBytes());
			System.setIn(inputStream);
			task.run();
		} finally {
			System.setIn(oldStdin);
		}
	}
	public static <T> T run(Supplier<T> task, String inputs) {
		InputStream oldStdin = System.in;
		T result = null;
		try {
			InputStream inputStream =
			new ByteArrayInputStream(inputs.getBytes());
			System.setIn(inputStream);
			result = task.get();
		} finally {
			System.setIn(oldStdin);
		}

		return result;
	}
	public static <T> void copy2(List<T> dest, List<T> src) {
	}
//	public static <T>  T runWithStdinInputs(Producer<T> task, String inputs) {
//		InputStream oldStdin = System.in;
//		try {
//			InputStream inputStream =
//			new ByteArrayInputStream(inputs.getBytes());
//			System.setIn(inputStream);
//			task.run();
//		} finally {
//			System.setIn(oldStdin);
//		}
//	}

}
