package ca.nrc.data.harvesting;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
	boolean stop = false;
	
	SearchResultsCollector resultsCollector = null;
	
	private Thread thr;
	public Exception error;	
		
	public SearchEngineWorker(String _term, Query _query, String _threadName, 
			SearchEngine engineProto) throws SearchEngineException {
		Class<? extends SearchEngine> clazz = engineProto.getClass();
		try {
			this.searchEngine = clazz.getConstructor().newInstance().setCheckHitLanguage(true);
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
		Logger tLogger = Logger.getLogger("ca.nrc.data.harvesting.SearchEngineWorker.run");
		try {
			if (tLogger.isTraceEnabled()) {
				tLogger.trace("Worker '"+thrName+"' started with query=\n"+PrettyPrinter.print(query));
			}
			this.results = searchEngine.search(query);
			if (tLogger.isTraceEnabled()) {
				tLogger.trace("Worker '"+thrName+"' retrieved a total of "+results.retrievedHits.size()+" hits");
			}
			if (resultsCollector != null) {
				resultsCollector.addResultsForWorker(this, results);
			}
		} catch (SearchEngineException e) {
			this.error = e;
			tLogger.trace("Worker '"+thrName+"' raised an exception: "+e.getMessage());
			return;
		}
	}
	
	public void start () {
	   if (thr == null) {
		   thr = new Thread (this, this.thrName);
		   thr.start ();
	   }
	}
	
	public synchronized boolean stillWorking() {
		
		Boolean isWorking = null;
		if (resultsCollector == null) {
			isWorking = thr.isAlive();
		} else {
			isWorking = !resultsCollector.workerProducedResults(this);
		}

		return isWorking;
	}

	public void setCollector(SearchResultsCollector collector) {
		resultsCollector = collector;
	}
	
	
}
