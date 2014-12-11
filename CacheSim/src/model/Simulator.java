package model;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * Caching Simulator program that reads in address traces and allocates them appropriately to 
 * cache locations. 
 * 
 * @author Erik Tedder
 */
public class Simulator {
	
	/** 
	 * Size of first level memory from 0 to this value. Anything beyond constitutes second
	 * level memory.
	 */
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
	/**	String name of the file for memory trace. */
	private final static String TRACE_FILE = "trace-2k.csv";
	
	/** The shared level 3 cache for the CPUs. */
	protected Cache L3;
	
	/** The total number of misses from caching. */
	private int missNum;
	/** The total number of hits from caching. */
	private int hitNum;
	/** The total time (in cycles) based off cache misses. */
	private int totalTime;
	/** List for the TRACE_FILE. */
	private ArrayList<MemoryInfo> trace;
	
	/**
	 * Some constructor.
	 */
	public Simulator() {
		trace = new ArrayList<MemoryInfo>();
		
		try {
			FileInputStream is = new FileInputStream(TRACE_FILE);
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
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
				
				//System.out.format("instruction address %d, IO value %d, data address %d\n", add, io, data);
				trace.add(new MemoryInfo(add, io, data));
				
				line = br.readLine();
			}
			
			is.close();
			br.close();
		} catch (FileNotFoundException e) {
			System.err.println("ERROR READING FILE");
		} catch (IOException e) {
			System.err.println("ISSUE READING LINE");
		}

		CPU cpu1 = new CPU(trace, L1_SIZE, L1_LATENCY, L2_SIZE, L2_LATENCY, NUM_OF_WAYS);
		CPU cpu2 = new CPU(trace, L1_SIZE, L1_LATENCY, L2_SIZE, L2_LATENCY, NUM_OF_WAYS);		
	}

	/**
	 * Testing purposes currently. 
	 * 
	 * @param theArgs Command-line inputs (not applicable to this program).
	 */
	public static void main(String... theArgs) {		
		Simulator s = new Simulator();		
	}
}
