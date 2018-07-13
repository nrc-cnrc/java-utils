package ca.nrc.debug;

import java.util.ArrayList;
import java.util.List;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;


public class JVM {
	public static List<String> jvmArgs() {
		RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
		List<String> args = runtimeMxBean.getInputArguments();			
		
		return args;
	}
	
//	public static void main(String[] cmdLine) {
//		List<String> args = jvmArgs();
//		System.out.println("JVM args="+args);
//	}
}
