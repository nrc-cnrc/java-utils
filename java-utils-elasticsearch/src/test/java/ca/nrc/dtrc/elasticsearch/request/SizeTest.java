package ca.nrc.dtrc.elasticsearch.request;

import org.junit.Test;

public class SizeTest {

    @Test
    public void test__Size__HappyPath() throws Exception {
        Size size = new Size(20);

        String expJson = "{\"size\": 20}";

        new AssertRequestBodyElement(size)
                .jsonEquals(expJson);
    }
}
