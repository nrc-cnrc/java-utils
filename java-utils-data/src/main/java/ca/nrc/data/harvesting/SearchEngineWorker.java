package ca.nrc.data.harvesting;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import ca.nrc.data.harvesting.SearchEngine.Hit;
import ca.nrc.data.harvesting.SearchEngine.Query;
import ca.nrc.data.harvesting.SearchEngine.SearchEngineException;
import ca.nrc.data.harvesting.SearchEngine.Type;
import ca.nrc.datastructure.Cloner;
import ca.nrc.datastructure.Cloner.ClonerException;
import ca.nrc.json.PrettyPrinter;

public class SearchEngineWorker implements Runnable {
	
	Query query = null;
	String thrName = null;
	SearchEngine searchEngine = null;
	SearchResults results = null;
	
	private Thread thr;
	public Exception error;	
	
	public SearchEngineWorker(String _term, Query _query, String _threadName, SearchEngine engineProto) throws SearchEngineException {
		Class<? extends SearchEngine> clazz = engineProto.getClass();
		try {
			this.searchEngine = clazz.getConstructor().newInstance();
		} catch (NoSuchMethodException | SecurityException | InstantiationException 
				| IllegalAccessException | IllegalArgumentException 
				| InvocationTargetException e) {
			throw new SearchEngineException("Cannot create a search engine of type: "+clazz.getName(), e);
		}
		
		{
			try {
				this.query = Cloner.clone(_query);
			} catch (ClonerException e) {
				throw new SearchEngineException("Could not clone query:\n"+PrettyPrinter.print(query), e);
			}
			this.query.terms = new ArrayList<String>();
			this.query.terms.add(_term);
		}
		this.thrName = _threadName;
	}
	
	@Override
	public void run()  {
		System.out.println("** SearchEngineWorker[thr="+thrName+"]: started running");
		try {
			this.results = searchEngine.search(query);
		} catch (SearchEngineException e) {
			System.out.println("** SearchEngineWorker[thr="+thrName+"]: Caugh exception: "+e.getMessage());			
			this.error = e;
			return;
		}
		
		System.out.println("** SearchEngineWorker[thr="+thrName+"]: returning with #hits="+this.results.retrievedHits.size());		
	}
	
	public void start () {
	   if (thr == null) {
		   thr = new Thread (this, this.thrName);
		   thr.start ();
	   }
	}
	
	public boolean stillWorking() {
		return thr.isAlive();
	}
	
	

}
