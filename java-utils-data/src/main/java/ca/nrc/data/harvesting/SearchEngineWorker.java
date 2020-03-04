package ca.nrc.data.harvesting;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import ca.nrc.data.harvesting.SearchEngine.Hit;
import ca.nrc.data.harvesting.SearchEngine.Query;
import ca.nrc.data.harvesting.SearchEngine.SearchEngineException;
import ca.nrc.data.harvesting.SearchEngine.Type;
import ca.nrc.datastructure.Cloner;
import ca.nrc.datastructure.Cloner.ClonerException;
import ca.nrc.json.PrettyPrinter;

public class SearchEngineWorker implements Runnable {
	
	public static enum Status {START_FETCHING, FETCHING, NO_MORE_HITS, 
							   READY_TO_PULL, STOP, };
		
	private Status status = Status.START_FETCHING;
		public synchronized Status getStatus() { return status; }
		public synchronized void setStatus(Status _status) { this.status = _status; }
	
	public static final Hit NO_MORE_HITS = new Hit();
	public static final Hit WAIT_FOR_MORE = new Hit();	
	
	private boolean _cancel;
		public void cancel() {
			this._cancel = true;
		}
	
	Query query = null;
	String thrName = null;
	SearchEngine searchEngine = null;
	
	SearchResults currentBatch = null;
	
	Set<URL> prevBatchURLs = null;
	
	long estTotalHits = 0;
	
	SearchResultsCollector resultsCollector = null;
	
	private Thread thr;
	public Exception error;	
		
	public SearchEngineWorker(String _term, Query _query, String _threadName, 
			SearchEngine engineProto, SearchResultsCollector collector) throws SearchEngineException {
		Class<? extends SearchEngine> clazz = engineProto.getClass();		
		{
			try {
				this.query = Cloner.clone(_query);
			} catch (ClonerException e) {
				throw new SearchEngineException("Could not clone query:\n"+PrettyPrinter.print(query), e);
			}
			this.query.terms = new ArrayList<String>();
			this.query.terms.add(_term);
			this.query.hitsPageNum = 0;
			this.resultsCollector = collector;
			
			try {
				this.searchEngine = clazz.getConstructor().newInstance().setCheckHitLanguage(true);
				this.searchEngine.setCheckHitSummary(shouldCheckHitSummary());
			} catch (NoSuchMethodException | SecurityException | InstantiationException 
					| IllegalAccessException | IllegalArgumentException 
					| InvocationTargetException e) {
				throw new SearchEngineException("Cannot create a search engine of type: "+clazz.getName(), e);
			}
			
		}
		this.thrName = _threadName;
	}
	
	public boolean shouldCheckHitSummary()  { return searchEngine.shouldCheckHitLanguage();}
	
	public SearchEngineWorker setCheckHitSummary(boolean flag)  {
		this.searchEngine.setCheckHitSummary(flag);
		return this;
	}
	
	
	@Override
	public void run()  {
		Logger tLogger = Logger.getLogger("ca.nrc.data.harvesting.SearchEngineWorker.run");
		try {
			
			// Keep fetching new batches of results until someone tells the thread
			// to stop
			//
			while(true) {

				if (getStatus() == Status.STOP) {
//					System.out.println("** SearchEngineWorker.run: Worker '"+thrName+"' is STOPPING");
					break;
				}
				
				if (getStatus() == Status.START_FETCHING) {
//					System.out.println("** SearchEngineWorker.run: Worker '"+thrName+"' is FETCHING results for query=\n"+PrettyPrinter.print(query));
					setStatus(Status.FETCHING);
					fetchNextBatchOfResults();
//					System.out.println("** SearchEngineWorker.run: Worker '"+thrName+"' FETCHED a batch with a total of "+currentBatch.retrievedHits.size()+" hits");
				}
				
				
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		} catch (SearchEngineException e) {
			this.error = e;
			tLogger.trace("Worker '"+thrName+"' raised an exception: "+e.getMessage());
			return;
		}
	}
	
	public Hit pullHit() throws SearchEngineException {
		Hit hit = null;
		
		if (getStatus() == Status.STOP) {
			hit = NO_MORE_HITS;
		}
		
		if (hit == null && 
				(getStatus() == Status.FETCHING || 
				 getStatus() == Status.START_FETCHING)) {
			hit = WAIT_FOR_MORE;
		}
		
		if (hit == null && getStatus() == Status.READY_TO_PULL) {
			if (currentBatch.retrievedHits.size() > 0) {
				hit = currentBatch.retrievedHits.remove(0);
//				System.out.println("** SearchEngineWorker.pullHit: hit="+hit);
			} else {
				hit = WAIT_FOR_MORE;
				setStatus(Status.START_FETCHING);
			}
		}
				
		return hit;
	}
	
	private void fetchNextBatchOfResults() throws SearchEngineException {
//		System.out.println("** SearchEngineWorker.fetchNextBatchOfResults: Fetching batch number "+query.hitsPageNum);
		this.setStatus(Status.FETCHING);
		query.hitsPageNum++;
		this.currentBatch = searchEngine.search(query);
		setEstTotalHits(currentBatch.estTotalHits);
		
//		System.out.println("** SearchEngineWorker.fetchNextBatchOfResults: DONE Fetching batch number "+query.hitsPageNum);
		
		stopIfCurrBatchIsSameAsPrevious();
		if (this.currentBatch.retrievedHits.size() == 0) {
			setStatus(Status.STOP);
		}
	}

	private void stopIfCurrBatchIsSameAsPrevious() {
		boolean newURLsFound = false;
		Set<URL> urlsThisBatch = new HashSet<URL>();
		for (Hit hit: currentBatch.retrievedHits) {
			urlsThisBatch.add(hit.url);
			if (prevBatchURLs == null || !prevBatchURLs.contains(hit.url)) {
				newURLsFound = true;
			}
		}
		
		if (newURLsFound) {
			prevBatchURLs = urlsThisBatch;
			setStatus(Status.READY_TO_PULL);			
		} else {
			// This batch is EXACTLY the same as the previous batch.
			// This can happen with certain search engines, when you ask
			// for more batches than can actually be found on the internet.
			//
			setStatus(Status.STOP);
		}
		
	}

	public void start () {
	   if (thr == null) {
		   thr = new Thread (this, this.thrName);
		   thr.start ();
	   }
	}
	
	private synchronized void setEstTotalHits(long _estTotalHits) {
		this.estTotalHits = _estTotalHits;
	}

	public synchronized long getEstTotalHits() {
		return this.estTotalHits;
	}
}
