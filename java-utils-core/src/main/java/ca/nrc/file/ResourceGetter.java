package ca.nrc.file;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.file.attribute.FileAttribute;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

/*
 * A class for accessing resource files and directories.
 */

public class ResourceGetter {

	public static String getResourcePath(File resourceRelativePath) throws IOException {
		return getResourcePath(resourceRelativePath.toString());
	}

	public static String getResourcePath(String resourceRelativePath) throws IOException
	{
		return getResourcePath(resourceRelativePath, null);
	}
	
	
	public static String getResourcePath(String resourceRelativePath, Class relativeToClass) throws IOException {
		URL resourceURL = getResourceFileUrl(resourceRelativePath, relativeToClass);
		
		String resourcePath = null;
		resourcePath = resourceURL.getPath().toString();

		/* Replace %20 by spaes */
		resourcePath = resourcePath.replaceAll("%20", " ");

		/* For Windows, substitute /C:/etc -> C:/etc.. */
		String regexWindowsPathPrefix = "^/([A-Z]\\:/)";
		resourcePath = resourcePath.replaceAll(regexWindowsPathPrefix, "$1");
		
		
		return resourcePath;
	}


	public static URL getResourceFileUrl(String resourceRelativePath) throws IOException {
		return getResourceFileUrl(resourceRelativePath, null);
	}

	public static URL getResourceFileUrl(String resourceRelativePath, Class relativeToClass) throws IOException {
		ClassLoader loader;
		if (relativeToClass == null)
		{
			loader = Thread.currentThread().getContextClassLoader();
		} else {
			loader = relativeToClass.getClassLoader();
		}
		
		URL resourceURL = null;
		resourceURL = loader.getResource(resourceRelativePath.replace("\\", "/")); //getResource doesn't recognize Windows file separators
		
		if (resourceURL == null) {
			String message = "Could not find a resource file with path: "+resourceRelativePath+".";
			if (relativeToClass != null) {
				message = message+"\nPath was relative to class: "+relativeToClass.toString();
			}
			throw new IOException(message);
		}

		return resourceURL;
	}
	
	public static InputStream getResourceAsStream(String resourceRelativePath) throws IOException{
		return getResourceAsStream(resourceRelativePath, null);
	}
	
	public static InputStream getResourceAsStream(String resourceRelativePath, Class relativeToClass) throws IOException{
		ClassLoader loader;
		if (relativeToClass == null)
		{
			loader = Thread.currentThread().getContextClassLoader();
		} else {
			loader = relativeToClass.getClassLoader();
		}
		
		InputStream resStream = loader.getResourceAsStream(resourceRelativePath);
		if (resStream == null) {
			String message = "Could not find a resource file with path: "+resourceRelativePath+".";
			if (relativeToClass != null) {
				message = message+"\nPath was relative to class: "+relativeToClass.toString();
			}
			throw new IOException(message);
		}

		return resStream;
	}

	public static void createFileIfNotExist(String filePath) throws IOException {
		File f = new File(filePath);
		if (!f.exists()) {
			FileOutputStream fStream = FileUtils.openOutputStream(f);
		}
	}
	
	public static void createDirectoryIfNotExists(Path dir) throws IOException {
		
		if (!Files.exists(dir)) {
			Files.createDirectories(dir);
		}
		return;
	}
	
	public static Path copyResourceFilesToTempDir(File resDirRelPath) throws ResourceGetterException {
		return copyResourceFilesToTempDir(resDirRelPath.toString());
	}

	public static Path copyResourceFilesToTempDir(String resDirRelPath) throws ResourceGetterException {
		Path tempDir;
		try {
			tempDir = Files.createTempDirectory("", new FileAttribute[0]);
		} catch (IOException e) {
			throw new ResourceGetterException("Could not create temporary directory", e);
		}
		copyResourceFilesToDir(resDirRelPath, tempDir);
		
		return tempDir;
	}

	public static File copyResourceToDir(String resDirRelPath, Path targDir) throws ResourceGetterException {
		String resPath;
		try {
			resPath = getResourcePath(resDirRelPath);
		} catch (IOException e1) {
			throw new ResourceGetterException("Could not find resource with path '"+resDirRelPath+"'", e1);
		}


		if (!targDir.toFile().exists()) {
			targDir.toFile().mkdirs();
		}

		File destFile = null;
		if (isInJar(resPath)) {
			destFile = copyJarResourceDir(resPath, targDir);
		} else {
			destFile = copyFileSystemResourceToDir(resPath, targDir);
		}

		return destFile;
	}

	private static File copyJarResourceDir(String resPath, Path targDir)
		throws ResourceGetterException {
		throw new ResourceGetterException("This method is not implemented yet.");
	}

