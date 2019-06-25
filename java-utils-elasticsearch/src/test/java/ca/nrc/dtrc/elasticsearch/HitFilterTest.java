package ca.nrc.dtrc.elasticsearch;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import ca.nrc.datastructure.Pair;
import ca.nrc.dtrc.elasticsearch.Document;
import ca.nrc.dtrc.elasticsearch.HitFilter;
import ca.nrc.testing.AssertHelpers;

public class HitFilterTest {
	
	//////////////////////////////////////////////
	// DOCUMENTATION TESTS
	//////////////////////////////////////////////
	
	
	@Test
	public void test__HitFilter__Synopsis() throws Exception {
		//
		// The elasticsearch MLT (More Like This) search does not allow you to specify
		// a filter on the hits.
		//
		// This class allows you to easily define such a filter and apply as a post
		// processing step.
		//
		// The filter supports a limited subset of the ElasticSearch freeform query 
		// language.
		// 
		// For example, say you want are doing a MLT search on books and you want to
		// to keep all hits that have:
		//
		// - value of 2018 for the field 'year'
		//     AND
		// - value of 'sci-fi' for the field 'genre'
		//
		// You would create the filter as follows:
		//
		HitFilter filter = new HitFilter("+AND year:2018 genre:\"sci-fi\"");
		
		// 
		// This next filter will only keep books that do NOT fit the 
		// criteria (year:2018 AND genre:"sci-fi")
		//
		filter = new HitFilter("-AND year:2018 genre:\"sci-fi\"");
		
		// 
		// This next filter will only keep books that fit the 
		// criteria (year:2018 OR genre:"sci-fi")
		//
		filter = new HitFilter("+OR year:2018 genre:\"sci-fi\"");
		
		// 
		// This next filter will only keep books that DO NOT fit the 
		// criteria (year:2018 OR genre:"sci-fi")
		//
		filter = new HitFilter("-OR year:2018 genre:\"sci-fi\"");
		
		// 
		// Note that if you do not specify +/- or AND/OR, the filter
		//   assumes + and AND respective. For example, the following
		//   filter will only keep boooks that FIT the criteria
		//   (year:2018 AND genre:"sci-fi").
		filter = new HitFilter("year:2018 genre:\"sci-fi\"");
		
		// 
		// Once you have defined a filter, you can use to determine
		// if a particular Hit should be kept or not.
		//
		Hit<Document> aHit = new Hit<Document>(
				new Document()
					.setAdditionalField("author", "Melville")
					.setAdditionalField("title", "Moby Dick")
					.setAdditionalField("genre", "classical")
			);
		
		boolean keep = filter.keep(aHit);
	}
	
	
	//////////////////////////////////////////////
	// VERIFICATION TESTS
	//////////////////////////////////////////////
	

	@Test
	public void test__constructor__ExplicitPositiveExplicitAND() throws Exception {
		HitFilter filter = new HitFilter("+AND year:2018 genre:\"sci-fi\"");		
		Set<Pair<String,String>> expFields = new HashSet<Pair<String,String>>();
		{
			expFields.add(Pair.of("year", "2018"));
			expFields.add(Pair.of("genre", "sci-fi"));
		}
		
		this.assertFilterCharacteristics(filter, true, true, expFields);
	}

	@Test
	public void test__constructor__ExplicitPositiveExplicitOR() throws Exception {
		HitFilter filter = new HitFilter("+OR year:2018 genre:\"sci-fi\"");
		Set<Pair<String,String>> expFields = new HashSet<Pair<String,String>>();
		{
			expFields.add(Pair.of("year", "2018"));
			expFields.add(Pair.of("genre", "sci-fi"));
		}
		this.assertFilterCharacteristics(filter, true, false, expFields);		
	}

	@Test
	public void test__constructor__ExplicitPositiveImplicitAND() throws Exception {
		HitFilter filter = new HitFilter("+ year:2018 genre:\"sci-fi\"");
		Set<Pair<String,String>> expFields = new HashSet<Pair<String,String>>();
		{
			expFields.add(Pair.of("year", "2018"));
			expFields.add(Pair.of("genre", "sci-fi"));
		}
		this.assertFilterCharacteristics(filter, true, true, expFields);
		
	}
	

	@Test
	public void test__constructor__ImplicitPositiveExplicitAND() throws Exception {
		HitFilter filter = new HitFilter("AND year:2018 genre:\"sci-fi\"");
		Set<Pair<String,String>> expFields = new HashSet<Pair<String,String>>();
		{
			expFields.add(Pair.of("year", "2018"));
			expFields.add(Pair.of("genre", "sci-fi"));
		}
		this.assertFilterCharacteristics(filter, true, true, expFields);
		
	}

	@Test
	public void test__constructor__ImplicitPositiveExplicitOR() throws Exception {
		HitFilter filter = new HitFilter("OR year:2018 genre:\"sci-fi\"");
		Set<Pair<String,String>> expFields = new HashSet<Pair<String,String>>();
		{
			expFields.add(Pair.of("year", "2018"));
			expFields.add(Pair.of("genre", "sci-fi"));
		}
		this.assertFilterCharacteristics(filter, true, false, expFields);
		
	}

	@Test
	public void test__constructor__ImplicitPositiveImplicitAnd() throws Exception {
		HitFilter filter = new HitFilter("year:2018 genre:\"sci-fi\"");
		Set<Pair<String,String>> expFields = new HashSet<Pair<String,String>>();
		{
			expFields.add(Pair.of("year", "2018"));
			expFields.add(Pair.of("genre", "sci-fi"));
		}
		this.assertFilterCharacteristics(filter, true, true, expFields);
		
	}

