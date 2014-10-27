package model;

import java.awt.Point;

/**
 * Class which represents a single cell on the Minesweeper Flags board.
 * 
 * @author Erik Tedder
 * @version 1 (6/15/2014)
 */
public class Cell {
	
	private static final String MINE_ICON = "X";
	
	/** Field representing the cell having been selected. */
	private boolean myBeenSelected;
	
	/** Field representing the number of nearby mines for this cell. */
	private char myCount;
	
	/** The location of this cell. */
	private final Point myLocation;
	
	/** Field representing if this node is mine. */
	private Boolean myIsMine;
	
	/**
	 * Constructor of a new cell based upon a point.
	 * 
	 * @param thePoint The point of this cell.
	 */
	public Cell(final Point thePoint) {
		myBeenSelected = false;
		myIsMine = false;
		myCount = 0;
		myLocation = (Point) thePoint.clone();		
	}
	
	/**
	 * Constructor of a new cell based upon an x and y coordinate.
	 * 
	 * @param theX The x value of this cell's location.
	 * @param theY The y value of this cell's location.
	 */
	public Cell(final int theX, final int theY) {
		this(new Point(theX, theY));
	}
	
	/**
	 * Getter method which returns this cell's location.
	 * 
	 * @return A Point object denoting this cell's location.
	 */
	public Point getLocation() {
		return (Point) myLocation.clone();
	}
	
	/**
	 * Method which sets this cell to denote being selected.
	 */
	public void setSelected() {
		myBeenSelected = true;
	}
	
	/**
	 * Getter method which returns a boolean representing whether this cell has been selected
	 * already or not.
	 * 
	 * @return Whether this cell has been selected.
	 */
	public boolean isSelected() {
		return myBeenSelected;
	}
	
	/**
	 * Method which increments this cell's nearby mine counter.
	 */
	public void incrementCount() {
		if (!myIsMine)
			myCount++;
	}
	
	/**
	 * Method which returns the number of mines neighboring this cell.
	 * 
	 * @return the number of nearby mines.
	 */
	public int getCount() {
		return myCount;
	}
	
	/**
	 * Method which sets this cell to be a mine.
	 */
	public void setAsMine() {
		myIsMine = true;
	}
	
	/**
	 * Method which returns whether this cell is considered a mine.
	 * 
	 * @return
	 */
	public boolean isMine() {
		return myIsMine;
	}
	
	/**
	 * Method which returns whether this cell is blank.
	 * 
	 * @return
	 */
	public boolean isBlank() {
		return !myIsMine && myCount == 0;
	}
	
	@Override
	public String toString() {
		String str = " ";
		
		if (myCount != 0 && !myIsMine) {
			str = Integer.toString(myCount);
		} else if (myIsMine){
			str = MINE_ICON;
		}
		
		return str;
	}
	
}
