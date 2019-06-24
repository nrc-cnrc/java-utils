package ca.nrc.dtrc.elasticsearch;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ca.nrc.dtrc.elasticsearch.Document;
import ca.nrc.dtrc.elasticsearch.MLT_HitsFilter;

public class MLT_HitsFilterTest {

	@Test
	public void test__filterDups__PositiveFilter() {
		List<Document> origDups = new ArrayList<Document>();
		{
			origDups.add(new Document()
							.setAdditionalField("author", "Melville")
							.setAdditionalField("title", "Moby Dick")
							.setAdditionalField("lang", "en")
						);
			origDups.add(new Document()
					.setAdditionalField("author", "Hugo")
					.setAdditionalField("title", "Les Misérables")
					.setAdditionalField("lang", "fr")
				);
			origDups.add(new Document()
					.setAdditionalField("author", "Shakespeare")
					.setAdditionalField("title", "Hamlet")
					.setAdditionalField("lang", "en")
				);
		}
		
		MLT_HitsFilter filter = new MLT_HitsFilter("+ additionalFields.lang:en");
		List<Document> gotFilteredDups = filter.filter(origDups);
		
		Assert.fail("Implement verifications on this test");
		
	}


}
