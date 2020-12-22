package ca.nrc.dtrc.elasticsearch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ca.nrc.datastructure.Pair;
import ca.nrc.testing.AssertHelpers;

public class ResultsMergerTest {
	
	ResultsMerger<Document> merger = null;
	SearchResults<Document> mltHits = null;
	SearchResults<Document> bQueryHits = null;
	@Before
	public void setUp() throws Exception {
		merger = new ResultsMerger<Document>();		
		mltHits = new SearchResults<Document>();
		{
			mltHits.addHit(new Document("mltOnlyDoc1"), 10.3, null);
			mltHits.addHit(new Document("mltOnlyDoc2"), 5.36, null);
			mltHits.addHit(new Document("mltAndBQueryDoc"), 2.89, null);
		}
		bQueryHits = new SearchResults<Document>();
		{
			bQueryHits.addHit(new Document("mltAndBQueryDoc"), 0.67, null);
			bQueryHits.addHit(new Document("bQueryOnlyDoc1"), 0.35, null);
			bQueryHits.addHit(new Document("bQueryOnlyDoc2"), 7.90, null);
		}
	}
	
	/*******************************************************************
	 * DOCUMENTATION TESTS
	 ******************************************************************/
	
	@Test
	public void test__Synopsis() throws Exception {
		//
		// Use ResultsMerger to merge two sets of SearchResults. 
		//
		// For example, say you want to search for documents that
		// fit a query, but want to sort them not only in terms of
		// their relevance to the query, but also to how similar
		// the documents are to an input document.
		//
		// In that situation, you would do a more-like-this query
		// with the input document and a regular search with the
		// input query. Then you would merge the results, taking
		// into accout the scores of both lists.
		//
		SearchResults<Document> mltHits = new SearchResults<Document>();
		{
			mltHits.addHit(new Document("mltOnlyDoc"), 10.3, null);
			mltHits.addHit(new Document("mltAndBQueryDoc"), 2.89, null);
		}
		SearchResults<Document> bQueryHits = new SearchResults<Document>();
		{
			bQueryHits.addHit(new Document("mltAndBQueryDoc"), 0.67, null);
			bQueryHits.addHit(new Document("bQueryOnlyDoc"), 0.35, null);
		}
		
		//
		// You would merge them as follows
		//
		ResultsMerger<Document> merger = new ResultsMerger<Document>();
		List<Hit<Document>> mergedHits = merger.mergeHits(mltHits, bQueryHits);
	}

	/*******************************************************************
	 * VERIFICATION TESTS
	 ******************************************************************/

	@Test
	public void test__mergeHits__HappyPath() throws Exception {
		List<Hit<Document>> gotMerged = merger.mergeHits(mltHits, bQueryHits);
		
		List<Pair<String,Double>> expHits = new ArrayList<Pair<String,Double>>();
		{
			expHits.add(Pair.of("mltAndBQueryDoc", 2.14));
			expHits.add(Pair.of("bQueryOnlyDoc2", 0.71));
			expHits.add(Pair.of("mltOnlyDoc1", 0.67));
			expHits.add(Pair.of("mltOnlyDoc2", -0.13));
			expHits.add(Pair.of("bQueryOnlyDoc1", -0.38));
		}
		assertHitsAre(expHits, gotMerged);
	}
	

	@Test
	public void test__getScoreDistribution__HappyPath() {
		ScoreDistribution gotDistr = merger.getScoreDistribution(mltHits);
		assertDistributionEquals(3, 6.183, 9.490, 2.89, 10.3, gotDistr);
	}
	
	@Test
	public void test__normalizeScores__HappyPath() throws Exception {
		merger.normalizeScores(mltHits);
		List<Double> expScores = new ArrayList<Double>();
		{
			expScores.add(1.336); expScores.add(-0.267); expScores.add(-1.069);
			
		}
		assertScoresAre(expScores, mltHits);
	}

	@Test
	public void test__normalizeScores__ListHasZeroVariance() throws Exception {
		SearchResults<Document> hitsWithZeroVariance = new SearchResults<Document>();
		{
			hitsWithZeroVariance.addHit(new Document("doc1"), 32.0, null);
			hitsWithZeroVariance.addHit(new Document("doc2"), 32.0, null);
		}
		merger.normalizeScores(hitsWithZeroVariance);
		List<Double> expScores = new ArrayList<Double>();
		{
			expScores.add(0.0); expScores.add(0.0);
			
		}
		assertScoresAre(expScores, hitsWithZeroVariance);
	}
	
	
	/*******************************************************************
	 * TEST HELPERS
	 ******************************************************************/
	
	private void assertDistributionEquals(int expN, double expAvg, 
			double expVariance, double expMin, double expMax, ScoreDistribution gotDistr) {
		
		Assert.assertEquals("Number of observations was not as expected", expN, gotDistr.getN());
		double tolerance = 0.001;
		Assert.assertEquals("Average was not as expected", expAvg, gotDistr.getAverage(), tolerance);
		Assert.assertEquals("Variance was not as expected", expVariance, gotDistr.getVariance(), tolerance);
		Assert.assertEquals("Max value was not as expected", expMax, gotDistr.getMaxValue(), tolerance);
		Assert.assertEquals("Min value was not as expected", expMin, gotDistr.getMinValue(), tolerance);
	}

	private void assertScoresAre(List<Double> expScores, SearchResults<Document> hits) throws Exception {
		List<Double> gotScores = new ArrayList<Double>();
		Iterator<Hit<Document>> iter = hits.iterator();
		while (iter.hasNext()) {
			gotScores.add(iter.next().getScore());
		}
		Integer decimalsTolerance = 3;
		AssertHelpers.assertDeepEquals("Scores were not as expected", expScores, gotScores, null, decimalsTolerance);
	}
	
	private void assertHitsAre(List<Pair<String, Double>> expHits, List<Hit<Document>> gotHitsRaw) throws IOException {
		List<Pair<String,Double>> gotHits = new ArrayList<Pair<String,Double>>();
		for (Hit<Document> aHit: gotHitsRaw) {
			gotHits.add(Pair.of(aHit.getDocument().getId(), aHit.getScore()));
		}
		
		Integer decimalsTolerance = 2;
		AssertHelpers.assertDeepEquals("Merged documents were not as expected", expHits, gotHits, 
				new HashSet<String>(), decimalsTolerance);
	}

}
