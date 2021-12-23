package ca.nrc.dtrc.elasticsearch;

public class ExcludeFields extends FieldFilter {

	public ExcludeFields(String _regexp) {
		super(_regexp, true);
	}

}
