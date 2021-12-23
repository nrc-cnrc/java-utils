package ca.nrc.dtrc.elasticsearch.es6.request.query;

import ca.nrc.dtrc.elasticsearch.es6.request.AssertRequestBodyElement;
import org.junit.jupiter.api.Test;

public class FilterClauseTest {

	@Test
	public void test__FilterClause__HappyPath() throws Exception {
		FilterClause gotClause = new FilterClause()
			.addTerm("name", "Homer")
			.addTerm("show", "The Simpsons");
		String expJson = "{\"term\":{\"name\":\"homer\",\"show\":\"the simpsons\"}}";
		new AssertRequestBodyElement(gotClause)
			.jsonEquals(expJson);
	}
}
