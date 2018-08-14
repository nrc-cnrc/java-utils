package ca.nrc.dtrc.stats;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import ca.nrc.datastructure.Pair;
import ca.nrc.datastructure.PairSndComparer;

public class Counter<K> implements Serializable{

	private static final long serialVersionUID = 1L;

	private class Count implements Serializable
	{
		private static final long serialVersionUID = 1L;
		int iCount;
		
		public Count(int iVal) { iCount = iVal; }
		
		public int increment(int iVal) 
		{			
			iCount += iVal;
			return iCount; 
		}
		
		public int decrement(int iVal) 
		{ 		
			iCount -= iVal;
			return iCount; 
		}
		public int get() { return iCount; }
	}
	
	// Member variables
	
	private Map<K,Count> m_map;
	long m_iTotal;
	
	// Constructors
	
	public Counter()
	{
		m_map = new HashMap<K,Count>();
		m_iTotal = 0;
	}
	
	// Functions
	
	public int increment(K key, int iVal)
	{
		m_iTotal+=iVal;
		
		Count c = null;
		if((c=m_map.get(key))!=null)
		{
			return c.increment(iVal);
		}
		else
		{
			m_map.put(key, new Count(iVal));
			return iVal;
		}
	}
	
	public int increment(K key)
	{
		return increment(key,1);
	}
	
	public int decrement(K key, int iVal) 
	{
		m_iTotal-=iVal;
		assert m_iTotal >=0;
		
		Count c = null; int iCount = -1;
		if((c=m_map.get(key))!=null)
		{
			iCount = c.decrement(iVal);
			if(iCount==0)
			{
				m_map.remove(key);
			}
		}
		
		assert iCount>=0;
		return iCount;
	}
	
	public int decrement(K key)
	{
		return decrement(key, 1);
	}

	public int count(K key)
	{
		Count c = null;
		if((c = m_map.get(key))!=null)
			return c.get();
		else
			return 0;
	}
	
	public long total()
	{
		return m_iTotal;
	}
	
	public Set<K> keySet()
	{
		return m_map.keySet();
	}
	
	public Set<K> sortedKeySet()
	{
		return new TreeSet<K>(m_map.keySet());
	}
	
	public List<Pair<K,Integer>> freqSortedEntries()
	{
		List<Pair<K,Integer>> lpsd = new ArrayList<Pair<K,Integer>>();
		for(K key : this.keySet()) {
			lpsd.add(Pair.of(key, Integer.valueOf(this.count(key))));
		}
		Collections.sort(lpsd, Collections.reverseOrder(new PairSndComparer<K,Integer>()));
		return lpsd;
	}

	public boolean empty() {
		assert m_map.isEmpty() == (m_iTotal==0);
		return m_map.isEmpty();
	}
	
	public int pruneToTopK(int k)
	{
		List<Pair<K,Integer>> counts = new ArrayList<Pair<K,Integer>>();
		for(Map.Entry<K, Count> ekc : m_map.entrySet())
		{
			counts.add(new Pair<K,Integer>(ekc.getKey(), ekc.getValue().get()));
		}
		Collections.sort(counts, new PairSndComparer<K, Integer>());
		
		int iToPrune = counts.size() - k;
		for(int i=0;i<iToPrune;i++) m_map.remove(counts.get(i).fst);
				
		return iToPrune;
	}
	
	public int pruneByFreq(int iMinFreq)
	{
		int iPruned = 0;
		Map<K, Count> newCounts = new HashMap<K, Count>();
		for(Map.Entry<K, Count> ekc : m_map.entrySet())
		{
			int cnt = ekc.getValue().get();
			if(cnt < iMinFreq) {
				iPruned++;
				m_iTotal-=cnt;
			} else {
				newCounts.put(ekc.getKey(),ekc.getValue());
			}
		}
		m_map = newCounts;
		return iPruned;
	}
	
	public boolean hasCountFor(K key)
	{
		return m_map.containsKey(key);
	}

	public Set<Integer> valueSet() {
		HashSet<Integer> toRet = new HashSet<Integer>();
		for(K key : keySet())
			toRet.add(count(key));
		return toRet;
	}
	
	public String prettyPrint() {
		StringBuilder sb = new StringBuilder();
		for(K key : sortedKeySet()) {
			if(sb.length()!=0) sb.append(String.format("%n"));
			sb.append(String.format("%s\t%s",key,count(key)));
		}
		return sb.toString();
	}

	/**
	 * @return Number of distinct types (as opposed to tokens) being counted
	 */
	public int numTypes() {
		return m_map.size();
	}
}