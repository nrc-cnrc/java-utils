package ca.nrc.data.harvesting;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Represents information about a particular page download done
 * on a particular domain.
 */
public class DownloadActivity {
	public String host = null;
	public URL page = null;
	public Long endedAtMsecs = null;
	public Long lastedMsecs = null;

	public DownloadActivity(URL _page, Long _startMSecs, Long _endMSecs) {
		init_DownloadActivity(_page, _startMSecs, endedAtMsecs);
	}

	public DownloadActivity(String _url, Long _startMSecs, Long _endMSecs) throws DownloadActivityException {
		init_DownloadActivity(_url, _startMSecs, _endMSecs);
	}

	protected void init_DownloadActivity(String _urlStr, Long _startMSecs,
		 Long _endMSecs) throws DownloadActivityException {
		URL url = null;
		try {
			url = new URL(_urlStr);
		} catch (MalformedURLException e) {
			throw new DownloadActivityException(e);
		}
		init_DownloadActivity(url, _startMSecs, _endMSecs);
	}

	protected void init_DownloadActivity(URL _page, Long _startMSecs, Long _endMSecs) {
		page = _page;
		host = _page.getHost();
		endedAtMsecs = _endMSecs;
		lastedMsecs = _endMSecs - _startMSecs;
	}
}
