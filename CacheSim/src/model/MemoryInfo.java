package model;

public class MemoryInfo {
	
	protected int iAddress;
	protected int ioValue;
	protected int dAddress;
	
	public MemoryInfo(final int theInstructionAddress, final int theIOValue, 
			final int theDataAddress) {
		iAddress = theInstructionAddress;
		ioValue = theIOValue;
		dAddress = theDataAddress;
	}

}
