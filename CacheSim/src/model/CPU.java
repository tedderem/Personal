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
	
	/**
	 * Denotes the data handling method. If '1', then write back is enabled and data will be
	 * written once flushed from L1d. If '0', then data will be written through or instantly
	 * written instead of delayed.
	 */
	private int writeBack;
	
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
			final int theNumOfWays, final int theWB, final int thecpuNumber) {		
		cpuNumber = thecpuNumber;
		memoryTrace = theTrace;
		writeBack = theWB;
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
			
			if (m.ioValue == 1) { //is a data write
				int dataIndex = getIndex(m.dAddress, L1d);
				int dataTag = getTag(m.dAddress, L1d);
				
				//denote an immediate write to memory if write-through
				//(just needs to happen within same span of placing to cache)
				if (writeBack == 0) {
					setChanged();
					notifyObservers(CacheEvent.DATA_WRITE);
				}
				
				//Check if item is in the L1 cache (iterates through checking)
				for (int i = 0; i < L1d.numOfWays && !located && i + dataIndex < L1d.cacheSize; i++) {
					if (L1d.entries[dataIndex + i].tag == dataTag) {
						located = true;
						l1hitNum++;
						//notify simulator of a write and state change
						setChanged();
						notifyObservers(new CacheModification(L1d.entries[dataIndex + i].MESIState, 'M', m));
						//set entry as modified
						L1d.entries[dataIndex + i].MESIState = 'M';
					} 
				}
				dataIndex = getIndex(m.dAddress, L2);
				dataTag = getTag(m.dAddress, L2);
				for (int i = 0; i < L2.numOfWays && !located && i + dataIndex < L2.cacheSize; i++) {
					if (L2.entries[dataIndex + i].tag == dataTag) {
						located = true;
						l1missNum++;
						l2hitNum++;
						//notify simulator of a write and state change
						setChanged();
						notifyObservers(new CacheModification(L2.entries[dataIndex + i].MESIState, 'M', m));
						//set entry as modified
						L2.entries[dataIndex + i].MESIState = 'M';
					} 
				}
			} else {
				//Check if item is in the L1 cache (iterates through checking)
				for (int i = 0; i < L1i.numOfWays && !located && i + L1Index < L1i.cacheSize; i++) {
					if (L1i.entries[L1Index + i].tag == L1Tag) {
						located = true;
						l1hitNum++;
					} 
				}
				//Check if the item is in the L2 cache (iterates through checking)
				for (int i = 0; i < L2.numOfWays && !located && i + L2Index < L2.cacheSize; i++) {
					if (L2.entries[L2Index + i].tag == L2Tag) {
						located = true;
						l1missNum++;
						l2hitNum++;
					}
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
			try {
				Thread.sleep(0);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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
	 * cache. If so, returns the MemoryInfo contained. If not, returns a MemoryInfo with an 
	 * iAddress of -1.
	 * 
	 * @param theMemoryItem The potential memory item being snooped.
	 * @return MemoryInfo
	 */
	public MemoryInfo snoop(final MemoryInfo theMemoryItem) {
		//MemoryInfo to be returned (set address to -1 to denote none found)
		MemoryInfo snooped = new MemoryInfo(-1, -1, -1);
		//boolean to denote item being found
		boolean contained = false;
		
		//Construct the L1 and L2 indices and Tags
		int L1Index, L1Tag, L2Index, L2Tag;
		L1Index = getIndex(theMemoryItem.dAddress, L1i);
		L1Tag = getTag(theMemoryItem.dAddress, L1i);
		L2Index = getIndex(theMemoryItem.dAddress, L2);
		L2Tag = getTag(theMemoryItem.dAddress, L2);
		
		//see if memory item is in l1d cache (since checking data)
		for (int i = 0; i < L1d.numOfWays && !contained && i + L1Index < L1d.cacheSize; i++) {
			if (L1d.entries[L1Index + i].tag == L1Tag) {
				contained = true;
				snooped = L1d.entries[L1Index + i].data;
				L1d.entries[L1Index + i].MESIState = 'S';
				l1hitNum++;
			}
		}
		//data not contained in L1, increment miss counter
		if (!contained) {
			l1missNum++;
		}
		
		//see if memory item is in l2 cache
		for (int i = 0; i < L2.numOfWays && !contained && i + L2Index < L2.cacheSize; i++) {
			if (L2.entries[L2Index + i].tag == L2Tag) {
				contained = true;
				snooped = L2.entries[L2Index + i].data;
				l2hitNum++;
			}
		}
		
		//return whether this CPU contains this item
		return snooped;
	}
	
	/**
	 * Method for adding an item to this CPU's caches. 
	 * 
	 * @param theMemoryItem
	 */
	public void add(final MemoryInfo mem, final char theState) {
		int L1Index, L1Tag, dataIndex, dataTag;
		//boolean denoting whether item has been placed (for eviction purposes)
		boolean placed = false;
		
		//Item deals with data, try to add to L1d
		if (mem.ioValue != -1) {
			boolean dataPlaced = false;
			dataIndex = getIndex(mem.dAddress, L1d);
			dataTag = getTag(mem.dAddress, L1d);
			for (int i = 0; i < L1d.numOfWays && i + dataIndex < L1d.cacheSize; i++) {
				if (L1d.entries[dataIndex + i].tag == -1) {
					//is an empty spot, insert and denote placed boolean
					L1d.insert(mem, dataIndex + i, dataTag, theState);
					dataPlaced = true;
				}
			}
			
			if (!dataPlaced) {
				//Choose random value within the number of ways
				int random = r.nextInt(L1d.numOfWays);
				MemoryInfo oldValue;
				//MemoryInfo for the item being evicted
				oldValue = L1d.entries[dataIndex + random].data;
				//save the old MESI state to ensure we don't write out faulty data
				char oldMESI = L1d.entries[dataIndex + random].MESIState;
				L1d.insert(mem, dataIndex + random, dataTag, theState);
				//Old data entry got evicted and needs to be written
				if (writeBack == 1 && oldValue.ioValue == 1 && oldMESI != 'I') {
					setChanged();
					notifyObservers(CacheEvent.DATA_WRITE);
				}			
				//Calculate the L2 index and tag for the evicted item
				int L2Index = getIndex(oldValue.dAddress, L2);
				int L2Tag = getTag(oldValue.dAddress, L2);
				
				placed = false;
				//Check all of L2 caches (within the necessary range) to check for free spot
				for (int i = 0; i < L2.numOfWays && i + L2Index < L2.cacheSize; i++) {
					if (L2.entries[L2Index + i].tag == -1) {
						L2.insert(oldValue, L2Index + i, L2Tag, theState);
						//item placed in L2 cache
						placed = true;
					}
				}
				//Evicted item was not placed in L2
				if(!placed) {
					//take a random value within the necessary range
					random = r.nextInt(L2.numOfWays);
					MemoryInfo temp = oldValue;
					//Construct address value of evicted item
					oldValue = L2.entries[L2Index + random].data;
					//Place item evicted from L1 into L2
					L2.insert(temp, L2Index + random, L2Tag, theState);
					
					//Let simulator know something needs to be placed into L3.
					//only placing non-data values into L3
					if (oldValue.ioValue == -1) {
						setChanged();
						notifyObservers(new Integer(oldValue.iAddress));
					}
				}
			}
		} 
		
		//calculate index and tag for L1
		L1Index = getIndex(mem.iAddress, L1i);
		L1Tag = getTag(mem.iAddress, L1i);
		
		//Search L1i to see if there are any empty spots
		for (int i = 0; i < L1i.numOfWays && i + L1Index < L1i.cacheSize; i++) {
			if (L1i.entries[L1Index + i].tag == -1) {
				//is an empty spot, insert and denote placed boolean
				L1i.insert(mem, L1Index + i, L1Tag, theState);
				placed = true;
			}
		}
			
		
		//No empty spots in cache, need to evict something
		if (!placed) {
			//Choose random value within the number of ways
			int random = r.nextInt(L1i.numOfWays);
			MemoryInfo oldValue;
			//MemoryInfo for the item being evicted
			//item doesnt involve data, put in instruction
			oldValue = L1i.entries[(L1Index + random)].data;
			L1i.insert(mem, L1Index + random, L1Tag, theState);
			
			//Calculate the L2 index and tag for the evicted item
			int L2Index = getIndex(oldValue.iAddress, L2);
			int L2Tag = getTag(oldValue.iAddress, L2);
			//denote it not being placed
			placed = false;
			//Check all of L2 caches (within the necessary range) to check for free spot
			for (int i = 0; i < L2.numOfWays && i + L2Index < L2.cacheSize; i++) {
				if (L2.entries[L2Index + i].tag == -1) {
					L2.insert(oldValue, L2Index + i, L2Tag, theState);
					//item placed in L2 cache
					placed = true;
				}
			}
			//Evicted item was not placed in L2
			if(!placed) {
				//take a random value within the necessary range
				random = r.nextInt(L2.numOfWays);
				MemoryInfo temp = oldValue;
				//Construct address value of evicted item
				oldValue = L2.entries[L2Index + random].data;
				//Place item evicted from L1 into L2
				L2.insert(temp, L2Index + random, L2Tag, theState);
				
				//Let simulator know something needs to be placed into L3.
				setChanged();
				notifyObservers(new Integer(oldValue.iAddress));
			}
		}
	}
	
	/**
	 * Method that is called when an entry in cache may need to be invalidated.
	 * 
	 * @param mem The item that is being written in a different CPU.
	 */
	public void invalidateData(final MemoryInfo mem) {
		int dataIndex = getIndex(mem.dAddress, L1d);
		int dataTag = getTag(mem.dAddress, L1d);
		
		//Check if item is in the L1 cache (iterates through checking)
		for (int i = 0; i < L1d.numOfWays && i + dataIndex < L1d.cacheSize; i++) {
			if (L1d.entries[dataIndex + i].tag == dataTag) {
				//notify simulator of a write and state change
				setChanged();
				notifyObservers(new CacheModification(L1d.entries[dataIndex + i].MESIState, 'I', mem));
				//set entry as modified
				L1d.entries[dataIndex + i].MESIState = 'I';
			} 
		}
		dataIndex = getIndex(mem.dAddress, L2);
		dataTag = getTag(mem.dAddress, L2);
		for (int i = 0; i < L2.numOfWays && i + dataIndex < L2.cacheSize; i++) {
			if (L2.entries[dataIndex + i].tag == dataTag) {
				//notify simulator of a write and state change
				setChanged();
				notifyObservers(new CacheModification(L2.entries[dataIndex + i].MESIState, 'I', mem));
				//set entry as modified
				L2.entries[dataIndex + i].MESIState = 'I';
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
