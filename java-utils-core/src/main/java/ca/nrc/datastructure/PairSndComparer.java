package ca.nrc.datastructure;

import java.util.Comparator;

public class PairSndComparer<A ,B extends Comparable<B>> implements Comparator<Pair<A,B>> {
	
	@Override
	public int compare(Pair<A, B> o1, Pair<A, B> o2) {
		return o1.snd.compareTo(o2.snd);
	}

}