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
	private Random r = new Random();
	
	protected int l1missNum = 0;
	protected int l1hitNum = 0;
	protected int l2missNum = 0;
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
			boolean located = false;
			
			L1Index = getIndex(m.iAddress, L1i);
			L1Tag = getTag(m.iAddress, L1i);
			L2Index = getIndex(m.iAddress, L2);
			L2Tag = getTag(m.iAddress, L2);
			
			for (int i = 0; i < L1i.cacheSize/L1i.numOfWays && !located && i + L1Index < L1i.cacheSize; i++) {
				if (L1i.entries[L1Index + i].tag == L1Tag) {
					located = true;
					l1hitNum++;
				} 
			}
			
			for (int i = 0; i < L2.cacheSize/L2.numOfWays && !located && i + L2Index < L2.cacheSize; i++) {
				if (L2.entries[L2Index + i].tag == L2Tag) {
					located = true;
					l1missNum++;
					l2hitNum++;
				}
			}
			if (!located) {
				l1missNum++;
				l2missNum++;
				setChanged();
				notifyObservers(m);
			}
			
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		System.out.format("\n[CPU %d] Finished\n[L1] Hits: %d Misses: %d\n[L2] Hits: %d "
				+ "Misses: %d\n", cpuNumber, l1hitNum, l1missNum, l2hitNum, l2missNum);

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
		boolean contained = false;
		
		int L1Index, L1Tag, L2Index, L2Tag;
		L1Index = getIndex(theMemoryItem.iAddress, L1i);
		L1Tag = getTag(theMemoryItem.iAddress, L1i);
		L2Index = getIndex(theMemoryItem.iAddress, L2);
		L2Tag = getTag(theMemoryItem.iAddress, L2);
		
		for (int i = 0; i < L1i.cacheSize/L1i.numOfWays && !contained && i + L1Index < L1i.cacheSize; i++) {
			if (L1i.entries[L1Index + i].tag == L1Tag) {
				contained = true;
				l1hitNum++;
			}
		}
		
		for (int i = 0; i < L2.cacheSize/L2.numOfWays && !contained && i + L2Index < L2.cacheSize; i++) {
			if (L2.entries[L2Index + i].tag == L2Tag) {
				contained = true;
				l1missNum++;
				l2hitNum++;
			}
		}
		
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
		int constructedValue = -1;
		boolean found = false;
		
		int L1Index, L1Tag, L2Index, L2Tag;
		L1Index = getIndex(theMemoryItem.iAddress, L1i);
		L1Tag = getTag(theMemoryItem.iAddress, L1i);
		L2Index = getIndex(theMemoryItem.iAddress, L2);
		L2Tag = getTag(theMemoryItem.iAddress, L2);
		
		for (int i = 0; i < L1i.cacheSize/L1i.numOfWays && !found && i + L1Index < L1i.cacheSize; i++) {
			if (L1i.entries[L1Index + i].tag == L1Tag) {
				found = true;
				constructedValue = L1Tag << (int)(Math.log(L1i.cacheSize/L1i.numOfWays) / Math.log(2));
				constructedValue = constructedValue + L1Index;
				L1i.entries[L1Index + i].MESIState = 'S';
			}
		}
		
		for (int i = 0; i < L2.cacheSize/L2.numOfWays && !found && i + L2Index < L2.cacheSize; i++) {
			if (L2.entries[L2Index + i].tag == L2Tag) {
				found = true;
				constructedValue = L2Tag << (int)(Math.log(L2.cacheSize/L2.numOfWays) / Math.log(2));
				constructedValue = constructedValue + L2Index;
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
		boolean placed = false;
		
		L1Index = getIndex(theAddress, L1i);
		L1Tag = getTag(theAddress, L1i);
		
		for (int i = 0; i < L1i.cacheSize/L1i.numOfWays && i + L1Index < L1i.cacheSize; i++) {
			if (L1i.entries[L1Index + i].tag == -1) {
				L1i.insert(L1Index + i, L1Tag, 'E');
				placed = true;
			}
		}
		if (!placed) {
			int random = r.nextInt(L1i.numOfWays);
			int oldValue = L1i.entries[L1Index + random].tag << (int)(Math.log(L1i.cacheSize/L1i.numOfWays) / Math.log(2));
			oldValue = oldValue + L1Index;
			L1i.insert(L1Index + random, L1Tag, 'E');
			
			int L2Index = getIndex(oldValue, L2);
			int L2Tag = getTag(oldValue, L2);
			placed = false;
			for (int i = 0; i < L2.cacheSize/L2.numOfWays && i + L2Index < L2.cacheSize; i++) {
				if (L2.entries[L2Index].tag == -1) {
					L2.insert(L2Index + i, L2Tag, 'E');
					placed = true;
				}
			}
			
			if(!placed) {
				random = r.nextInt(L2.numOfWays);
				oldValue = L2.entries[L2Index + random].tag << (int)(Math.log(L2.cacheSize/L2.numOfWays) / Math.log(2));
				oldValue = oldValue + L2Index;
				L2.insert(L2Index + random, L2Tag, 'E');
				
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
