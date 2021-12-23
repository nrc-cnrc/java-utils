package ca.nrc.dtrc.elasticsearch.es5.request;

import org.junit.Test;

public class SortTest {

    /////////////////////////////////////////////////
    // VERIFICATION TESTS
    /////////////////////////////////////////////////

    @Test
    public void test__Sort__HappyPath() throws Exception {
        Sort sort = new Sort()
            .sortBy("age", Sort.Order.desc)
            .sortBy("revenue", Sort.Order.asc);

        String expJson =
            "{\n" +
            "  \"sort\": [\n" +
            "    {\"age\": \"desc\"},\n" +
            "    {\"revenue\": \"asc\"}\n" +
            "  ]\n" +
            "}"
            ;

        new AssertRequestBodyElement(sort)
            .jsonEquals(expJson);

    }
}
