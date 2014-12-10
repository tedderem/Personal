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
	int myCacheSize;
	/** Latency/Penalty for misses. */
	int myLatency;
	/** Stored values. */
	CacheEntry[] myEntries;
	
	public Cache(final int theCacheSize, final int theLatency) {
		myCacheSize = theCacheSize;
		myLatency = theLatency;
		myEntries = new CacheEntry[theCacheSize];
	}

	/**
	 * The individual entries to a Cache's myEntries field. Each CacheEntry consists of a MESI
	 * State and a tag. 
	 * 
	 * @author Erik Tedder	 
	 */
	public class CacheEntry {
		char myMESIState;
		int myTag;
		
		public CacheEntry() {
			
		}
	}
}
