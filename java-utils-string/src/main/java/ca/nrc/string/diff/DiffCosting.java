package ca.nrc.string.diff;

import java.util.List;

/**
 * This class is used to assign a "cost" to the transformations 
 * required to transform one string into another.
 * 
 * @author desilets
 *
 */
public abstract class DiffCosting {
	
	public static final double INFINITE = 1000000;
	public static final Double SMALL_COST = new Double(0.1);
	public static final Double TINY_COST = new Double(1.0);

	public abstract double cost(DiffResult diff);
}
