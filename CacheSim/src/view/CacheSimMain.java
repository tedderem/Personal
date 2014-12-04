package view;

import java.awt.EventQueue;
import java.awt.GraphicsConfiguration;
import java.awt.HeadlessException;

import javax.swing.JFrame;

public class CacheSimMain extends JFrame {
	
	public CacheSimMain() {
		super();
	}
	
	public static void main(final String... theArgs) {
		EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new CacheSimMain(); //create the GUI
            }
        });
	}
	
}
