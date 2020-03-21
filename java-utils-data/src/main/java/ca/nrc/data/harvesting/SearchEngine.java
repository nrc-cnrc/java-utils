package ca.nrc.data.harvesting;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import ca.nrc.datastructure.Pair;
import ca.nrc.json.PrettyPrinter;

public abstract class SearchEngine {

protected abstract SearchResults searchRaw(Query query) throws SearchEngineException;
	
	private boolean checkHitLanguage = false;
		public boolean shouldCheckHitLanguage()  { return checkHitLanguage; }
		public SearchEngine setCheckHitLanguage(boolean flag)  {
			checkHitLanguage = flag;
			return this;
		}
	
	private boolean checkHitSummary = false;
		public boolean shouldCheckHitSummary()  { return checkHitSummary; }
		public SearchEngine setCheckHitSummary(boolean flag)  {
//			System.out.println("** SearchEngine.setCheckHitSummary: set to "+flag);
			checkHitSummary = flag;
			return this;
		}
	
	public SearchResults search(Query seQuery) throws SearchEngineException {
		Logger tLogger = Logger.getLogger("ca.nrc.data.harvesting.SearchEngine.search");

		if (tLogger.isTraceEnabled()) {
			tLogger.trace("Invoked with seQuery=\n"+PrettyPrinter.print(seQuery));
		}
		
		SearchResults results = searchRaw(seQuery);	
		List<Hit> rawHits = results.retrievedHits;
		tLogger.trace("Number of retrieved 'raw' hits: "+rawHits.size());

		List<Hit> filteredHits = filterRawHits(rawHits, seQuery);
		
		results.retrievedHits = filteredHits;
		
		tLogger.trace("Number of 'filtered' hits: "+filteredHits.size());

		return results;	
	}

	private List<Hit> filterRawHits(List<Hit> rawHits, Query seQuery) throws SearchEngineException {
		
//		System.out.println("** filterRawHits: shouldCheckHitLanguage()="+shouldCheckHitLanguage()+", shouldCheckHitSummary()="+shouldCheckHitSummary());
		
		List<Hit> filteredHits = rawHits;
		
		if (shouldCheckHitLanguage() || shouldCheckHitSummary()) {
			filteredHits = new ArrayList<Hit>();
			for (Hit aHit: rawHits) {
				Boolean pass = true;
				if (shouldCheckHitLanguage()) {
					if (!aHit.isInLanguage(seQuery.lang)) {
						pass = false;
					}
				}
				
				if (pass && shouldCheckHitSummary()) {
					pass = hitSummaryFitsQuery(aHit, seQuery);
				}
				
				if (pass) {
					filteredHits.add(aHit);
				}
			}
		}
		
		return filteredHits;
	}

	private Boolean hitSummaryFitsQuery(Hit aHit, Query seQuery) {
		System.out.println("** hitSummaryFitsQuery: looking at aHit.summary="+aHit.summary);
		boolean fitsQuery = true;
		
		if (seQuery.terms != null) {
			fitsQuery = false;
			String summary = aHit.summary.toLowerCase();
			for (String term: seQuery.terms) {
				term = term.toLowerCase();
				System.out.println("** hitSummaryFitsQuery: Checking term="+term+"\n  in summary="+summary);				
				if (summary.contains(term)) {
					fitsQuery = true;
					System.out.println("** hitSummaryFitsQuery: Summary contains the term");				
					break;
				}
			}
		}
		
		System.out.println("** hitSummaryFitsQuery: returning fitsQuery="+fitsQuery);				

		return fitsQuery;
	}

	public enum Type {
	    NEWS, ANY, BLOG
	}		
	
	public static class Query {
		
		public String fuzzyQuery = null;
		public List<String> terms = null;
		public Type[] types = new Type[] {Type.ANY};
		
		public Integer hitsPageNum = null;
		public Integer hitsPerPage = 10;
		public Integer maxHits = 10;
		public String lang = "en";
		
		private String site = null;				
		public String getSite() {return site;}
		
		private String inURL = null;
		public String getInURL() {
			return inURL;
		}
		public void setInURL(String inURL) {
			this.inURL = inURL;
		}

		public Query() {
			initialize(null, null);
		}
		
		public Query(String fuzzyQuery) {
			initialize(fuzzyQuery, null);
		}

		public Query(String[] terms) {
			List<String> termsList = new ArrayList<String>();
			for (String term: terms) termsList.add(term);
			initialize(null, termsList);
		}

		public Query(List<String> terms) {
			initialize(null, terms);
		}
		
		private void initialize(String _fuzzyQuery, List<String> _terms) {
			this.fuzzyQuery = _fuzzyQuery;
			this.terms = _terms;
		}

		public Query setType(Type type) {
			this.types = new Type[] {type};
			return this;
		}

		public Query setTypes(Type[] types) {
			this.types = types;
			return this;
		}		
		
		public Query setMaxHits(int numHits) {
			this.maxHits = numHits;
			this.hitsPageNum = null;
			return this;
		}
		
		public Query setSite(String _site) {
			this.site = _site;
			return this;
		}
		
		public Query setLang(String _lang) {
			this.lang = _lang;
			return this;
		}
		
		public Query setHitsPerPage(int _hitsPerPage) {
			this.hitsPerPage = _hitsPerPage; 
			return this;
		}
		public Query setHitsPageNum(int pageNum) {
			this.hitsPageNum = pageNum;
			this.maxHits = null;
			return this;
		}
		
		public Pair<Integer, Integer> computeFirstAndLastPage() {
			Integer first = new Integer(0);
			Integer last = null;
			if (maxHits != null) {
				last = new Long(Math.round(1.0 * maxHits / hitsPerPage)).intValue();
			} else {
				first = hitsPageNum;
				last = hitsPageNum;
			}
			return Pair.of(first, last);
		}
	}
	
	public static class SearchEngineException extends Exception {
		private static final long serialVersionUID = -2549689613326642612L;

		public SearchEngineException(Exception exc) {
			super(exc);
		}

		public SearchEngineException(String message, Exception exc) {
			super(message, exc);
		}

		public SearchEngineException(String message) {
			super(message);
		}
	}
	
	public static class Hit {
		public URL url;
		public String title;
		public String summary;
		public Long outOfTotal;
		
		public Hit() {
			
		}		
		
		public Hit(URL url, String title, String summary) {
			this.url = url;
			this.title = title;
			this.summary = summary;
		}
		

		public String toString() {
			String wholeContent = 
					"URL: "+this.url.toString().toLowerCase()+"\nTitle: "
					+this.title.toLowerCase() + "\nSummary: " + this.summary.toLowerCase();
			return wholeContent;
		}

		public boolean isInLanguage(String desiredLang) throws SearchEngineException {
			String actualLang;
			try {
				actualLang = new LanguageGuesser().detect(summary);
			} catch (LanguageGuesserException e) {
				throw new SearchEngineException(e);
			}
			boolean answer = true;
			if (actualLang != null && desiredLang != null) {
				answer = (actualLang.equals(desiredLang));
			} else {
				// On of the two languages is null. Is the other non-null?
				if (actualLang != null || desiredLang != null) {
					answer = false;
				}
			}
					
			return answer;
		}
	}
	
	public interface IHitVisitor {
		public void visitHit(Hit hit) throws Exception;
	}	
}
