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
	int cacheSize;
	/** Latency/Penalty for misses. */
	int latency;
	/** The number of ways for associative entry. */
	int numOfWays;
	/** Stored values. */
	CacheEntry[] entries;
	
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
	 * The individual entries to a Cache's myEntries field. Each CacheEntry consists of a MESI
	 * State and a tag. 
	 * 
	 * @author Erik Tedder	 
	 */
	public class CacheEntry {
		char MESIState;
		int tag;
		
		public CacheEntry() {
			MESIState = ' ';
			tag = -1;
		}
	}
}
