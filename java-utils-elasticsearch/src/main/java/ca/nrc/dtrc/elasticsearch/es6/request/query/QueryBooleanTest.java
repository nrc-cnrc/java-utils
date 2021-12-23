package ca.nrc.dtrc.elasticsearch.es6.request.query;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

public class QueryBooleanTest {

	//////////////////////////////////
	// DOCUMENTATION TESTS
	//////////////////////////////////

	@Test
	public void test__QueryBoolean__Synopsis() {
		// A QueryBoolean is made up of several types of clauses

		// "must" clause
		MustClause must =
			new MustClause(new JSONObject()
				.put("exists", new JSONObject()
					.put("field", "userName")
				)
			);

		// "must_not" clause
		MustNotClause mustNot =
			new MustNotClause(new JSONObject()
				.put("exists", new JSONObject()
					.put("field", "disabled")
				)
			);

		// "should" clause
		ShouldClause should =
			new ShouldClause(new JSONObject()
				.put("exists", new JSONObject()
					.put("field", "memberSince")
				)
			);

		// You can specify one or more of those
		QueryBoolean query = new QueryBoolean(must, mustNot, should);

		// You can also add new clauses
		MustClause additionalClause =
			new MustClause(new JSONObject()
				.put("exists", new JSONObject()
					.put("field", "gender")
				)
			);

		query.addClause(additionalClause);
	}
}
