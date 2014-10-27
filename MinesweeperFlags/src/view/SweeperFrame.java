package view;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import model.Board;

@SuppressWarnings("serial")
public class SweeperFrame extends JFrame {
	
	private final Board myBoard;
	
	public SweeperFrame(final Board theBoard) {
		super();
		
		myBoard = theBoard;
		
		constructFrame();
		
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
	}

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
	
	private JToggleButton createCell(final int theRow, final int theColumn) {
		final JToggleButton button = new JToggleButton();
		button.setBackground(Color.GRAY);
		button.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
		button.setForeground(Color.WHITE);
		
		button.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				myBoard.selectCell(theRow, theColumn);
				button.setText(myBoard.getCell(theRow, theColumn).toString());
				button.setEnabled(false);
			}
		});
		
		return button;
	}
	
	 public static void main(final String... the_args) {

	        EventQueue.invokeLater(new Runnable() {
	            @Override
	            public void run() {
	                new SweeperFrame(new Board(20, 20, 100)); // create the graphical user interface
	            }
	        });
	    }

}
