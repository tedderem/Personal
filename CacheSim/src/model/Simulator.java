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
	private final static int L1_SIZE = 16;
	private final static int L1_LATENCY = 2;
	private final static int L2_SIZE = 64;
	private final static int L2_LATENCY = 12;
	private final static int L3_SIZE = 256;
	private final static int L3_LATENCY = 25;
	private final static int FIRST_MEM_LATENCY = 120;
	private final static int SECOND_MEM_LATENCY = 400;
	private final static int NUM_OF_WAYS = 8;
	private final static int CPU_TOTAL = 2;
	private final static int WRITE_BACK = 1;
	/**	String name of the file for memory trace. */
	private final static String TRACE_FILE = "trace-5k.csv";
	
	/** The shared level 3 cache for the CPUs. */
	protected Cache L3;
	/** L3 miss counter. */
	private int l3missNum;
	/** L3 hit counter. */
	private int l3hitNum;
	
	/**
	 * Matrix to represent the changes of MESI states. Will have 0 be M, 1 is Exclusive, 2 is
	 * Shared, and 4 is Invalid. Each row is the starting value and the column is the ending 
	 * value. ie mesiTransitions[1][2] value is the number of times going from Exclusive to
	 * Shared.
	 */
	private int[][] mesi = new int[4][4];
	
	/** Counter for memory latency. */
	private int memCycles;
	
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
		memCycles = 0;
		threadsComplete = 0;		
		//Construct the L3
		L3 = new Cache(L3_SIZE, L3_LATENCY, NUM_OF_WAYS);
		//Initialize the trace array
		trace = new ArrayList<MemoryInfo>();
		
		//Read in the trace file
		try {
			BufferedReader br = new BufferedReader(new FileReader(TRACE_FILE));
			String line = br.readLine();
			//read in each line and tokenize			
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
		cpu1 = new CPU(trace, L1_SIZE, L1_LATENCY, L2_SIZE, L2_LATENCY, NUM_OF_WAYS, WRITE_BACK, 1);
		cpu2 = new CPU(trace, L1_SIZE, L1_LATENCY, L2_SIZE, L2_LATENCY, NUM_OF_WAYS, WRITE_BACK, 2);
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
				if (m.ioValue == 0) { //read instruction
					//cpu2 does contain this item
					if(cpu2.snoop(m).iAddress != -1) {
						cpu1.add(cpu2.snoop(m), 'S');
						//increment transition counter by 2 since snoop sets value to be S as well
						mesi[1][2] = mesi[1][2] + 2;
					} else { //cpu2 doesnt have this, fetch from memory
						if (m.dAddress < FIRST_MEM_SIZE) {
							memCycles += FIRST_MEM_LATENCY;
						} else {
							memCycles += SECOND_MEM_LATENCY;
						}
						//add into CPU1's L1d
						cpu1.add(m, 'E');
					}				
				} else { //not a read instruction
					int index = m.iAddress & (L3_SIZE/NUM_OF_WAYS - 1);
					int tag = m.iAddress >> (int)(Math.log(L3_SIZE/NUM_OF_WAYS) / Math.log(2));
					boolean found = false;
					//look through L3 set for item
					for (int i = 0; i < NUM_OF_WAYS && i + index < L3.cacheSize; i++) {
						if (L3.entries[index + i].tag == tag) {							
							l3hitNum++;
							found = true;
						} 
					}
					//not found in L3, denote a L3 miss and make cpu1 add to its L1
					if (!found) {
						l3missNum++;
						cpu1.add(m, 'E');
					}
				}				
			} else { //CPU 2 made this call
				//trying to do a read
				if (m.ioValue == 0) {
					//cpu1 does contain this item
					if(cpu1.snoop(m).iAddress != -1) {
						cpu2.add(cpu1.snoop(m), 'S');
						//increment transition counter by 2 since snoop sets value to be S as well
						mesi[1][2] = mesi[1][2] + 2;
					} else {
						if (m.dAddress < FIRST_MEM_SIZE) {
							memCycles += FIRST_MEM_LATENCY;
						} else {
							memCycles += SECOND_MEM_LATENCY;
						}
						//add into CPU2's L1d
						cpu2.add(m, 'E');
					}
				} else { //not a read instruction
					int index = m.iAddress & (L3_SIZE/NUM_OF_WAYS - 1);
					int tag = m.iAddress >> (int)(Math.log(L3_SIZE/NUM_OF_WAYS) / Math.log(2));
					boolean found = false;
					//Look in L3 set to see if it is contained
					for (int i = 0; i < NUM_OF_WAYS && i + index < L3.cacheSize; i++) {
						if (L3.entries[index + i].tag == tag) {							
							l3hitNum++;
							found = true;
						} 
					}
					//Not found in L3. Increment miss counter and make cpu2 insert into L1
					if (!found) {
						l3missNum++;
						cpu2.add(m, 'E');
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
			for (int i = 0; i < NUM_OF_WAYS && i + index < L3.cacheSize; i++) {
				if(L3.entries[index + i].tag == -1) {
					L3.insert(new MemoryInfo((Integer)arg, -1, -1), index + i, tag, 'E');
					//Item was placed in L3
					placed = true;
				}
			}
			//Item was not placed in an empty slot, something needs to be evicted
			if (!placed) {
				L3.insert(new MemoryInfo((Integer)arg, -1, -1), index + r.nextInt(NUM_OF_WAYS), tag, 'E');
			}
		}
		
		//a CPU has done a data-write call
		if (arg == CacheEvent.DATA_WRITE) {
			memCycles += SECOND_MEM_LATENCY;
		}
		
		//a CPU has modified its data
		if (arg instanceof CacheModification) {
			CacheModification cm = (CacheModification)arg;
			//update mesi trackers
			if (cm.startState == 'E') {
				if (cm.endState == 'M') {
					mesi[1][0] = mesi[1][0] + 1;
					if (((CPU)o).cpuNumber == 1) {
						cpu2.invalidateData(cm.mem);
					} else {
						cpu1.invalidateData(cm.mem);
					}
				} else if (cm.endState == 'I') {
					mesi[1][3] = mesi[1][3] + 1;
				}
			} else if (cm.startState == 'S') {
				if (cm.endState == 'M') {
					mesi[2][0] = mesi[2][0] + 1;
					if (((CPU)o).cpuNumber == 1) {
						cpu2.invalidateData(cm.mem);
					} else {
						cpu1.invalidateData(cm.mem);
					}
				} else if (cm.endState == 'I') {
					mesi[2][3] = mesi[2][3] + 1;
				}
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
				System.out.format("\n[L3] Hits: %d Misses: %d\n\n", l3hitNum, l3missNum);
				int hits = l3hitNum + cpu1.l1hitNum + cpu1.l2hitNum + cpu2.l1hitNum + cpu2.l2hitNum;
				int misses = l3missNum + cpu1.l1missNum + cpu1.l2missNum + cpu2.l1missNum + cpu2.l2missNum;
				int cycles = l3missNum * L3_LATENCY + cpu1.l1missNum * L1_LATENCY + cpu1.l2missNum * L2_LATENCY + cpu2.l1missNum * L1_LATENCY + cpu2.l2missNum * L2_LATENCY;
				System.out.format("Total hits: %d Total Misses: %d Total Cycles: %d\n", hits, misses, cycles + memCycles);
				float hitP = ((float)hits/(hits+misses)) * 100;
				float missP = ((float)misses/(hits+misses)) * 100;
				System.out.format("Hit Percentage: %.2f%% Miss Percentage: %.2f%%\n\n", hitP, missP);
				System.out.format("MESI STATE CHANGES\nE to S %d\nE to I %d\nE to M %d\n", mesi[1][2], mesi[1][3], mesi[1][0]);
				System.out.format("S to I %d\nS to M %d", mesi[2][3], mesi[2][0]);
			}
		}
	}
}
