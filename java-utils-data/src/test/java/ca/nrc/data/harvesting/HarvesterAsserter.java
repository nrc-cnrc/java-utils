package ca.nrc.data.harvesting;

import ca.nrc.testing.AssertString;
import ca.nrc.testing.Asserter;

public class HarvesterAsserter extends Asserter<PageHarvester> {
    public HarvesterAsserter(PageHarvester _harvester) {
        super(_harvester);
    }

    protected PageHarvester harvester() {
        return (PageHarvester)gotObject;
    }

    public void currentPageTitleContains(String expTitleText) throws Exception {
        String gotTitle = harvester().getTitle();
        AssertString.assertStringContains(
            baseMessage+"\nTitle of current page did not contain the expected text.",
            gotTitle, expTitleText);
    }
}
