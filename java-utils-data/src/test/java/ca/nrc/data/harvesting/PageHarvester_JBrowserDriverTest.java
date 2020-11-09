package ca.nrc.data.harvesting;

public class PageHarvester_JBrowserDriverTest extends PageHarvester_WebDriverTest {

    @Override
    protected PageHarvester makeHarvesterToTest() {
        return new PageHarvester_JBrowserDriver();
    }
}
