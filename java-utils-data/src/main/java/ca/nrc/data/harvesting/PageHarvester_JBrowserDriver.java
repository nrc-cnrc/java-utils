package ca.nrc.data.harvesting;

import com.machinepublishers.jbrowserdriver.JBrowserDriver;
import org.htmlcleaner.HtmlCleaner;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class PageHarvester_JBrowserDriver extends PageHarvester_WebDriver {

    @Override
    protected WebDriver makeDriver() throws PageHarvesterException {
        return new JBrowserDriver();
    }

    public static void main(String[] args) throws Exception {
        JBrowserDriver driver = new JBrowserDriver();
        HtmlCleaner cleaner = new HtmlCleaner();
            driver.get("https://gov.nu.ca/");
//            Thread.sleep(5 * 1000);
            System.out.println("Status: " + driver.getStatusCode());
            String origHtml = driver.getPageSource();
            System.out.println("Original page Html:\n" + origHtml);
            String origText = cleaner.clean(origHtml).getText().toString();
            System.out.println("Original page Text:\n" + origText);


            String anchorText = "Read More";
            WebElement link = driver.findElementByLinkText(anchorText);
            System.out.println("Link with anchor='"+anchorText+"'="+link.toString());

            link.click();
            String newHtml = driver.getPageSource();
            System.out.println("Second page Html:\n" + newHtml);
            String newText = cleaner.clean(newHtml).getText().toString();
            System.out.println("Second page Text:\n" + newText);
            String url = driver.getCurrentUrl();
            System.out.println("Second page URL: "+url);

            System.out.println("DONE");
            driver.close();
    }
}
