package ca.nrc.dtrc.elasticsearch.es7;

import ca.nrc.dtrc.elasticsearch.DocIDIterator;
import ca.nrc.dtrc.elasticsearch.ESFactory;
import ca.nrc.dtrc.elasticsearch.ESTestHelpers;
import static ca.nrc.dtrc.elasticsearch.ESTestHelpers.PlayLine;

import ca.nrc.dtrc.elasticsearch.SearchResults;
import ca.nrc.dtrc.elasticsearch.es7mi.ES7miFactory;
import ca.nrc.testing.AssertNumber;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * This class compares the speed of the es7mi vs es7 apprach.
 * - es7mi: Stores different types of documents in different indices
 * - es7: Stores all types in same index, but with a 'type' field to differenciate them
 */
public class SpeedComparison__MultiVersuSingleIndexTest {

	protected ESFactory esFactory(String indexName, Boolean multiIndex)
		throws Exception {
		ESFactory factory = null;
		if (multiIndex) {
			factory = new ES7miFactory(indexName);
		} else {
			factory = new ES7Factory(indexName);
		}
		return factory;
	}

	@Test
	public void test__listAll__SpeedComparison() throws Exception {
		Assertions.fail("TODO: Implement this test");
		ESFactory factoryMultiIndex = new ESTestHelpers(7, true).makeHamletTestIndex();
		ESFactory factorySigleIndex = new ESTestHelpers(7, false).makeHamletTestIndex();

		Map<ESFactory,Long> elapsedTimes = new HashMap<ESFactory,Long>();
		for (ESFactory esFactory: new ESFactory[] {factoryMultiIndex, factoryMultiIndex}) {
			PlayLine proto = new PlayLine();
			Long startTime = System.currentTimeMillis();
			try(SearchResults<PlayLine> results =
				 esFactory.indexAPI().listAll(proto)) {
				DocIDIterator<PlayLine> iter = results.docIDIterator();
				while (iter.hasNext()) {
					iter.next();
				}
			}
			Long endTime = System.currentTimeMillis();
			Long elapsedTime = endTime - startTime;
			elapsedTimes.put(esFactory, elapsedTime);
		}

		double gotRatio = 1.0 * elapsedTimes.get(factoryMultiIndex) / elapsedTimes.get(factorySigleIndex);
		double expMinRatio = 1000;
//		System.out.println("batchSize=null  : "+elapsedTimes.get(null));
//		System.out.println("batchSize=1000  : "+elapsedTimes.get(new Integer(1000)));
//		System.out.println("null/1000 ratio : "+gotRatio);
		AssertNumber.isGreaterOrEqualTo(
			"listAll  with multi-index should have been much faster than single-index approach",
			gotRatio, expMinRatio);
	}
}
