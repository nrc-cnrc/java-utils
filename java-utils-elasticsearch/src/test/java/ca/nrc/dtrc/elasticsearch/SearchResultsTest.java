package ca.nrc.dtrc.elasticsearch;


import java.util.Iterator;

import org.junit.Assert;
import org.junit.Test;

import ca.nrc.dtrc.elasticsearch.ESTestHelpers.PlayLine;
import ca.nrc.json.PrettyPrinter;
import ca.nrc.testing.AssertHelpers;

public class SearchResultsTest {
	
	///////////////////////////////
	// DOCUMENTATION TESTS
	///////////////////////////////

	@Test
	public void test__SearchResult__Synopsis() throws Exception {
		StreamlinedClient client = ESTestHelpers.makeHamletTestClient();
		
		// SearchResults are produced as a result of some search. For example:
		String query = "content:kingdom";
		SearchResults<PlayLine> results = client.searchFreeform(query, new PlayLine());
		
		// You can loop through the search results as follows..
		Iterator<Hit<PlayLine>> iterator = results.iterator();
		while (iterator.hasNext()) {
			Hit<PlayLine> aHit = iterator.next();
			Document doc = aHit.getDocument();
			Double relevanceScore = aHit.getScore();
		}
		
		// You can also attach a HitFilter to a SearchResult to only loop through
		// hits that meet certain criteria.
		//
		// This is mostly useful for similarity searches (moreLikeThis and moreLikeThese) which
		// do not allow you to set hard criteria on the similar docs you want. For example, say
		// you want a PlayLine whose content is similar to "my kingdom for a horse", but you want
		// to ensure that the hits have speaker:hamlet. You would do it as follows.
		//
		PlayLine line = new PlayLine("my kingdom for a horse");
		results = client.moreLikeThis(line);
		results.setFilter(new HitFilter("additionalFields.speaker:hamlet"));
		iterator = results.iterator();
		while (iterator.hasNext()) {
			Hit<PlayLine> aHit = iterator.next();
			// etc...
		}

		// You can aslo ask for some aggregations to be performed on some of
		// the fields of the hits.
		//
		Assert.fail("Document this new feature");
	}

	///////////////////////////////
	// VERIFICATION TESTS
	///////////////////////////////
	
	@Test
	public void test__setFilter__HappyPath() throws Exception {
		StreamlinedClient client = ESTestHelpers.makeHamletTestClient();
		Thread.sleep(1*1000);
		
		String query = "+content:kingdom";
		SearchResults<PlayLine> results = client.searchFreeform(query, new PlayLine());
		results.setFilter(new HitFilter("additionalFields.speaker:\"PRINCE FORTINBRAS\""));
		assertLinesSpokenBy(results, "PRINCE FORTINBRAS");
	}
	
	///////////////////////////////
	// TEST HELPERS
	///////////////////////////////
	

	private void assertLinesSpokenBy(SearchResults<PlayLine> results, String expSpeaker) {
		int countdown = 100;
		boolean noHits = true;
		Iterator<Hit<PlayLine>> iterator = results.iterator();
		while (countdown > 0 && iterator.hasNext()) {
			noHits = false;
			countdown--;
			PlayLine line = iterator.next().getDocument();
			String gotSpeaker = (String) line.getAdditionalField("speaker");
			
			AssertHelpers.assertStringEquals(
				"Line was spoken by wrong speaker\n"+
				"Line was: "+PrettyPrinter.print(line),
				expSpeaker, gotSpeaker);
		}
		
		if (noHits) {
			Assert.fail("The search results were empty.");
		}
	}
	
}
