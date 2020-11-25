package ca.nrc.data.harvesting;

import java.net.MalformedURLException;

public class DownloadActivityException extends Exception {
	public DownloadActivityException(String mess, Exception e) {
		super(mess, e);
	}
	public DownloadActivityException(Exception e) {
		super(e);
	}
	public DownloadActivityException(String mess) {
		super(mess);
	}
}
