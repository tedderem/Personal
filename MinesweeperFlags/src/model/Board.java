package model;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Random;

/**
 * Class which represents the board of the Minesweeper game. Makes up rows and columns of Cells
 * which contain a number representation of how many nearby mines there are, or whether the 
 * cell itself is a mine.
 * 
 * @author Erik Tedder
 * @version 1 (6/15/2014)
 */
public class Board extends Observable {
	
	/** Default board width. */
	private static final int DEFAULT_WIDTH = 10;
	
	/** Default board height. */
	private static final int DEFAULT_HEIGHT = 10;
	
	/** Default number of mines on the board. */
	private static final int DEFAULT_NUMBER_OF_MINES = 20;
	
	/** Two-dimensional array of Cells to represent the game's board. */
	private Cell[][] myBoard;
	
	/** Boolean representing the game being over. */
	private boolean myGameOver;
	
	/** The total number of mines in the current board. */
	private int myMineTotal;

	/** The height of this board. */
	private int myHeight;
	
	/** The width of this board. */
	private int myWidth;
	
	/**	Counter for the number of mines found within the game. Used to determine winning. */
	private int myMinesFound;
	
	/**
	 * Default, no-argument constructor of a new board with the default height, width, and
	 * number of mines.
	 */
	public Board() {
		this(DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_NUMBER_OF_MINES);
	}
	
	/**
	 * Constructor of a new Board based on the passed width, height, and total number of mines.
	 * 
	 * @param theWidth The desired width of the board.
	 * @param theHeight The desired height of the board.
	 * @param theTotalMines The total number of mines on the board.
	 */
	public Board(final int theWidth, final int theHeight, final int theTotalMines) {
		myWidth = theWidth;
		myHeight = theHeight;
		myMineTotal = theTotalMines;
		myMinesFound = 0;
		myGameOver = false;
		
		myBoard = new Cell[myHeight][myWidth];
		
		for (int i = 0; i < theHeight; i++) {
			for (int j = 0; j < theWidth; j++) {
				myBoard[i][j] = new Cell(i, j);
			}
		}
		
		populateBoard();
	}

	/**
	 * Getter method which returns the current state of the game.
	 * 
	 * @return Boolean of the game being over.
	 */
	public boolean isGameOver() {
		return myGameOver;
	}

	/**
	 * Method to set the current game to be over.
	 * 
	 * @param theGameOver The desired state of the game.
	 */
	public void setGameOver(final boolean theGameOver) {
		myGameOver = theGameOver;
	}

	/**
	 * Getter method of the total number of mines within the board.
	 * 
	 * @return int of the number of mines within the board.
	 */
	public int getMineNumber() {
		return myMineTotal;
	}

	/**
	 * Setter method for the number of mines to be in this board.
	 * 
	 * @param theMineNumber The desired number of mines within the board.
	 */
	public void setMineNumber(final int theMineNumber) {
		myMineTotal = theMineNumber;
	}

	/**
	 * Getter method for the height of the board.
	 * 
	 * @return int of the board's height.
	 */
	public int getHeight() {
		return myHeight;
	}

	/**
	 * Setter method for the height of the board.
	 * 
	 * @param theHeight The desired height of the board.
	 */
	public void setHeight(final int theHeight) {
		myHeight = theHeight;
	}

	/**
	 * Getter method which returns the width of the board.
	 * 
	 * @return int of the board width.
	 */
	public int getWidth() {
		return myWidth;
	}

	/**
	 * Setter method which sets the Width of the current Board.
	 * 
	 * @param theWidth The desired with of the board.
	 */
	public void setWidth(final int theWidth) {
		myWidth = theWidth;
	}
	
	/**
	 * Getter method for the number of mines left in the current game.
	 * 
	 * @return the number of mines found.
	 */
	public int getMinesLeft() {
		return myMineTotal - myMinesFound;
	}
	
	/**
	 * Method for selecting a cell on the board, making the respective cell being set to 
	 * selected. If the given cell has a count of 0, then selection is expanded in all 
	 * directions until no more adjacent empty cells are left unselected.
	 * 
	 * @param theRow The row to the cell.
	 * @param theColumn The column of the cell.
	 */
	public void selectCell(final int theRow, final int theColumn) {
		if (!myGameOver) {
			myBoard[theRow][theColumn].setSelected();
			setChanged();
			notifyObservers(getCell(theRow, theColumn));

			if (myBoard[theRow][theColumn].isBlank()) {
				blankSelected(theRow, theColumn);
			}

			if (myBoard[theRow][theColumn].isMine()) {
				myMinesFound++;
				//All mines have been found, game is now over
				if (myMinesFound == myMineTotal) {
					myGameOver = true;
					setChanged();
					notifyObservers();
				}
			}
		}
	}
	
	public Cell getCell(final int theRow, final int theColumn) {
		return myBoard[theRow][theColumn];
	}
	
