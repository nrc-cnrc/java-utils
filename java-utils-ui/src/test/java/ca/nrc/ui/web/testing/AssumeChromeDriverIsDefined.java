package ca.nrc.ui.web.testing;

import ca.nrc.config.ConfigException;
import ca.nrc.ui.config.UIConfig;
import org.junit.Assume;

public class AssumeChromeDriverIsDefined {
    public static void 	assume() {
        boolean isDefined = true;
        try {
            new UIConfig().getChromeDriverPath();
        } catch (ConfigException e) {
            isDefined = false;
        }

        Assume.assumeTrue(
    "Property ca.nrc.ui.test.chromedriver was not defined anywhere.\n" +
            "Skipping all tests.\n" +
            "TO run those tests, install chromedriver and make that property point to its location.",
            isDefined);
    }
}
