package ca.nrc.dtrc.elasticsearch.es5.request;

import org.junit.Test;

public class HighlightTest {

    ////////////////////////////////////
    // VERIFICATION TESTS
    ////////////////////////////////////

    @Test
    public void test__Highlight__HappyPath() throws Exception {
        Highlight highlight = new Highlight();
        highlight
                .hihglightField("abstract")
                .hihglightField("content");

        String expJson =
            "{\n" +
            "  \"highlight\": {\n" +
            "    \"fields\": {\n" +
            "      \"abstract\": {},\n" +
            "      \"content\": {}\n" +
            "    }\n" +
            "  }\n" +
            "}"
            ;

        new AssertRequestBodyElement(highlight)
            .jsonEquals(expJson);
    }
}
