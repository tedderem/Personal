package view;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Observable;
import java.util.Observer;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import control.BoardEvents;
import model.Board;
import model.Cell;

/**
 * The GUI representation of a Minesweeper Flags game. 
 * 
 * @author Erik Tedder
 * @version 10/27/2014
 */
@SuppressWarnings("serial")
public class SweeperFrame extends JFrame implements Observer {
	
	/** The Board representing the current game. */
	private final Board myBoard;
	/** The arrangement of JToggleButtons that make up the various cells within the game. */
	private JToggleButton[][] myCells;
	
	/**
	 * Single Argument constructor of a new SweeperFrame. Constructor requires a Board to be 
	 * able to create the desired layout of the Minesweeper Flag game.
	 * 
	 * @param theBoard The board to be displayed.
	 */
	public SweeperFrame(final Board theBoard) {
		super();
		
		myBoard = theBoard;
		myCells = new JToggleButton[myBoard.getWidth()][myBoard.getHeight()];
		myBoard.addObserver(this);
		
		constructFrame();
		
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
	}

	/**
	 * Method to construct the overall frame of the GUI and populates the frame with the 
	 * desired number of cells.
	 */
	private void constructFrame() {
		JPanel gamePanel = new JPanel();	
		gamePanel.setLayout(new BoxLayout(gamePanel, BoxLayout.Y_AXIS));
		
		for(int i = 0; i < myBoard.getHeight(); i++) {
			JPanel panel = new JPanel();
			panel.setLayout(new GridLayout(1, myBoard.getWidth()));
			for(int j = 0; j < myBoard.getWidth(); j++) {
				panel.add(createCell(i, j));
			}
			gamePanel.add(panel);
		}
		
		add(gamePanel);
	}
	
	/**
	 * Method which creates all the Cells within the GUI for the user's interaction. Each cell 
	 * is made of a JToggleButton and notifies the board when a cell has been selected.
	 * 
	 * @param theRow The row value for the current Cell.
	 * @param theColumn The column for the current Cell.
	 * @return The JToggleButton representing the required cell.
	 */
	private JToggleButton createCell(final int theRow, final int theColumn) {
		final JToggleButton button = new JToggleButton();
		button.setText(" ");
		button.setFocusable(false);
		myCells[theRow][theColumn] = button;
		button.setBackground(Color.GRAY);
		button.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
		button.setForeground(Color.WHITE);
		
		//Action for when the cell has been selected
		button.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!myBoard.isGameOver()) 
					myBoard.selectCell(theRow, theColumn);
			}
		});
		
		return button;
	}
	
	 /**
	  * Main method for starting the SweeperFrame GUI and setting of the current board size.
	  * @param the_args
	  */
	 public static void main(final String... the_args) {

	        EventQueue.invokeLater(new Runnable() {
	            @Override
	            public void run() {
	                new SweeperFrame(new Board(20, 20, 20)); //create the GUI
	            }
	        });
	    }

	 /**
	  * Updates the Minesweeper Flags game board to reflect the actions within of the board
	  * model.
	  */
	@Override
	public void update(final Observable theObserable, final Object theArgument) {
		//Argument is a Cell. A cell has been selected and must be visually represented.
		if (theArgument instanceof Cell) {
			Cell c = (Cell)theArgument;
			int row = (int)c.getLocation().getX();
			int column = (int)c.getLocation().getY();
			myCells[row][column].setText(myBoard.getCell(row, column).toString());
			myCells[row][column].setSelected(true);
			myCells[row][column].setEnabled(false);
		}
		
		if (theArgument == BoardEvents.GAME_OVER) {
			JOptionPane.showMessageDialog(this, "GAME OVER");
			for (JToggleButton[] row : myCells) {
				for (JToggleButton b : row) {
					b.setEnabled(false);
				}
			}
		}

	}

}
