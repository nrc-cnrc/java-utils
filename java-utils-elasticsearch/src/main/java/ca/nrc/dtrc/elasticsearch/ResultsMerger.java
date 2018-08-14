package ca.nrc.dtrc.elasticsearch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.SerializationUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;

import ca.nrc.datastructure.Pair;

/***************************************************************
 * 
 * Use this class to merge two lists of Search results.
 * 
 * For details on how to use it, see the DOCUMENTATION TESTS
 * in ResultsMergerTest.
 *
 ***************************************************************/

public class ResultsMerger<T extends Document> {

	public List<Hit<Document>> mergeHits(SearchResults<Document> hits1, SearchResults<Document> hits2) {
		return mergeHits(hits1, hits2, null);
	}
	
	public List<Hit<Document>> mergeHits(SearchResults<Document> hits1, SearchResults<Document> hits2, Integer maxHits) {
		normalizeScores(hits1);
		normalizeScores(hits2);
		Map<String,Pair<Hit<Document>,Hit<Document>>> hitsWithID = new HashMap<String,Pair<Hit<Document>,Hit<Document>>>();
		
		// Compile hits in hits1
		{
			int counter = 0;
			for (Hit<Document> aHit: hits1) {
				counter++;
				if (maxHits != null && counter > maxHits) break;
				String docID = aHit.getDocument().getId();
				Pair<Hit<Document>,Hit<Document>> entry = null;
				if (!hitsWithID.containsKey(docID)) {
					entry = Pair.of(null, null);
					hitsWithID.put(docID, entry);
				} else {
					entry = hitsWithID.get(docID);
				}
				entry.setFirst(aHit);
			}
			
		}

		// Compile hits in hits2
		{
			int counter = 0;
			for (Hit<Document> aHit: hits2) {
				counter++;
				if (maxHits != null && counter > maxHits) break;
				String docID = aHit.getDocument().getId();
				Pair<Hit<Document>,Hit<Document>> entry = null;
				if (!hitsWithID.containsKey(docID)) {
					entry = Pair.of(null, null);
					hitsWithID.put(docID, entry);
				} else {
					entry = hitsWithID.get(docID);
				}
				entry.setSecond(aHit);
			}
		}
		
		// Create a new list with all documents from hits1 and hits2, with score being
		// the average of the score in both lists
		List<Hit<Document>> mergedHits = new ArrayList<Hit<Document>>();
		{
			for (String docID: hitsWithID.keySet()) {
				Pair<Hit<Document>,Hit<Document>> entry = hitsWithID.get(docID);
				Double score = 0.0;
				Hit<Document> rescoredDoc = null;
				int presentInLists = 0;
				if (entry.getFirst() != null) {
					presentInLists++;
					score += entry.getFirst().getScore();
					rescoredDoc = entry.getFirst();
				}
				if (entry.getSecond() != null) {
					presentInLists++;
					score += entry.getSecond().getScore();
					rescoredDoc = entry.getSecond();
				}
				
				score = score / 2;
				if (presentInLists == 2) {
					// If the hit was in both lists, them bump up its 
					// score by 3, which in this case equals 3 std deviations
					score += 3;
				}
				rescoredDoc.setScore(score);
				mergedHits.add(rescoredDoc);
			}
		}
		
		// Sort the list according to the new merged scores
		//
		{
			Collections.sort(mergedHits, (h1, h2) -> h2.getScore().compareTo(h1.getScore()));
		}
		
//		return mergedHits;
		List<Hit<Document>> finalList;
		if (maxHits == null) {
			finalList = mergedHits;
		} else {
			finalList = mergedHits.subList(0, Math.min(maxHits, mergedHits.size()));
		}
		return finalList;
	}

	@JsonIgnore
	public ScoreDistribution getScoreDistribution(SearchResults<Document> hits) {
		double sumScores = 0.0;
		double sumScoresSquared = 0.0;
		int numObs = 0;
		Double maxValue = null;
		Double minValue = null;
		Iterator<Hit<Document>> iter = hits.iterator();
		while(iter.hasNext()) {
			numObs++;
			double score = iter.next().getScore();
			if (maxValue == null || maxValue < score) {
				maxValue = score;
			}
			if (minValue == null || minValue > score) {
				minValue = score;
			}
			sumScores += score;
			sumScoresSquared += score*score;
		}
		
		
		ScoreDistribution distr = new ScoreDistribution();
		distr.setN(numObs);
		distr.setMinValue(minValue);
		distr.setMaxValue(maxValue);
		if (numObs > 0) {
			double average = sumScores /  numObs;
			double avgOfSquares = sumScoresSquared / numObs;
			distr.setAverage(average);
			distr.setVariance(avgOfSquares - average*average);
		}
		
		return distr;
	}
	
	public void normalizeScores(SearchResults<Document> hits) {
		List<Hit<Document>> normalizedHits = new ArrayList<Hit<Document>>();
		ScoreDistribution distr1 = getScoreDistribution(hits);
		
		Double stdDev = Math.sqrt(distr1.variance);	
		Double avg = distr1.getAverage();
		Iterator<Hit<Document>> iter = hits.iterator();
		while (iter.hasNext()) {
			Hit<Document> origHit = iter.next();
			Double origScore = origHit.getScore();
			Double normScore  = null;
			if (stdDev > 0) {
				normScore = (origScore - avg) / stdDev;
			} else {
				// If stdDev == 0, then all the scores are the same.
				// So set them all the the normalized mean of 0
				normScore = 0.0;
			}
			origHit.setScore(normScore);
		}
	}	

}
