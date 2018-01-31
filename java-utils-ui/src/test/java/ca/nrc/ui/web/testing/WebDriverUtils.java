package ca.nrc.ui.web.testing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

/*
 * Description
 * 		Utilities for processing web pages with WebDriver
 * 
 * For more information on how to use this class, see the 
 * DOCUMENTATION TESTS section of test case
 * WebDriverUtilsTest.
 */
public class WebDriverUtils {
	
	public WebDriver driver;
	
	private int resetWaitInMSecs;
	
	public WebDriverUtils(WebDriver driver, int resetWaitInMSecs) {
		this.resetWaitInMSecs = resetWaitInMSecs;
		this.driver = driver;
	}
	
	public WebDriverUtils(int resetWaitInMSecs) {
		this.resetWaitInMSecs = resetWaitInMSecs;
		this.driver = null;
	}
	
	public void setDriver(WebDriver driver) {
		this.driver = driver;
        driver.manage().timeouts().implicitlyWait(resetWaitInMSecs, TimeUnit.MILLISECONDS);
	}
	
	public ArrayList<String> extractTableInnerHTMLAsArray(By eltLocator){
		ArrayList<String> rows = new ArrayList<>();
		WebElement table = driver.findElement(eltLocator);
		for (WebElement row : table.findElements(By.tagName("tr"))){
			rows.add(row.getAttribute("innerHTML"));
		}		
		return rows;
	}
	
	public ArrayList<ArrayList<String>> extractTableTextAsArray(By eltLocator) {
//		logger.debug("WebDriverUtils.extractTableAsArray: invoked with eltLocator="+eltLocator);
		WebElement table = driver.findElement(eltLocator);
		List<WebElement> rows = table.findElements(By.tagName("tr"));
		ArrayList<ArrayList<String>> tableText = extractRowsText(rows);
		
		return tableText;
	}

	public ArrayList<ArrayList<String>> extractTableTextAsArray(By eltLocator, WebElement withinElement) {
		WebElement table = withinElement.findElement(eltLocator);
		List<WebElement> rows = table.findElements(By.tagName("tr"));
		ArrayList<ArrayList<String>> tableText = extractRowsText(rows);
		
		return tableText;
	}

	public ArrayList<ArrayList<String>> extractRowsText(List<WebElement> rows) {
		
		ArrayList<ArrayList<String>> rowsText = new ArrayList<ArrayList<String>>();

		for (WebElement aRow: rows) {
//			logger.debug("-- WebDriverUtils.extractRowsText: Looking at aRow="+aRow.getText());
			ArrayList<String> aRowText = new ArrayList<String>();

			// Note: We need to fetch both td and th elements.
			List<WebElement> cells = aRow.findElements(By.xpath("./*"));
			for (WebElement aCell: cells) {
				String aCellString = aCell.getText();
//				logger.debug("-- WebDriverUtils.extractRowsText: adding aCellString="+aCellString);
				aRowText.add(aCellString);
			}
			rowsText.add(aRowText);
	}
		
	return rowsText;
	}	

	public ArrayNode extractTableTextAsJson(By eltLocator) throws IOException {
//		logger.debug("-- WebDriverUtils.extractTableTextAsJson: invoked with eltLocator="+eltLocator);
		ArrayList<ArrayList<String>> tableAsArray = extractTableTextAsArray(eltLocator);
//		logger.debug("-- WebDriverUtils.extractTableTextAsJson: extracted tableAsArray="+tableAsArray);
		ObjectMapper mapper = new ObjectMapper();
		String tableAsJsonString = mapper.writeValueAsString(tableAsArray);
		ArrayNode  tableAsArrayNode = (ArrayNode)mapper.readTree(tableAsJsonString);

		return tableAsArrayNode;
	}

	public ArrayNode extractTableTextAsJson(By eltLocator, WebElement withinElement) throws IOException {
		ArrayList<ArrayList<String>> tableAsArray = extractTableTextAsArray(eltLocator, withinElement);
		ObjectMapper mapper = new ObjectMapper();
		String tableAsJsonString = mapper.writeValueAsString(tableAsArray);
		ArrayNode  tableAsArrayNode = (ArrayNode)mapper.readTree(tableAsJsonString);

		return tableAsArrayNode;
	}

	public boolean elementExists(By selector) {
		
		// Temporarily set the implicit wait to 1 millisecond
        driver.manage().timeouts().implicitlyWait(1, TimeUnit.MILLISECONDS);

		boolean exists;
	    try {
	        WebDriverWait wait = new WebDriverWait(driver, 1);
	        wait.until(ExpectedConditions.presenceOfElementLocated(selector));
	        exists = true;
	      } catch (TimeoutException e) {
	        exists = false;
	      }
	    
	    // Reset the implicit wait to its original value 
        driver.manage().timeouts().implicitlyWait(resetWaitInMSecs, TimeUnit.MILLISECONDS);
	    
	    return exists;
	}


}
