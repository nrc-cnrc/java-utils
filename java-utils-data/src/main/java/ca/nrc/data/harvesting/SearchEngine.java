package ca.nrc.data.harvesting;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public abstract class SearchEngine {

	public abstract List<SearchEngine.Hit> search(Query query) throws SearchEngineException;
	
	public enum Type {
	    NEWS, ANY, BLOG
	}		
	
	public static class Query {
		
		public String fuzzyQuery = null;
		public List<String> terms = null;
		public Type[] types = new Type[] {Type.ANY};
		public int maxHits = 10;
		
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
			return this;
		}
		
		public Query setSite(String _site) {
			this.site = _site;
			return this;
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
	}
	
	public class Hit {
		public URL url;
		public String title;
		public String summary;
		
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
	}
	
	public interface IHitVisitor {
		public void visitHit(Hit hit);
	}
	
}
