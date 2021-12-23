package ca.nrc.dtrc.elasticsearch.es6;

public class ExcludeFields extends FieldFilter {

	public ExcludeFields(String _regexp) {
		super(_regexp, true);
	}

}
