package ca.nrc.dtrc.elasticsearch.es6.request.query;

import ca.nrc.dtrc.elasticsearch.es6.Document;
import ca.nrc.dtrc.elasticsearch.es6.ESTestHelpers;
import ca.nrc.dtrc.elasticsearch.es6.ESTestHelpers.*;
import ca.nrc.dtrc.elasticsearch.es6.StreamlinedClient;
import ca.nrc.dtrc.elasticsearch.es6.request.AssertRequestBodyElement;
import ca.nrc.testing.RunOnCases;
import ca.nrc.testing.RunOnCases.*;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static ca.nrc.dtrc.elasticsearch.es6.ESTestHelpers.*;

public class QueryTest {

	@BeforeAll
	public static void beforeAll() throws Exception {
		boolean skip = true;
		org.junit.Assume.assumeFalse(skip);
	}

	//////////////////////////////////////////
	// DOCUMENTATION TESTS
	//////////////////////////////////////////

	@Test
	public void test__Query__Synopsis() throws Exception {

//		// You can create a Query with a mix of different clauses
//		new Query()
//			.queryString("name:Homer");

		StreamlinedClient esClient = makeCartoonTestIndex();
		ShowCharacter homer = new ShowCharacter("Homer", "Simpson", "The Simpsons");

		// You can create a query with any combination of boolean clauses
		// You need to provide at least one of them
		new Query()
			// Restrict the type of documents
			// This is COMPULSORY, unless you specify a moreLikeThis
			// clause
			.type("character")

			// A "freeform" query à la Google
			.queryString("surname:Simpson")

			// Criteria specified in the JSONObject MUST be met
			.must(new JSONObject())

			// Criteria specified in the JSONObject must NOT be met
			.mustNot(new JSONObject())

			// Criteria specified in the JSONObject SHOULD be met as much
			// as possible
			.should(new JSONObject())

			// The returned documents should be as CLOSE as  possible to the
			// provided document.
			// Note: You need to provide the method with an ESFactory client so it can
			//   find out which fields of the input document can be searched with
			//   a mlt query.
			.moreLikeThis(homer, esClient);
			;
	}

	//////////////////////////////////////////
	// VERIFICATION TESTS
	//////////////////////////////////////////