	/**
	 * Method which populates the current Board with mines.
	 */
	private void populateBoard() {
		Random r = new Random();
		int minesToPlace = myMineTotal;
		
		while (minesToPlace > 0) {
			int column = r.nextInt(myWidth);
			int row = r.nextInt(myHeight);
			
			//place a mine aslong as the current slot isn't already a mine.
			if (!myBoard[row][column].isMine()) {
				myBoard[row][column].setAsMine();
				updateNeighbors(row, column);
				minesToPlace--;
			}
		}		
	}
	
	/**
	 * Method that will expand empty spaces of the board when a user selects a cell that is
	 * empty.
	 * 
	 * @param theRow The selected empty row value.
	 * @param theColumn The selected empty column value.
	 */
	private void blankSelected(final int theRow, final int theColumn) {
		ArrayList<Cell> neighbors = (ArrayList<Cell>)getNeighbors(theRow, theColumn);
		
		for (Cell c : neighbors) {
			if (!c.isSelected())
				selectCell((int)c.getLocation().getX(), (int)c.getLocation().getY());		
		}
	}
	
	/**
	 * Method that finds all the available neighbors of the current cell (denoted by the row
	 * and the column of the cell).
	 * 
	 * @param theRow The row of the cell.
	 * @param theColumn The column of the cell.
	 * @return A list of cells.
	 */
	private List<Cell> getNeighbors(final int theRow, final int theColumn) {
		ArrayList<Cell> neighbors = new ArrayList<Cell>();
		
		if (theRow == 0) {
			neighbors.add(myBoard[theRow + 1][theColumn]);
			
			if (theColumn == 0) {
				neighbors.add(myBoard[theRow + 1][theColumn + 1]);
				neighbors.add(myBoard[theRow][theColumn + 1]);
			} else if (theColumn == (myWidth - 1)) {
				neighbors.add(myBoard[theRow][theColumn - 1]);
				neighbors.add(myBoard[theRow + 1][theColumn - 1]);
			} else {
				neighbors.add(myBoard[theRow][theColumn - 1]);
				neighbors.add(myBoard[theRow + 1][theColumn - 1]);
				neighbors.add(myBoard[theRow + 1][theColumn + 1]);
				neighbors.add(myBoard[theRow][theColumn + 1]);
			}
		} else if (theRow == (myHeight - 1)) {
			neighbors.add(myBoard[theRow - 1][theColumn]);
			
			if (theColumn == 0) {
				neighbors.add(myBoard[theRow - 1][theColumn + 1]);
				neighbors.add(myBoard[theRow][theColumn + 1]);
			} else if (theColumn == (myWidth - 1)) {
				neighbors.add(myBoard[theRow][theColumn - 1]);
				neighbors.add(myBoard[theRow - 1][theColumn - 1]);
			} else {
				neighbors.add(myBoard[theRow][theColumn - 1]);
				neighbors.add(myBoard[theRow - 1][theColumn - 1]);
				neighbors.add(myBoard[theRow - 1][theColumn + 1]);
				neighbors.add(myBoard[theRow][theColumn + 1]);
			}
		} else {
			neighbors.add(myBoard[theRow - 1][theColumn]);			
			neighbors.add(myBoard[theRow + 1][theColumn]);			
			
			if (theColumn == 0) {
				neighbors.add(myBoard[theRow - 1][theColumn + 1]);
				neighbors.add(myBoard[theRow][theColumn + 1]);
				neighbors.add(myBoard[theRow + 1][theColumn + 1]);
			} else if (theColumn == (myWidth - 1)) {
				neighbors.add(myBoard[theRow][theColumn - 1]);
				neighbors.add(myBoard[theRow - 1][theColumn - 1]);
				neighbors.add(myBoard[theRow + 1][theColumn - 1]);
			} else {
				neighbors.add(myBoard[theRow - 1][theColumn + 1]);
				neighbors.add(myBoard[theRow][theColumn + 1]);
				neighbors.add(myBoard[theRow + 1][theColumn + 1]);
				neighbors.add(myBoard[theRow][theColumn - 1]);
				neighbors.add(myBoard[theRow - 1][theColumn - 1]);
				neighbors.add(myBoard[theRow + 1][theColumn - 1]);
			}
		}
		
		return neighbors;
	}
	
	/**
	 * Method which will updates the neighboring cells to reflect the number of mines 
	 * surrounding it. Method is called when initializing mine locations.
	 * 
	 * @param theRow The row of the mine cell.
	 * @param theColumn The column of the mine cell.
	 */
	private void updateNeighbors(final int theRow, final int theColumn) {
		for (Cell c : getNeighbors(theRow, theColumn)) {
			c.incrementCount();
		}
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * Returns a string representation of the current Minesweeper Board for debugging purposes.
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("  ");
		for(int i = 0; i < myWidth; i++) {	
			sb.append(i);
			sb.append(" ");
		}

		sb.append("\n");

		for(int i = 0; i < myHeight; i++) {
			sb.append(i);
			sb.append(" ");
			for (int j = 0; j < myWidth; j++) {		
				sb.append(myBoard[i][j]);
				sb.append(" ");
			}		
			sb.append("\n");
		}	
		
		return sb.toString();
	}
}
