package ca.nrc.dtrc.elasticsearch.es6.request.query;

import ca.nrc.dtrc.elasticsearch.es6.request.AssertRequestBodyElement;
import ca.nrc.testing.RunOnCases;
import ca.nrc.testing.RunOnCases.*;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

public class QueryStringTest {

	@Test
    public void test__QueryString__HappyPath() throws Exception {
		QueryString gotQuery = new QueryString("name:Simpson");
		String expJson =
			"{\"query_string\": {\"query\":\"name:Simpson\"}}";
		new AssertRequestBodyElement(gotQuery)
			.jsonEquals(expJson);
	}

	@Test
	public void test__QueryString__SeveralCases() throws Exception {
		Case[] cases = new Case[] {
			new Case("Simplest case",
				"name:Homer", null,
				"{\"query_string\": {\"query\":\"name:Homer\"}}"
			),
			new Case("Non-null type name",
				"name:Homer", "SimpsonsFamilyMember",
				"{\"query_string\":{\"query\":\"name:Homer +type:\\\"SimpsonsFamilyMember\\\"\"}}"
			),
		};
		Consumer<Case> runner = (aCase) -> {
			String queryString = (String) aCase.data[0];
			String typeName = (String) aCase.data[1];
			String expJson = (String) aCase.data[2];
			QueryString gotQuery =
				new QueryString(queryString)
					.typeContraint(typeName);
			try {
				new AssertRequestBodyElement(gotQuery)
					.jsonEquals(expJson);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		};

		new RunOnCases(cases, runner)
//			.onlyCaseNums(2)
			.run();

	}
}
