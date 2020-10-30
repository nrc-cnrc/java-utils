package ca.nrc.config;

import ca.nrc.testing.AssertObject;
import ca.nrc.testing.AssertString;
import ca.nrc.testing.Asserter;

public class AssertConfig extends Asserter<Object> {
    public AssertConfig(String mess) {
        super(null, mess);
    }

    public AssertConfig assertConfigPropertyEquals(
        String expValue, String propName) throws Exception {
        return assertConfigPropertyEquals(expValue, propName, true);
    }

    public AssertConfig assertConfigPropertyEquals(
        String expValue, String propName, boolean failIfAbsent) throws Exception {
        String gotValue = Config.getConfigProperty(propName, failIfAbsent);
        AssertString.assertStringEquals(expValue, gotValue);
        return this;
    }

    public AssertConfig assertConfigPropertyEquals(
        String expValue, String propName, String defVal) throws Exception {
        String gotValue = Config.getConfigProperty(propName, defVal);
        AssertString.assertStringEquals(expValue, gotValue);
        return this;
    }


    public <T> AssertConfig assertConfigPropertyEquals(
            T expValue, String propName, Class<T> clazz) throws Exception {
        T gotValue = Config.getConfigProperty(propName, clazz);
        AssertObject.assertDeepEquals(
            "Congfig prop "+propName+" did not have the expected value",
            expValue, gotValue);
        return this;
    }

    public <T> AssertConfig assertParsedPropValueIs(T expValue,
        String propStr, Class<T> clazz) throws Exception {
        T gotValue = Config.parsePropValue("propName", propStr, clazz);
        AssertObject.assertDeepEquals(
            this.baseMessage+
            "\nDid not parse proper value for property string '"+propStr+"'",
            expValue, gotValue);
        return this;
    }
}
