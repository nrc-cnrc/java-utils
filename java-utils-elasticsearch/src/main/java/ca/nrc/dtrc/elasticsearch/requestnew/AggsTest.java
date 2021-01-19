package ca.nrc.dtrc.elasticsearch.requestnew;

import ca.nrc.dtrc.elasticsearch.requestnew.AssertRequestBodyElement;
import org.junit.Test;

public class AggsTest {

    /////////////////////////////////////////////////
    // VERIFICATION TESTS
    /////////////////////////////////////////////////

    @Test
    public void test__Aggs__HappyPath() throws Exception {
        Aggs aggs = new Aggs()
            .aggregate("avgAge", "avg", "age")
            .aggregate("totalRevenue", "sum", "revenue")
            ;

        String expJson =
            "{\n"+
            "  \"aggs\": {\n"+
            "    \"avgAge\": {\"avg\": {\"field\": \"age\"}},\n"+
            "    \"totalRevenue\": {\"sum\": {\"field\": \"revenue\"}}\n"+
            "  }" +
            "}"
            ;

        new AssertRequestBodyElement(aggs)
                .jsonEquals(expJson);

    }
}