	@Test
	public void test__Query__VariousCases() throws Exception {
		makeCartoonTestIndex();
		Case[] cases = new Case[] {
			new Case("query_string",
				"name:Homer",
				null,
				null,
				null,
				null,
				"{\n" +
				"  \"query\":\n" +
				"    {\n" +
				"      \"bool\":\n" +
				"        {\n" +
				"          \"must\":\n" +
				"            [\n" +
				"              {\n" +
				"                \"query_string\":\n" +
				"                  {\n" +
				"                    \"query\":\n" +
				"                      \"name:Homer\"\n" +
				"                  }\n" +
				"              },\n" +
				"              {\n" +
				"                \"match\":\n" +
				"                  {\n" +
				"                    \"type\":\n" +
				"                      \"sometype\"\n" +
				"                  }\n" +
				"              }\n" +
				"            ]\n" +
				"        }\n" +
				"    }\n" +
				"}"
			),
			new Case("bool-must-single-clause",
				null,
				null,
				new MustClause(
					new JSONObject().put("exists", "id")
				),
				null,
				null,
				"{\n" +
				"  \"query\":\n" +
				"    {\n" +
				"      \"bool\":\n" +
				"        {\n" +
				"          \"must\":\n" +
				"            [\n" +
				"              {\n" +
				"                \"exists\":\n" +
				"                  \"id\"\n" +
				"              },\n" +
				"              {\n" +
				"                \"match\":\n" +
				"                  {\n" +
				"                    \"type\":\n" +
				"                      \"sometype\"\n" +
				"                  }\n" +
				"              }\n" +
				"            ]\n" +
				"        }\n" +
				"    }\n" +
				"}"
			),
			new Case("bool-must_not-single-clause",
				null,
				null,
				null,
				new MustNotClause(
					new JSONObject().put("exists", "id")
				),
				null,
				"{\n" +
				"  \"query\":\n" +
				"    {\n" +
				"      \"bool\":\n" +
				"        {\n" +
				"          \"must\":\n" +
				"            [\n" +
				"              {\n" +
				"                \"match\":\n" +
				"                  {\n" +
				"                    \"type\":\n" +
				"                      \"sometype\"\n" +
				"                  }\n" +
				"              }\n" +
				"            ],\n" +
				"          \"must_not\":\n" +
				"            [\n" +
				"              {\n" +
				"                \"exists\":\n" +
				"                  \"id\"\n" +
				"              }\n" +
				"            ]\n" +
				"        }\n" +
				"    }\n" +
				"}"
			),
			new Case("bool-should-single-clause",
				null,
				null,
				null,
				null,
				new ShouldClause(
					new JSONObject().put("exists", "id")),
				"{\n" +
				"  \"query\":\n" +
				"    {\n" +
				"      \"bool\":\n" +
				"        {\n" +
				"          \"must\":\n" +
				"            [\n" +
				"              {\n" +
				"                \"match\":\n" +
				"                  {\n" +
				"                    \"type\":\n" +
				"                      \"sometype\"\n" +
				"                  }\n" +
				"              }\n" +
				"            ],\n" +
				"          \"should\":\n" +
				"            [\n" +
				"              {\n" +
				"                \"exists\":\n" +
				"                  \"id\"\n" +
				"              }\n" +
				"            ]\n" +
				"        }\n" +
				"    }\n" +
				"}"
			),
			new Case("bool-AND-query_string",
				"name:Homer",
				null,
				null,
				null,
				new ShouldClause(
					new JSONObject().put("exists", "id")),
				"{\n" +
				"  \"query\":\n" +
				"    {\n" +
				"      \"bool\":\n" +
				"        {\n" +
				"          \"must\":\n" +
				"            [\n" +
				"              {\n" +
				"                \"query_string\":\n" +
				"                  {\n" +
				"                    \"query\":\n" +
				"                      \"name:Homer\"\n" +
				"                  }\n" +
				"              },\n" +
				"              {\n" +
				"                \"match\":\n" +
				"                  {\n" +
				"                    \"type\":\n" +
				"                      \"sometype\"\n" +
				"                  }\n" +
				"              }\n" +
				"            ],\n" +
				"          \"should\":\n" +
				"            [\n" +
				"              {\n" +
				"                \"exists\":\n" +
				"                  \"id\"\n" +
				"              }\n" +
				"            ]\n" +
				"        }\n" +
				"    }\n" +
				"}"
			),
			new Case("EMPTY query_string",
				"   ",
				null,
				null,
				null,
				null,
				"{\n" +
				"  \"query\":\n" +
				"    {\n" +
				"      \"bool\":\n" +
				"        {\n" +
				"          \"must\":\n" +
				"            [\n" +
				"              {\n" +
				"                \"match\":\n" +
				"                  {\n" +
				"                    \"type\":\n" +
				"                      \"sometype\"\n" +
				"                  }\n" +
				"              }\n" +
				"            ]\n" +
				"        }\n" +
				"    }\n" +
				"}"
			),

			new Case("more_like_this",
				null,
				new ESTestHelpers.ShowCharacter("homer", "simpson", "The Simpsons"),
				null,
				null,
				null,
				"{\n" +
				"  \"query\":\n" +
				"    {\n" +
				"      \"bool\":\n" +
				"        {\n" +
				"          \"more_like_this\":\n" +
				"            {\n" +
				"              \"highlight\":\n" +
				"                {\n" +
				"                  \"fields\":\n" +
				"                    {\n" +
				"                      \"content\":\n" +
				"                        {\n" +
				"                          \"type\":\n" +
				"                            \"plain\"\n" +
				"                        },\n" +
				"                      \"shortDescription\":\n" +
				"                        {\n" +
				"                          \"type\":\n" +
				"                            \"plain\"\n" +
				"                        }\n" +
				"                    },\n" +
				"                  \"order\":\n" +
				"                    \"score\"\n" +
				"                },\n" +
				"              \"query\":\n" +
				"                {\n" +
				"                  \"more_like_this\":\n" +
				"                    {\n" +
				"                      \"fields\":\n" +
				"                        [\n" +
				"                          \"firstName\",\n" +
				"                          \"idWithoutType\",\n" +
				"                          \"lang\",\n" +
				"                          \"show\",\n" +
				"                          \"surname\",\n" +
				"                          \"type\"\n" +
				"                        ],\n" +
				"                      \"like\":\n" +
				"                        {\n" +
				"                          \"_index\":\n" +
				"                            \"es-test-cartoons\",\n" +
				"                          \"doc\":\n" +
				"                            {\n" +
				"                              \"firstName\":\n" +
				"                                \"homer\",\n" +
				"                              \"idWithoutType\":\n" +
				"                                \"homersimpson\",\n" +
				"                              \"lang\":\n" +
				"                                \"en\",\n" +
				"                              \"surname\":\n" +
				"                                \"simpson\",\n" +
				"                              \"type\":\n" +
				"                                \"character\"\n" +
				"                            }\n" +
				"                        },\n" +
				"                      \"max_query_terms\":\n" +
				"                        12,\n" +
				"                      \"min_doc_freq\":\n" +
				"                        1,\n" +
				"                      \"min_term_freq\":\n" +
				"                        1\n" +
				"                    }\n" +
				"                }\n" +
				"            },\n" +
				"          \"must\":\n" +
				"            [\n" +
				"              {\n" +
				"                \"match\":\n" +
				"                  {\n" +
				"                    \"type\":\n" +
				"                      \"sometype\"\n" +
				"                  }\n" +
				"              }\n" +
				"            ]\n" +
				"        }\n" +
				"    }\n" +
				"}"
			),

		};

		Consumer<Case> runner = (aCase) -> {
			String queryString = (String) aCase.data[0];
			Document likeDoc = (Document) aCase.data[1];
			MustClause must = (MustClause) aCase.data[2];
			MustNotClause mustNot = (MustNotClause) aCase.data[3];
			ShouldClause should = (ShouldClause) aCase.data[4];
			String expJson = (String) aCase.data[5];

			try {
				Query gotQuery = new Query()
					.type("sometype", null)
					.queryString(queryString)
					.moreLikeThis(likeDoc, new StreamlinedClient(cartoonsTestIndex))
					.must(must)
					.mustNot(mustNot)
					.should(should);
				new AssertRequestBodyElement(gotQuery)
					.jsonEquals(expJson);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		};

		new RunOnCases(cases, runner)
//			.onlyCaseNums(7)
			.run();
	}
}
