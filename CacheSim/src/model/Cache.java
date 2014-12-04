package model;

public class Cache {

	int myCacheSize;
	int myLatency;
	CacheEntry[] myEntries;
	
	public Cache(final int theCacheSize, final int theLatency) {
		myCacheSize = theCacheSize;
		myLatency = theLatency;
		myEntries = new CacheEntry[theCacheSize];
	}

	
	public class CacheEntry {
		int myTag;
		
		public CacheEntry() {
			
		}
	}
}
