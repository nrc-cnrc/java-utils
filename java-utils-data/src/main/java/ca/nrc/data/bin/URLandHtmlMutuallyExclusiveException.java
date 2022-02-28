package ca.nrc.data.bin;

import ca.nrc.data.harvesting.PageHarvesterException;
import org.json.JSONObject;

public class URLandHtmlMutuallyExclusiveException extends PageHarvesterException {
	public URLandHtmlMutuallyExclusiveException(JSONObject input) {
		super("JSON input cannot have both 'url' and 'html' attributes at the same time: " + input.toString());
	}
}
