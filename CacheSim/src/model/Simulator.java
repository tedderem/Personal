package model;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

/**
 * Caching Simulator program that reads in address traces and allocates them appropriately to 
 * cache locations. 
 * 
 * @author Erik Tedder
 */
public class Simulator implements Observer {
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
	private final static int CPU_TOTAL = 2;
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
	private int threadsComplete;
	/** List for the TRACE_FILE. */
	private ArrayList<MemoryInfo> trace;
	
	private CPU cpu1;
	private CPU cpu2;
	
	/**
	 * Some constructor.
	 */
	public Simulator() {
		missNum = 0;
		hitNum = 0;
		totalTime = 0;
		threadsComplete = 0;
		
		L3 = new Cache(L3_SIZE, L3_LATENCY, NUM_OF_WAYS);
		
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

		cpu1 = new CPU(trace, L1_SIZE, L1_LATENCY, L2_SIZE, L2_LATENCY, NUM_OF_WAYS, 1);
		cpu2 = new CPU(trace, L1_SIZE, L1_LATENCY, L2_SIZE, L2_LATENCY, NUM_OF_WAYS, 2);
		cpu1.addObserver(this);
		cpu2.addObserver(this);
		cpu1.start();
		cpu2.start();
		
		//Testing index and tag calculations
//		for (int i = 0; i < trace.size(); i++) {
//			int test = trace.get(i).iAddress;
//			System.out.format("Instruction Address In decimal %d in hex 0x%x\n", test, test);
//			System.out.format("Instruction Address In binary %s\n", Integer.toBinaryString(test));
//			int index = test & (L1_SIZE - 1);
//			int tag = test >> (int)(Math.log(L1_SIZE) / Math.log(2));
//			System.out.format("L1 Index %s and Tag %s\n", Integer.toBinaryString(index), Integer.toBinaryString(tag));
//			index = test & (L2_SIZE - 1);
//			tag = test >> (int)(Math.log(L2_SIZE) / Math.log(2));
//			System.out.format("L2 Index %s and Tag %s\n", Integer.toBinaryString(index), Integer.toBinaryString(tag));	
//			int recon = tag << (int)(Math.log(L2_SIZE) / Math.log(2));
//			recon += index;
//			System.out.format("Address reconstructed %s\n\n", Integer.toBinaryString(recon));			
//		}		
	}

	/**
	 * Testing purposes currently. 
	 * 
	 * @param theArgs Command-line inputs (not applicable to this program).
	 */
	public static void main(String... theArgs) {		
		Simulator s = new Simulator();		
	}

	@Override
	public void update(Observable o, Object arg) {
		if (arg == CacheEvent.L1_HIT) {
			hitNum++;
			totalTime += L1_LATENCY;
		}
		
		if (arg == CacheEvent.L2_HIT) {
			hitNum++;
			missNum++;
			totalTime += L1_LATENCY + L2_LATENCY;
		} 
		
		if (arg instanceof MemoryInfo) {
			missNum += 2;
			totalTime += L1_LATENCY + L2_LATENCY;
			MemoryInfo m = (MemoryInfo) arg;
			if (((CPU) o).cpuNumber == 1) {
				if(cpu2.snoop(m)) {
					cpu1.add(cpu2.get(m));
				} else {
					missNum++;
					totalTime += L3_LATENCY;
					int index = m.iAddress & (L3_SIZE - 1);
					int tag = m.iAddress >> (int)(Math.log(L3_SIZE) / Math.log(2));
					if (L3.entries[index].tag == tag) {
						hitNum++;
					} else {
						cpu1.add(m.iAddress);
					}
				}
				
			} else {
				if(cpu1.snoop(m)) {
					cpu2.add(cpu1.get(m));
				} else {
					missNum++;
					totalTime += L3_LATENCY;
					int index = m.iAddress & (L3_SIZE - 1);
					int tag = m.iAddress >> (int)(Math.log(L3_SIZE) / Math.log(2));
					if (L3.entries[index].tag == tag) {
						hitNum++;
					} else {
						cpu2.add(m.iAddress);
					}
				}
			}
		}
		
		if (arg instanceof Integer) {
			int index = (int)arg & (L3_SIZE - 1);
			int tag = (int)arg >> (int)(Math.log(L3_SIZE) / Math.log(2));;
			totalTime += L3_LATENCY;
			L3.insert(index, tag, 'E');
		}
		
		if (arg == CacheEvent.COMPLETE) {
			System.out.println("thread " + ((CPU) o).cpuNumber + " complete");
			if (threadsComplete != CPU_TOTAL) {
				threadsComplete++;
			}
			
			if (threadsComplete == CPU_TOTAL) {
				System.out.format("%d hits %d misses %d total cycles", hitNum, missNum, totalTime);
			}
		}
	}
}
