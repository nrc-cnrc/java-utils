package ca.nrc.ict.Collections;

public class Pair<A, B> {
	 
	  public A fst;
	  public B snd;
	  
	  // Empty constructor needed for Jackson serialization
	  public Pair() {
		  this.fst = null;
		  this.snd = null;
	  }
	 
	  public Pair(A fst, B snd) {
	    this.fst = fst;
	    this.snd = snd;
	  }
	 
	  public A getFirst() { return fst; }
	  public B getSecond() { return snd; }
	 
	  public void setFirst(A v) { fst = v; }
	  public void setSecond(B v) { snd = v; }
	 
	  public String toString() {
	    return "(" + fst + "," + snd + ")";
	  }
	 
	  private static boolean equals(Object x, Object y) {
	    return (x == null && y == null) || (x != null && x.equals(y));
	  }
	 
	  @Override
	  public boolean equals(Object other) {
	     return
	      other != null &&
	      other instanceof Pair<?,?> &&
	      equals(fst, ((Pair<?,?>)other).fst) &&
	      equals(snd, ((Pair<?,?>)other).snd);
	  }
	 
	  @Override
	  public int hashCode() {
	    if (fst == null) return (snd == null) ? 0 : snd.hashCode() + 1;
	    else if (snd == null) return fst.hashCode() + 2;
	    else return fst.hashCode() * 17 + snd.hashCode();
	  }
	 
	  public static <A,B> Pair<A,B> of(A a, B b) {
	    return new Pair<A,B>(a,b);
	  }
	}
