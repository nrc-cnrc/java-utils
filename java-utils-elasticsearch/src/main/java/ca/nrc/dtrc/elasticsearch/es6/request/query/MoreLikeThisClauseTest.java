package ca.nrc.dtrc.elasticsearch.es6.request.query;

import ca.nrc.dtrc.elasticsearch.es6.ESTestHelpers;
import static ca.nrc.dtrc.elasticsearch.es6.ESTestHelpers.*;
import ca.nrc.dtrc.elasticsearch.es6.StreamlinedClient;
import ca.nrc.dtrc.elasticsearch.es6.request.AssertRequestBodyElement;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class MoreLikeThisClauseTest {

	@BeforeAll
	public static void beforeAll() throws Exception {
//		new ESTestHelpers(7).skipTestsUnlessESIsRunning(7, 9207);
//		return;
		boolean skip = true;
		org.junit.Assume.assumeFalse(skip);
	}


	@Test
	public void test__MoreLikeThisClause() throws Exception {
		StreamlinedClient esClient = ESTestHelpers.makeCartoonTestIndex();
		ShowCharacter homer = new ShowCharacter("Homer", "Simpson", "The Simpsons");
		MoreLikeThisClause gotClause =
			new MoreLikeThisClause(homer, esClient);
		String expJson =
			"{\n" +
			"  \"more_like_this\":\n" +
			"    {\n" +
			"      \"highlight\":\n" +
			"        {\n" +
			"          \"fields\":\n" +
			"            {\n" +
			"              \"content\":\n" +
			"                {\n" +
			"                  \"type\":\n" +
			"                    \"plain\"\n" +
			"                },\n" +
			"              \"shortDescription\":\n" +
			"                {\n" +
			"                  \"type\":\n" +
			"                    \"plain\"\n" +
			"                }\n" +
			"            },\n" +
			"          \"order\":\n" +
			"            \"score\"\n" +
			"        },\n" +
			"      \"query\":\n" +
			"        {\n" +
			"          \"more_like_this\":\n" +
			"            {\n" +
			"              \"fields\":\n" +
			"                [\n" +
			"                  \"firstName\",\n" +
			"                  \"idWithoutType\",\n" +
			"                  \"lang\",\n" +
			"                  \"show\",\n" +
			"                  \"surname\",\n" +
			"                  \"type\"\n" +
			"                ],\n" +
			"              \"like\":\n" +
			"                {\n" +
			"                  \"_index\":\n" +
			"                    \"es-test-cartoons\",\n" +
			"                  \"doc\":\n" +
			"                    {\n" +
			"                      \"firstName\":\n" +
			"                        \"Homer\",\n" +
			"                      \"idWithoutType\":\n" +
			"                        \"HomerSimpson\",\n" +
			"                      \"lang\":\n" +
			"                        \"en\",\n" +
			"                      \"surname\":\n" +
			"                        \"Simpson\",\n" +
			"                      \"type\":\n" +
			"                        \"character\"\n" +
			"                    }\n" +
			"                },\n" +
			"              \"max_query_terms\":\n" +
			"                12,\n" +
			"              \"min_doc_freq\":\n" +
			"                1,\n" +
			"              \"min_term_freq\":\n" +
			"                1\n" +
			"            }\n" +
			"        }\n" +
			"    }\n" +
			"}";
		new AssertRequestBodyElement(gotClause)
			.jsonEquals(expJson);

	}

}
