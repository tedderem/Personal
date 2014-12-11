package model;

import java.util.ArrayList;

/**
 * Simulates a CPU for the Caching Simulator. CPU consists of an L1 and L2 cache locations.
 * 
 * @author Erik Tedder
 */
public class CPU {
	
	/** The first level cache. */
	private Cache L1;
	/** The second level cache. */
	private Cache L2;
	/** The memory trace. */
	private ArrayList<MemoryInfo> memoryTrace;
	
	/**
	 * CPU constructor for instantiating the different cache sizes and values.
	 * 
	 * @param theL1Size Size of L1.
	 * @param theL1Latency Latency of L1.
	 * @param theL2Size Size of L2.
	 * @param theL2Latency Latency of L2.
	 * @param theNumOfWays The cache associativity (2, 4, or 8).
	 */
	public CPU(final ArrayList<MemoryInfo> theTrace, final int theL1Size, 
			final int theL1Latency, final int theL2Size, final int theL2Latency, 
			final int theNumOfWays) {
		memoryTrace = theTrace;
		L1 = new Cache(theL1Size, theL1Latency, theNumOfWays);
		L2 = new Cache(theL2Size, theL2Latency, theNumOfWays);		
	}

}
