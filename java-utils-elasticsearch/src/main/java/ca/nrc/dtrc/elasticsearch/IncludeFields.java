package ca.nrc.dtrc.elasticsearch;

public class IncludeFields extends FieldFilter {

	public IncludeFields(String _regexp) {
		super(_regexp, false);
	}

}
