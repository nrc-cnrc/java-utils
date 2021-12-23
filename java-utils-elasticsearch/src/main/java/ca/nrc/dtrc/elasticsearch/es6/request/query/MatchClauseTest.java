package ca.nrc.dtrc.elasticsearch.es6.request.query;

import org.junit.jupiter.api.Test;

public class MatchClauseTest {

	@Test
	public void test__MatchClause__HappyPath() {
		MatchClause gotClause = new MatchClause()
			.addField("name", "Homer")
			.addField("show", "The Simpsons");


	}
}
