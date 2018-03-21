package ca.nrc.file;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.regex.Matcher;

/*
 * A class for accessing resource files and directories.
 */

public class ResourceGetter {
	
	public static String getResourcePath(String resourceRelativePath) throws IOException
	{
		return getResourcePath(resourceRelativePath, null);
	}
	
	
	public static String getResourcePath(String resourceRelativePath, Class relativeToClass) throws IOException {
		URL resourceURL = getResourceFileUrl(resourceRelativePath, relativeToClass);
		
		String resourcePath = null;
		resourcePath = resourceURL.getPath().toString();

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
		resourceURL = loader.getResource(resourceRelativePath);
		
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
}
