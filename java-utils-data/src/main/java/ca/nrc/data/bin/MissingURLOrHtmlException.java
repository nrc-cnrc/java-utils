package ca.nrc.data.bin;

import ca.nrc.data.harvesting.PageHarvesterException;
import org.json.JSONObject;

public class MissingURLOrHtmlException extends PageHarvesterException {
	public MissingURLOrHtmlException(JSONObject input) {
		super("JSON input must have either an 'url' or 'html' attribute: "+input.toString());
	}
}