	private static File copyFileSystemResourceToDir(String resPath, Path targDir)
		throws ResourceGetterException {
		File resFile = new File(resPath);
		File destFile = new File(targDir.toFile(), resFile.getName());
		try {
			if (resFile.isDirectory()) {
				FileUtils.copyDirectory(resFile, targDir.toFile());
			} else {
				FileUtils.copyFile(resFile, destFile);
			}
		} catch (IOException e) {
			throw new ResourceGetterException(
					"Could not copy '"+resPath+"' to directory '"+targDir.toString()+"'",
					e);
		}

		return destFile;
	}


	public static void copyResourceFilesToDir(String resDirRelPath, Path targDir) throws ResourceGetterException {
		String resPath;
		try {
			resPath = getResourcePath(resDirRelPath);
		} catch (IOException e1) {
			throw new ResourceGetterException("Could not find resource with path '"+resDirRelPath+"'", e1);
		}
		
		File tempLocation = null;

		if (isInJar(resPath)) {
			tempLocation = copyJarResourceToTempFile(resPath);
		} else {
			tempLocation = copyFileSystemResourceToTempFile(resPath);
		}
		
		FileCopy.copyFolder(tempLocation.toPath(), targDir);	
	}
	
	
	public static File copyResourceToTempLocation(String resRelPath) throws ResourceGetterException {
		Logger tLogger = Logger.getLogger("ca.nrc.file.ResourceGetter.copyResourceToTempLocation");


		String resPath;
		try {
			resPath = getResourcePath(resRelPath);
		} catch (IOException e1) {
			throw new ResourceGetterException("Could not find resource with path '"+resRelPath+"'", e1);
		}
		
		File tempLocation = null;

		if (isInJar(resPath)) {
			tLogger.trace("Getting resource from JAR");
			tempLocation = copyJarResourceToTempFile(resRelPath);
		} else {
			tLogger.trace("Getting resource from File system");
			tempLocation = copyFileSystemResourceToTempFile(resPath);
		}
						
		return tempLocation;
	}

	private static File copyFileSystemResourceToTempFile(String resPath) throws ResourceGetterException {
		File resFile = new File(resPath);
		File tempLocation = makeTempLocationForCopy(resPath);
		try {
			if (resFile.isDirectory()) {
				FileUtils.copyDirectory(resFile, tempLocation);
			} else {
				FileUtils.copyFile(resFile, tempLocation);
			}
		
		} catch (IOException e) {
			throw new ResourceGetterException(
					"Could not copy '"+resPath+"' to '"+tempLocation.toString()+"'", 
					e);
		}
		
		return tempLocation;
	}


	private static File makeTempLocationForCopy(String resPath) throws ResourceGetterException {
		File resFile = new File(resPath);
		String ext = FilenameUtils.getExtension(resPath);
		String fname = FilenameUtils.getBaseName(resPath);
		
		String suffix = "";
		if (ext !=  null && !ext.isEmpty()) suffix = "."+ext;
		
		File tempLocation = null;
		try {
			if (resFile.isDirectory()) {
				tempLocation = Files.createTempDirectory(fname).toFile();
			} else {
			tempLocation = File.createTempFile(fname, suffix);
				
			}
		} catch (IOException e) {
			throw new ResourceGetterException(e);
		}
	return tempLocation;
}


	private static File copyJarResourceToTempFile(String resPath) throws ResourceGetterException {
		Logger tLogger = Logger.getLogger("ca.nrc.file.ResourceGetter.copyJarResourceToTempFile");
		tLogger.trace("Getting resPath="+resPath);
		File tempLocation = null;

		tLogger.trace("File copied to temp location="+tempLocation);

		return tempLocation;
	}

	private static boolean isInJar(String resPath) {
		boolean answer = false;
		if (Pattern.compile("\\.jar![\\s\\S]*$").matcher(resPath).find()) {
			answer = true;
		}
		
		return answer;
	}

	public static String readResourceFileToString(String resRelPath) throws IOException {
		InputStream stream = getResourceAsStream(resRelPath);
		
		InputStreamReader isReader = new InputStreamReader(stream);
	      //Creating a BufferedReader object
	      BufferedReader reader = new BufferedReader(isReader);
	      StringBuffer sb = new StringBuffer();
	      String str;
	      while((str = reader.readLine())!= null){
	         sb.append(str);
	      }
	      reader.close();
	      
	      return sb.toString();
	}

	public static void main(String[] args) throws Exception {
		File temp = ResourceGetter.copyResourceToTempLocation("eclipseTemplates.xml");
		System.out.println("-- ResourceGetter.main: temp="+temp);
	}
}
