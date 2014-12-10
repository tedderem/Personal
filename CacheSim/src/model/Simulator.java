package model;

/**
 * Caching Simulator program that reads in address traces and allocates them appropriately to 
 * cache locations. 
 * 
 * @author Erik Tedder
 */
public class Simulator {
	
	/** Size of first level memory from 0 to this value. Anything beyond constitutes second
	 * level memory.
	 *  */
	private final static int FIRST_MEM_SIZE = 0x800000;
	
	/**
	 * Some constructor.
	 */
	public Simulator() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * Testing purposes currently. 
	 * 
	 * @param theArgs Command-line inputs (not applicable to this program).
	 */
	public static void main(String... theArgs) {
		System.out.format("The size of first level memory in hex 0x%x\n", FIRST_MEM_SIZE);
		System.out.format("The size of first level memory in dec %d\n", FIRST_MEM_SIZE);
		System.out.format("The size of first level memory in binary %s", Integer.toBinaryString(FIRST_MEM_SIZE));
	}
}
