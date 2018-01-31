package ca.nrc.ui.web.testing;

/**
 * Utilities used when comparing data from acceptance/unit tests
 */
public class ComparisonUtils {

	/**
	 * Creates a string used to compare the expected results from training or querying with what was is displayed on the page
	 * @param expectedLabels Labels expected on page, in order
	 * @param expectedURLs URLs expected in results, in order 
	 * @return expectedValues 3 dimensional array containing values for each web page
	 *  					  Each index in the array must correspond to the index/URL in the expectedURLs array.
	 * 						  Each sub-array contains relations (represented as arrays) found on that web page. 
	 * 						  Each index in a relation array must correspond to the indexes in the expectedLabels array.
	 */
	public static String generateExpectedResultsString(String[] expectedLabels, String[] expectedURLs, String[][][] expectedValues){
		StringBuilder builder = new StringBuilder("[");
		
		//for each URL
		for(int indexURL = 0; indexURL < expectedURLs.length; indexURL++){    
			builder.append("  {    \"relations\": [[");
				
			//for each label
			for(int indexLabel = 0; indexLabel < expectedLabels.length; indexLabel++){
				builder.append("\"");
				builder.append(expectedLabels[indexLabel]);
				builder.append("\"");
				
				if(indexLabel < expectedLabels.length - 1){
					builder.append(",");
				}
			}
			
			builder.append("]");
			
			if(expectedValues[indexURL].length > 0){
				builder.append(",");
			}
	
			//for each relation expected to be found at the web page
			for(int indexRelation = 0; indexRelation < expectedValues[indexURL].length; indexRelation++){
				builder.append("[");
						
				//for each value expected in the relation
				for(int indexRelationValue = 0; indexRelationValue < expectedValues[indexURL][indexRelation].length; indexRelationValue++){
					builder.append("\"");				
					builder.append(expectedValues[indexURL][indexRelation][indexRelationValue]);
					builder.append("\"");
					
					if(indexRelationValue < expectedValues[indexURL][indexRelation].length - 1){
						builder.append(",");
					}
				}
								
				builder.append("]");
				if(indexRelation < expectedValues[indexURL].length - 1){
					builder.append(",");
				}				
			}
			
			builder.append("],    \"url\": \"");
			builder.append(expectedURLs[indexURL]);
			builder.append("\"  }");

			if(indexURL < expectedURLs.length - 1){
				builder.append(",");
			}
		}
		builder.append("]");
		
		return builder.toString();
	}
}
