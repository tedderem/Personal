package model;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Random;

/**
 * Simulates a CPU for the Caching Simulator. CPU consists of an L1 and L2 cache locations.
 * 
 * @author Erik Tedder
 */
public class CPU extends Observable implements Runnable {
	
	/** The thread for this CPU. */
	private Thread t;
	/** The first level cache for instruction. */
	private Cache L1i;
	/** The first level cache for data. */
	private Cache L1d;
	/** The second level cache. */
	private Cache L2;
	/** Name for the CPU. */
	protected int cpuNumber;
	/** The memory trace. */
	private ArrayList<MemoryInfo> memoryTrace;
	/** Random number generator for assigning positions at random. */
	private Random r = new Random();
	
	/** L1 miss counter. */
	protected int l1missNum = 0;
	/** L1 hit counter. */
	protected int l1hitNum = 0;
	/** L2 miss counter. */
	protected int l2missNum = 0;
	/** L2 hit counter. */
	protected int l2hitNum = 0;
	
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
			final int theNumOfWays, final int thecpuNumber) {		
		cpuNumber = thecpuNumber;
		memoryTrace = theTrace;
		L1i = new Cache(theL1Size, theL1Latency, theNumOfWays);
		L1d = new Cache(theL1Size, theL1Latency, theNumOfWays);
		L2 = new Cache(theL2Size, theL2Latency, theNumOfWays);				
	}
	
	/**
	 * For threading.
	 */
	@Override
	public void run() {
		int L1Index, L1Tag, L2Index, L2Tag;
		
		//go through each item of the memoryTrace and see if it is in the Caches
		for (MemoryInfo m : memoryTrace) {
			//Flag to denote the item was found and no need to continue checking
			boolean located = false;
			//construct the L1 and L2 indices and tags
			L1Index = getIndex(m.iAddress, L1i);
			L1Tag = getTag(m.iAddress, L1i);
			L2Index = getIndex(m.iAddress, L2);
			L2Tag = getTag(m.iAddress, L2);
			
			//Check if item is in the L1 cache (iterates through checking)
			for (int i = 0; i < L1i.cacheSize/L1i.numOfWays && !located && i + L1Index < L1i.cacheSize; i++) {
				if (L1i.entries[L1Index + i].tag == L1Tag) {
					located = true;
					l1hitNum++;
				} 
			}
			
			//Check if the item is in the L2 cache (iterates through checking)
			for (int i = 0; i < L2.cacheSize/L2.numOfWays && !located && i + L2Index < L2.cacheSize; i++) {
				if (L2.entries[L2Index + i].tag == L2Tag) {
					located = true;
					l1missNum++;
					l2hitNum++;
				}
			}
			
			//Item has not be located in L1 or L2
			if (!located) {
				//L1 and L2 miss
				l1missNum++;
				l2missNum++;
				//Notify observer to check in shared L3
				setChanged();
				notifyObservers(m);
			}
			
			//Sleep thread for a second (good for fast processors)
//			try {
//				Thread.sleep(1);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
		}
		//CPU has finished with provided memory trace, output results.
		System.out.format("\n[CPU %d] Finished\n[L1] Hits: %d Misses: %d\n[L2] Hits: %d "
				+ "Misses: %d\n", cpuNumber, l1hitNum, l1missNum, l2hitNum, l2missNum);
		//Let observer know thread has completed
		setChanged();
		notifyObservers(CacheEvent.COMPLETE);
		
	}
	
	/**
	 * Snooping method for this CPU. Checks all local caches to see if the item is in the 
	 * cache. If so, return true.
	 * 
	 * @param theMemoryItem The potential memory item being snooped.
	 * @return boolean
	 */
	public boolean snoop(final MemoryInfo theMemoryItem) {
		//flag to denote if item is contained or not
		boolean contained = false;
		
		//Construct the L1 and L2 indices and Tags
		int L1Index, L1Tag, L2Index, L2Tag;
		L1Index = getIndex(theMemoryItem.iAddress, L1i);
		L1Tag = getTag(theMemoryItem.iAddress, L1i);
		L2Index = getIndex(theMemoryItem.iAddress, L2);
		L2Tag = getTag(theMemoryItem.iAddress, L2);
		
		//see if memory item is in l1 cache
		for (int i = 0; i < L1i.cacheSize/L1i.numOfWays && !contained && i + L1Index < L1i.cacheSize; i++) {
			if (L1i.entries[L1Index + i].tag == L1Tag) {
				contained = true;
				l1hitNum++;
			}
		}
		
		//see if memory item is in l2 cache
		for (int i = 0; i < L2.cacheSize/L2.numOfWays && !contained && i + L2Index < L2.cacheSize; i++) {
			if (L2.entries[L2Index + i].tag == L2Tag) {
				contained = true;
				l1missNum++;
				l2hitNum++;
			}
		}
		
		//return whether this CPU contains this item
		return contained;
	}
	
	/**
	 * Get method for retrieving an item from cache (in the case of giving a parallel CPU an 
	 * item).
	 * 
	 * @param theMemoryItem The cache entry to be retrieved.
	 * @return boolean
	 */
	public int get(final MemoryInfo theMemoryItem) {
		//Nonsensical return value
		int constructedValue = -1;
		//Boolean denoting if item has been found
		boolean found = false;
		
		//Construct l1 and l2 tag and index values
		int L1Index, L1Tag, L2Index, L2Tag;
		L1Index = getIndex(theMemoryItem.iAddress, L1i);
		L1Tag = getTag(theMemoryItem.iAddress, L1i);
		L2Index = getIndex(theMemoryItem.iAddress, L2);
		L2Tag = getTag(theMemoryItem.iAddress, L2);
		
		//search L1 for item
		for (int i = 0; i < L1i.cacheSize/L1i.numOfWays && !found && i + L1Index < L1i.cacheSize; i++) {
			if (L1i.entries[L1Index + i].tag == L1Tag) {
				found = true;
				//reconstruct the address value
				constructedValue = L1Tag << (int)(Math.log(L1i.cacheSize/L1i.numOfWays) / Math.log(2));
				constructedValue = constructedValue + L1Index;
				//denote item as being shared
				L1i.entries[L1Index + i].MESIState = 'S';
			}
		}
		
		//Search l2 for item
		for (int i = 0; i < L2.cacheSize/L2.numOfWays && !found && i + L2Index < L2.cacheSize; i++) {
			if (L2.entries[L2Index + i].tag == L2Tag) {
				found = true;
				//reconstruct the address value
				constructedValue = L2Tag << (int)(Math.log(L2.cacheSize/L2.numOfWays) / Math.log(2));
				constructedValue = constructedValue + L2Index;
				//denote item as being shared
				L2.entries[L2Index + i].MESIState = 'S';
			}
		}
		
		return constructedValue;
	}
	
	/**
	 * Method for adding an item to this CPU's caches. 
	 * 
	 * @param theMemoryItem
	 */
	public void add(final int theAddress) {
		int L1Index, L1Tag;
		//boolean denoting whether item has been placed (for eviction purposes)
		boolean placed = false;
		//calculate index and tag for L1
		L1Index = getIndex(theAddress, L1i);
		L1Tag = getTag(theAddress, L1i);
		
		//Search L1 to see if there are any empty spots
		for (int i = 0; i < L1i.cacheSize/L1i.numOfWays && i + L1Index < L1i.cacheSize; i++) {
			if (L1i.entries[L1Index + i].tag == -1) {
				//is an empty spot, insert and denote placed boolean
				L1i.insert(L1Index + i, L1Tag, 'E');
				placed = true;
			}
		}
		
		//No empty spots in cache, need to evict something
		if (!placed) {
			//Choose random value within the number of ways
			int random = r.nextInt(L1i.numOfWays);
			//Construct the address value for the item being evicted
			int oldValue = L1i.entries[(L1Index + random)].tag << (int)(Math.log(L1i.cacheSize/L1i.numOfWays) / Math.log(2));
			oldValue = oldValue + L1Index;
			//Insert item into L1
			L1i.insert(L1Index + random, L1Tag, 'E');
			//Calculate the L2 index and tag for the evicted item
			int L2Index = getIndex(oldValue, L2);
			int L2Tag = getTag(oldValue, L2);
			//denote it not being placed
			placed = false;
			//Check all of L2 caches (within the necessary range) to check for free spot
			for (int i = 0; i < L2.cacheSize/L2.numOfWays && i + L2Index < L2.cacheSize; i++) {
				if (L2.entries[L2Index + i].tag == -1) {
					L2.insert(L2Index + i, L2Tag, 'E');
					//item placed in L2 cache
					placed = true;
				}
			}
			//Evicted item was not placed in L2
			if(!placed) {
				//take a random value within the necessary range
				random = r.nextInt(L2.numOfWays);
				//Construct address value of evicted item
				oldValue = L2.entries[L2Index + random].tag << (int)(Math.log(L2.cacheSize/L2.numOfWays) / Math.log(2));
				oldValue = oldValue + L2Index;
				//Place item evicted from L1 into L2
				L2.insert(L2Index + random, L2Tag, 'E');
				
				//Let simulator know something needs to be placed into L3.
				setChanged();
				notifyObservers(new Integer(oldValue));
			}
		}
	}
	
	/**
	 * Method for calculating the index for caching.
	 * 
	 * @param theMemoryItem The item to be cached.
	 * @param theCacheSize The size of the cache used.
	 * @return The index for this item.
	 */
	private int getIndex(final int address, final Cache theCache) {
		return address & (theCache.cacheSize/theCache.numOfWays - 1);
	}
	
	/**
	 * Method for calculating the tag for caching.
	 * 
	 * @param theMemoryItem
	 * @param theCacheSize
	 * @return
	 */
	private int getTag(final int address, final Cache theCache) {
		return address >> (int)(Math.log(theCache.cacheSize/theCache.numOfWays) / Math.log(2));
	}

	/**
	 * Starts the CPU.
	 */
	public void start() {
		System.out.format("[CPU %d] started\n", cpuNumber);
		t = new Thread(this);
		t.start();
	}

}
