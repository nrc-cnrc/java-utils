package ca.nrc.data.harvesting;

public class PageHarvester_JBrowserDriverTest extends PageHarvesterTest {

    @Override
    protected PageHarvester makeHarvesterToTest() {
        return new PageHarvester_JBrowserDriver();
    }
}
