package ca.nrc.data.harvesting;

/**
 * This class of PageHarvester is able to "click" on DOM elements that might
 * be JS enabled.
 * 
 * @author desilets
 *
 */
public abstract class PageHarvester_JSEnabled extends PageHarvester {
	
	/**
	 * Click on DOM element that matches a regexp, and return the content 
	 * of the resulting page.
	 * 
	 * @param eltRegexp
	 * @return
	 */
	public abstract void clickOn(String eltRegexp);

}
