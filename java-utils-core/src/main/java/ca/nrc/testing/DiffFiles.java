package ca.nrc.testing;

import java.util.HashSet;
import java.util.Set;

public class DiffFiles {
	
	public static enum OPTION {DIRECTORIES, NON_RECURSIVE, RECURSIVE};
	
	public static final OPTION DIRECTORIES = OPTION.DIRECTORIES;
	public static final OPTION NON_RECURSIVE = OPTION.NON_RECURSIVE;
	public static final OPTION RECURSIV = OPTION.RECURSIVE;

	public String diff(String file1, String file2, OPTION... options) {
		Set<OPTION> optSet = new HashSet<OPTION>();
		for (int ii=0; ii < options.length; ii++) optSet.add(options[ii]);
		
		return null;
	}
	
}
