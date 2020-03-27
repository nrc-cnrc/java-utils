package ca.nrc.data.harvesting;

import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class PageHarvester_HtmlUnitTest extends PageHarvesterTest {

	@Override
	protected PageHarvester makeHarvesterToTest() {
		return new PageHarvester_HtmlUnit();
	}	
}
