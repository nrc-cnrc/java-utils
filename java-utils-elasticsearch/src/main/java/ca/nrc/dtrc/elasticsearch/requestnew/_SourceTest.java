package ca.nrc.dtrc.elasticsearch.requestnew;

import ca.nrc.dtrc.elasticsearch.requestnew.AssertRequestBodyElement;
import org.junit.Test;

public class _SourceTest {
    @Test
    public void test__Source__HappyPath() throws Exception {
        _Source source = new _Source("name", "age");

        String expJson = "{\"_source\": [\"name\", \"age\"]}";

        new AssertRequestBodyElement(source)
                .jsonEquals(expJson);
    }

}
