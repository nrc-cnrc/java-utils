package ca.nrc.data.harvesting;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectWriter.GeneratorSettings;

import ca.nrc.datastructure.Pair;

public abstract class SearchEngine {

	public abstract List<SearchEngine.Hit> search(Query query) throws SearchEngineException;
	
	private boolean checkHitLanguage = true;
		public boolean shouldCheckHitLanguage()  { return checkHitLanguage; }
		public SearchEngine setCheckHitLanguage(boolean flag)  {
			checkHitLanguage = flag;
			return this;
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

		public boolean isInLanguage(String desiredLang) throws IOException {
			String actualLang = LanguageGuesser.detect(summary);
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
