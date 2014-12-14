package model;

/**
 * Class for denoting a modification in the various CPUs that need to affect the others. 
 * Example of this is data being modified on one CPU and needing to invalidate data on others.
 * 
 * @author Erik Tedder
 */
public class CacheModification {
	/** The starting MESI state. */
	char startState;
	/** The ending MESI state. */
	char endState;
	/** The modified MemoryInfo item. */
	MemoryInfo mem;
	
	public CacheModification(final char theStartState, final char theEndState, 
			final MemoryInfo theMem) {
		startState = theStartState;
		endState = theEndState;
		mem = theMem;
	}
}
