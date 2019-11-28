package ca.nrc.data.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;

public class FileGlob {
	
	public static class CollectingFileVisitor extends SimpleFileVisitor<Path> {
		private PathMatcher matcher = null;
		private List<File> collectedFiles = null;
		
		public CollectingFileVisitor(String pattern) {
			collectedFiles = new ArrayList<File>();	
			FileSystem fs = FileSystems.getDefault();
			//Have to escape windows file separators since \\ is a glob escape character
			matcher = fs.getPathMatcher("glob:" + pattern.replace("\\", "\\\\"));
		}
		
	    @Override
	    public FileVisitResult visitFile(Path file, BasicFileAttributes attribs) {
	        Path fPath = file.toAbsolutePath();
	        if (matcher.matches(fPath)) {
	            collectedFiles.add(new File(file.toString()));
	        }
	        return FileVisitResult.CONTINUE;
	    }
	    
	    public FileVisitResult visitFileFailed(Path file, IOException io)
	    {   
	        return FileVisitResult.SKIP_SUBTREE;
	    }	    

	    public File[] getFiles() {
	    	File[] files = (File[]) collectedFiles.toArray(new File[collectedFiles.size()]);
	    	return files;
	    }
		
	}

	
	public static class DeleteFileVisitor extends SimpleFileVisitor<Path> {
		private PathMatcher matcher = null;
		
		public DeleteFileVisitor(String pattern) {
			FileSystem fs = FileSystems.getDefault();
			matcher = fs.getPathMatcher("glob:" + pattern.replace("\\", "\\\\"));
		}
		
	    @Override
	    public FileVisitResult visitFile(Path file, BasicFileAttributes attribs) {
	        Path fPath = file.toAbsolutePath();
	        if (matcher.matches(fPath)) {
	            file.toFile().delete();
	        }
	        return FileVisitResult.CONTINUE;
	    }
	    
	    public FileVisitResult visitFileFailed(Path file, IOException io)
	    {   
	        return FileVisitResult.SKIP_SUBTREE;
	    }	    
	}
	
	
	public static File[] listFiles(String pattern)  {		
		Path startDir = Paths.get(getStartingDir(pattern));

		File[] files = new File[0];
		CollectingFileVisitor matcherVisitor = new CollectingFileVisitor(pattern);
		try {
			Files.walkFileTree(startDir, matcherVisitor);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		files = matcherVisitor.getFiles();
		
		return files;
	}
	

	public static File[] listFiles(File rootDir, String[] patterns)  {
		if (!rootDir.isDirectory()) {
			throw new IllegalArgumentException("Root path was not a directory (was "+rootDir.toString()+")");
		}
		
		for (int ii=0; ii < patterns.length; ii++) {
			patterns[ii] = FilenameUtils.concat(rootDir.toString(), patterns[ii]);
		}
		File[] matchingFiles = listFiles(patterns);
		
		return matchingFiles;
	}
	
	
	public static File[] listFiles(String[] patterns)  {
		Set<File> matchingFilesLst = new HashSet<File>();
		for (String aPattern: patterns) {
			File[] filesThisPattern = listFiles(aPattern);
			for (File aFile: filesThisPattern) matchingFilesLst.add(aFile);
		}
		
		File[] matchingFilesArr = matchingFilesLst.toArray(new File[matchingFilesLst.size()]);
		return matchingFilesArr;
	}
	
	public static void deleteFiles(String pattern) {
		Path startDir = Paths.get(getStartingDir(pattern));

		File[] files = new File[0];
		CollectingFileVisitor matcherVisitor = new CollectingFileVisitor(pattern);
		try {
			Files.walkFileTree(startDir, matcherVisitor);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		files = matcherVisitor.getFiles();
	}
	
	public static void main(String[] args)  {
		String pattern = args[0];
		System.out.println("Files matching: "+pattern);
		File[] files = FileGlob.listFiles(pattern);
		if (files.length == 0) System.out.println("No match found");
		for (File aFile: files) {
			System.out.println(aFile.getAbsolutePath());
		}
	}

	protected static String getStartingDir(String pattern) {
		String startingDir = truncatePatternToFirstWildcard(pattern);
		if (!endsWithFileSeparator(startingDir)) {
			File parentDir = Paths.get(startingDir).toFile().getParentFile();			
			if (parentDir != null) {
				startingDir = parentDir.toString();
			}
		}
		
		if (!pattern.matches("^([a-zA-Z]:(\\|/|\\\\)|[\\/]|\\.).*$")) {
			startingDir = "./" + startingDir; 
		}
		return startingDir;
	}

	private static boolean endsWithFileSeparator(String path) {
		//Non-Windows OS
		if(!System.getProperty("os.name").toLowerCase().startsWith("win")) {
			return path.endsWith(File.separator);
		}else {
			return path.endsWith(File.separator) | path.endsWith("/");
		}
	}
	
	private static String truncatePatternToFirstWildcard(String pattern) {
		pattern = pattern.replaceFirst("[\\*\\?].*$", "");
		
		return pattern;
	}	
}
