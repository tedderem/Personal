package model;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;
import java.util.Random;

/**
 * Caching Simulator program that reads in address traces and allocates them appropriately to 
 * cache locations. 
 * 
 * @author Erik Tedder
 */
public class Simulator implements Observer {
	/* Necessary constants for the CPU states. */
	private final static int FIRST_MEM_SIZE = 0x800000;
	private final static int L1_SIZE = 8;
	private final static int L1_LATENCY = 1;
	private final static int L2_SIZE = 32;
	private final static int L2_LATENCY = 10;
	private final static int L3_SIZE = 128;
	private final static int L3_LATENCY = 20;
	private final static int FIRST_MEM_LATENCY = 100;
	private final static int SECOND_MEM_LATENCY = 250;
	private final static int NUM_OF_WAYS = 2;
	private final static int CPU_TOTAL = 2;
	/**	String name of the file for memory trace. */
	private final static String TRACE_FILE = "trace-2k.csv";
	
	/** The shared level 3 cache for the CPUs. */
	protected Cache L3;
	/** L3 miss counter. */
	private int l3missNum;
	/** L3 hit counter. */
	private int l3hitNum;
	
	/** Counter to denote when the threads are complete. */
	private int threadsComplete;
	/** List for the TRACE_FILE. */
	private ArrayList<MemoryInfo> trace;
	/** Random number generator for assigning positions at random. */
	private Random r = new Random();
	
	/** Cpu 1. */
	private CPU cpu1;
	/** Cpu 2. */
	private CPU cpu2;
	
	/**
	 * Some constructor.
	 */
	public Simulator() {
		l3missNum = 0;
		l3hitNum = 0;
		threadsComplete = 0;		
		//Construct the L3
		L3 = new Cache(L3_SIZE, L3_LATENCY, NUM_OF_WAYS);
		//Initialize the trace array
		trace = new ArrayList<MemoryInfo>();
		
		//Read in the trace file
		try {
			BufferedReader br = new BufferedReader(new FileReader(TRACE_FILE));
			String line = br.readLine();
						
			while (line != null) {
				String[] tokens = line.split(",", -1);
				
				int add, io, data;				
				
				add = Integer.parseInt(tokens[0]);
				if (tokens[1].equals("")) {
					io = -1;
				} else {
					io = Integer.parseInt(tokens[1]);
				}
				if (tokens[2].equals("")) {
					data = -1;
				} else {
					data = Integer.parseInt(tokens[2]);
				}				
				
				trace.add(new MemoryInfo(add, io, data));
				
				line = br.readLine();
			}
			br.close();
		} catch (FileNotFoundException e) {
			System.err.println("ERROR READING FILE");
		} catch (IOException e) {
			System.err.println("ISSUE READING LINE");
		}

		//Construct the CPUs
		cpu1 = new CPU(trace, L1_SIZE, L1_LATENCY, L2_SIZE, L2_LATENCY, NUM_OF_WAYS, 1);
		cpu2 = new CPU(trace, L1_SIZE, L1_LATENCY, L2_SIZE, L2_LATENCY, NUM_OF_WAYS, 2);
		//Set this simulator to observe them
		cpu1.addObserver(this);
		cpu2.addObserver(this);
		//start both CPUs
		cpu1.start();
		cpu2.start();	
	}

	/**
	 * Testing purposes currently. 
	 * 
	 * @param theArgs Command-line inputs (not applicable to this program).
	 */
	public static void main(String... theArgs) {		
		Simulator s = new Simulator();		
	}

	/**
	 * Called when a change has been made within the CPU.
	 */
	@Override
	public void update(Observable o, Object arg) {		
		//Called when an item is not found within the L1 or L2 caches
		if (arg instanceof MemoryInfo) {
			//Take argument passed and cast it to a MemoryInfo object
			MemoryInfo m = (MemoryInfo) arg;
			//CPU1 made this call
			if (((CPU) o).cpuNumber == 1) {				
				if(cpu2.snoop(m)) {
					cpu1.add(cpu2.get(m));
					//set to shared MESI state
				} else {
					int index = m.iAddress & (L3_SIZE/NUM_OF_WAYS - 1);
					int tag = m.iAddress >> (int)(Math.log(L3_SIZE/NUM_OF_WAYS) / Math.log(2));
					boolean found = false;
					
					for (int i = 0; i < L3.cacheSize/NUM_OF_WAYS && i + index < L3.cacheSize; i++) {
						if (L3.entries[index + i].tag == tag) {							
							l3hitNum++;
							found = true;
						} 
					}
					if (!found) {
						l3missNum++;
						cpu1.add(m.iAddress);
					}
				}				
			} else { //CPU 2 made this call
				if(cpu1.snoop(m)) {
					cpu2.add(cpu1.get(m));
				} else {
					int index = m.iAddress & (L3_SIZE/NUM_OF_WAYS - 1);
					int tag = m.iAddress >> (int)(Math.log(L3_SIZE/NUM_OF_WAYS) / Math.log(2));
					boolean found = false;
					
					for (int i = 0; i < L3.cacheSize/NUM_OF_WAYS && i + index < L3.cacheSize; i++) {
						if (L3.entries[index + i].tag == tag) {							
							l3hitNum++;
							found = true;
						} 
					}
					if (!found) {
						l3missNum++;
						cpu2.add(m.iAddress);
					}
				}
			}
		}
		
		//Observed CPU has an item needing to be placed into L3
		if (arg instanceof Integer) {
			//boolean flag for denoting item has been placed
			boolean placed = false;
			//calculate index and tag for the L3 cache
			int index = (int)arg & (L3_SIZE/NUM_OF_WAYS - 1);
			int tag = (int)arg >> (int)(Math.log(L3_SIZE/NUM_OF_WAYS) / Math.log(2));
			//Scan L3 cache within the set to see if there are any available slots
			for (int i = 0; i < L3.cacheSize/NUM_OF_WAYS && i + index < L3.cacheSize; i++) {
				if(L3.entries[index + i].tag == -1) {
					L3.insert(index + i, tag, 'E');
					//Item was placed in L3
					placed = true;
				}
			}
			//Item was not placed in an empty slot, something needs to be evicted
			if (!placed) {
				L3.insert(index + r.nextInt(NUM_OF_WAYS), tag, 'E');
			}
		}
		
		//Threads have completed
		if (arg == CacheEvent.COMPLETE) {
			//Running count of how many threads have completed so far
			if (threadsComplete != CPU_TOTAL) {
				threadsComplete++;
			}
			
			//When all threads/CPUs have completed their calculations, print out final values.
			if (threadsComplete == CPU_TOTAL) {
				System.out.format("\n[L3] Hits: %d Misses: %d\n", l3hitNum, l3missNum);
				int hits = l3hitNum + cpu1.l1hitNum + cpu1.l2hitNum + cpu2.l1hitNum + cpu2.l2hitNum;
				int misses = l3missNum + cpu1.l1missNum + cpu1.l2missNum + cpu2.l1missNum + cpu2.l2missNum;
				int cycles = l3missNum * L3_LATENCY + cpu1.l1missNum * L1_LATENCY + cpu1.l2missNum * L2_LATENCY + cpu2.l1missNum * L1_LATENCY + cpu2.l2missNum * L2_LATENCY;
				System.out.format("Total hits: %d Total Misses: %d Total Cycles: %d", hits, misses, cycles);
			}
		}
	}
}
