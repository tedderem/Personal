package model;

/**
 * Model version of a Cache within the Cache Simulator program. Each cache consists of a size, 
 * a latency (in number of cycles stalled on misses), and an array of CacheEntry values 
 * depicting the cached instructions or values within this specific cache. 
 * 
 * @author Erik Tedder
 */
public class Cache {

	/** Size of the Cache. */
	protected int cacheSize;
	/** Latency/Penalty for misses. */
	protected int latency;
	/** The number of ways for associative entry. */
	protected int numOfWays;
	/** Stored values. */
	protected CacheEntry[] entries;
	
	public Cache(final int theCacheSize, final int theLatency, final int theNumOfWays) {
		numOfWays = theNumOfWays;
		cacheSize = theCacheSize;
		latency = theLatency;
		entries = new CacheEntry[theCacheSize];
		for(int i = 0; i < entries.length; i++) {
			entries[i] = new CacheEntry();
		}
	}
	
	/**
	 * Method for inserting an item into the cache with a particular MESI value.
	 * 
	 * @param theIndex The location in cache for insertion.
	 * @param theTag The tag of the cache entry.
	 * @param theMESI The MESI state.
	 */
	public void insert(final MemoryInfo mem, final int theIndex, final int theTag, final char theMESI) {
		entries[theIndex] = new CacheEntry(theMESI, theTag, mem);
	}

	/**
	 * The individual entries to a Cache's myEntries field. Each CacheEntry consists of a MESI
	 * State and a tag. 
	 * 
	 * @author Erik Tedder	 
	 */
	public class CacheEntry {
		char MESIState;
		int tag;
		MemoryInfo data;
		
		public CacheEntry() {
			this(' ', -1, new MemoryInfo());
		}
		
		public CacheEntry(final char theMESI, final int theTag, final MemoryInfo theData) {
			data = theData;
			MESIState = theMESI;
			tag = theTag;
		}
	}
}
