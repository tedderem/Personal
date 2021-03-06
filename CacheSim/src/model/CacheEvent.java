package model;

/**
 * Simple enum class to help in identifying different events that occur during the simulation
 * of the cache.
 * 
 * @author Erik Tedder
 */
public enum CacheEvent {	
	DATA_WRITE,
	MODIFIED,
	COMPLETE;
}