	@Test
	public void test__filterDups__PositiveANDFilter() throws Exception {
		List<Hit<Document>> origHits = makeHitsList();
		HitFilter filter = new HitFilter("+AND additionalFields.lang:en additionalFields.author:Melville");
		List<String> gotFilteredIDs =  filterHits(filter, origHits);
		String[] expFilteredIDs = new String[] {"mobydick-en"};
		AssertHelpers.assertDeepEquals("Filtered hits not as expected", expFilteredIDs, gotFilteredIDs);
	}


	@Test
	public void test__filterDups__PositiveORFilter() throws Exception {
		List<Hit<Document>> origHits = makeHitsList();
		HitFilter filter = new HitFilter("+OR additionalFields.lang:en additionalFields.author:Melville");
		List<String> gotFilteredIDs =  filterHits(filter, origHits);
		String[] expFilteredIDs = new String[] {"mobydick-en","mobydick-fr", "lesmiserables-en", "hamlet-en"};
		AssertHelpers.assertUnOrderedSameElements("Filtered hits not as expected", expFilteredIDs, gotFilteredIDs);
	}


	@Test
	public void test__filterDups__NegativeANDFilter() throws Exception {
		List<Hit<Document>> origHits = makeHitsList();
		HitFilter filter = new HitFilter("-AND additionalFields.lang:en additionalFields.author:Melville");
		List<String> gotFilteredIDs =  filterHits(filter, origHits);
		String[] expFilteredIDs = new String[] {"mobydick-fr", "lesmiserables-en", "lesmiserables-fr", "hamlet-en", "hamlet-fr"};
		AssertHelpers.assertUnOrderedSameElements("Filtered hits not as expected", expFilteredIDs, gotFilteredIDs);
	}


	@Test
	public void test__filterDups__NegativeORFilter() throws Exception {
		List<Hit<Document>> origHits = makeHitsList();
		HitFilter filter = new HitFilter("-OR additionalFields.lang:en additionalFields.author:Melville");
		List<String> gotFilteredIDs =  filterHits(filter, origHits);
		String[] expFilteredIDs = new String[] {"lesmiserables-fr", "hamlet-fr"};
		AssertHelpers.assertUnOrderedSameElements("Filtered hits not as expected", expFilteredIDs, gotFilteredIDs);
	}
	

	@Test
	public void test__filterDups__EmptyFilter() throws Exception {
		List<Hit<Document>> origHits = makeHitsList();
		HitFilter filter = new HitFilter();
		List<String> gotFilteredIDs =  filterHits(filter, origHits);
		String[] expFilteredIDs = new String[] {"mobydick-en", "mobydick-fr", "lesmiserables-fr", "lesmiserables-en", "hamlet-en", "hamlet-fr"};
		AssertHelpers.assertUnOrderedSameElements("Filtered hits not as expected", expFilteredIDs, gotFilteredIDs);
	}
	
	@Test
	public void test__constructor__SpacesInFieldName() throws Exception {
		HitFilter filter = new HitFilter("genre:\"Science Fiction\"");		
		Set<Pair<String,String>> expFields = new HashSet<Pair<String,String>>();
		{
			expFields.add(Pair.of("genre", "Science Fiction"));
		}
		
		this.assertFilterCharacteristics(filter, true, true, expFields);
	}

	
	//////////////////////////////////////////////
	// TEST HELPERS
	//////////////////////////////////////////////
	
	
	private List<Hit<Document>> makeHitsList() {
		List<Hit<Document>> hits = new ArrayList<Hit<Document>>();
		{
			hits.add(new Hit<Document>(
							new Document("mobydick-en")
								.setAdditionalField("author", "Melville")
								.setAdditionalField("title", "Moby Dick")
								.setAdditionalField("lang", "en")
						));
			hits.add(new Hit<Document>(
					new Document("mobydick-fr")
						.setAdditionalField("author", "Melville")
						.setAdditionalField("title", "Moby Dick")
						.setAdditionalField("lang", "fr")
				));
			hits.add(new Hit<Document>(
						new Document("lesmiserables-fr")
						.setAdditionalField("author", "Hugo")
						.setAdditionalField("title", "Les Misérables")
						.setAdditionalField("lang", "fr")
					));
			hits.add(new Hit<Document>(
					new Document("lesmiserables-en")
					.setAdditionalField("author", "Hugo")
					.setAdditionalField("title", "Les Misérables")
					.setAdditionalField("lang", "en")
				));
			hits.add(new Hit<Document> (
						new Document("hamlet-en")
						.setAdditionalField("author", "Shakespeare")
						.setAdditionalField("title", "Hamlet")
						.setAdditionalField("lang", "en")
					));
			hits.add(new Hit<Document> (
					new Document("hamlet-fr")
					.setAdditionalField("author", "Shakespeare")
					.setAdditionalField("title", "Hamlet")
					.setAdditionalField("lang", "fr")
				));
		}
		
		return hits;
	}
	
	private void assertFilterCharacteristics(HitFilter filter, boolean expPositive, boolean expAnded, 
			Set<Pair<String,String>> expFields) throws IOException {
		Assert.assertEquals("Value of 'filterIsPositive' not as expected", expPositive, filter.filterIsPositive);
		Assert.assertEquals("Value of 'termsAreANDed' not as expected", expAnded, filter.termsAreANDed);
		AssertHelpers.assertDeepEquals("Terms of the filter were not as expected", expFields, filter.terms);

	}

	private List<String> filterHits(HitFilter filter, List<Hit<Document>> origHits) throws HitFilterException {
		List<String> filteredIDs = new ArrayList<String>();
		for (Hit<Document> aHit: origHits) {
			if (filter.keep(aHit)) {
				filteredIDs.add(aHit.getDocument().getId());
			}
		}

		return filteredIDs;
	}


}
