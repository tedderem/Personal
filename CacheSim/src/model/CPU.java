package model;

import java.util.ArrayList;
import java.util.Observable;

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
			L1Index = getIndex(m.iAddress, L1i.cacheSize);
			L1Tag = getTag(m.iAddress, L1i.cacheSize);
			L2Index = getIndex(m.iAddress, L2.cacheSize);
			L2Tag = getTag(m.iAddress, L2.cacheSize);
			
			if (L1i.entries[L1Index].tag == L1Tag) {
				setChanged();
				notifyObservers(CacheEvent.L1_HIT);
			} else if (L2.entries[L2Index].tag == L2Tag) {
				setChanged();
				notifyObservers(CacheEvent.L2_HIT);
			} else {
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
		L1Index = getIndex(theMemoryItem.iAddress, L1i.cacheSize);
		L1Tag = getTag(theMemoryItem.iAddress, L1i.cacheSize);
		L2Index = getIndex(theMemoryItem.iAddress, L2.cacheSize);
		L2Tag = getTag(theMemoryItem.iAddress, L2.cacheSize);
		
		if (L1i.entries[L1Index].tag == L1Tag) {
			contained = true;
			setChanged();
			notifyObservers(CacheEvent.L1_HIT);
		} else if (L1d.entries[L1Index].tag == L1Tag) {
			contained = true;
			setChanged();
			notifyObservers(CacheEvent.L1_HIT);
		} else if (L2.entries[L2Index].tag == L2Tag) {
			contained = true;
			setChanged();
			notifyObservers(CacheEvent.L2_HIT);
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
		
		int L1Index, L1Tag, L2Index, L2Tag;
		L1Index = getIndex(theMemoryItem.iAddress, L1i.cacheSize);
		L1Tag = getTag(theMemoryItem.iAddress, L1i.cacheSize);
		L2Index = getIndex(theMemoryItem.iAddress, L2.cacheSize);
		L2Tag = getTag(theMemoryItem.iAddress, L2.cacheSize);
		
		if (L1i.entries[L1Index].tag == L1Tag) {
			constructedValue = L1Tag << (int)(Math.log(L1i.cacheSize) / Math.log(2));
			constructedValue = constructedValue + L1Index;
			L1i.entries[L1Index].MESIState = 'S';
		} else if (L2.entries[L2Index].tag == L2Tag) {
			constructedValue = L2Tag << (int)(Math.log(L2.cacheSize) / Math.log(2));
			constructedValue = constructedValue + L2Index;
			L2.entries[L2Index].MESIState = 'S';
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
		L1Index = getIndex(theAddress, L1i.cacheSize);
		L1Tag = getTag(theAddress, L1i.cacheSize);
		if (L1i.entries[L1Index].tag != -1) {
			//attempt to reconstruct memory instruction
			int oldValue = L1i.entries[L1Index].tag << (int)(Math.log(L1i.cacheSize) / Math.log(2));
			oldValue = oldValue + L1Index;
			L1i.insert(L1Index, L1Tag, 'E');
			
			int L2Index = getIndex(oldValue, L2.cacheSize);
			int L2Tag = getTag(oldValue, L2.cacheSize);			
			if (L2.entries[L2Index].tag != -1) {
				oldValue = L2.entries[L2Index].tag << (int)(Math.log(L2.cacheSize) / Math.log(2));
				oldValue = oldValue + L2Index;
				L2.insert(L2Index, L2Tag, 'E');
				
				setChanged();
				notifyObservers(new Integer(oldValue));
			} else {
				L2.insert(L2Index, L2Tag, 'E');
			}
		} else {
			//no item in this cache location, insert.
			L1i.insert(L1Index, L1Tag, 'E');
		}
	}
	
	/**
	 * Method for calculating the index for caching.
	 * 
	 * @param theMemoryItem The item to be cached.
	 * @param theCacheSize The size of the cache used.
	 * @return The index for this item.
	 */
	private int getIndex(final int address, final int theCacheSize) {
		return address & (theCacheSize - 1);
	}
	
	/**
	 * Method for calculating the tag for caching.
	 * 
	 * @param theMemoryItem
	 * @param theCacheSize
	 * @return
	 */
	private int getTag(final int address, final int theCacheSize) {
		return address >> (int)(Math.log(theCacheSize) / Math.log(2));
	}

	/**
	 * Starts the CPU.
	 */
	public void start() {
		System.out.println("thread " + cpuNumber + " started");
		t = new Thread(this);
		t.start();
	}

}
