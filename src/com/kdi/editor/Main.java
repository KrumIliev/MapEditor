package com.kdi.editor;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

public class Main {

	private static String sheetPath;
	private static int tileSize;

	public static void main(String[] args) {
		setSheetFile();
		setTileSize("Tile size");

		JFrame window = new JFrame("Tile Map Editor");
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setContentPane(new Panel(sheetPath, tileSize));
		window.setResizable(false);
		window.pack();
		window.setVisible(true);
	}

	private static void setSheetFile() {
		JFileChooser chooser = new JFileChooser();
		FileNameExtensionFilter filter = new FileNameExtensionFilter("PNG & GIF Images", "png", "gif");
		chooser.setAcceptAllFileFilterUsed(false);
		chooser.setFileFilter(filter);
		int returnVal = chooser.showOpenDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			sheetPath = chooser.getSelectedFile().getAbsolutePath();
		}
	}

	private static void setTileSize(String massage) {
		try {
			tileSize = Integer.valueOf(JOptionPane.showInputDialog(massage));
		} catch (Exception e) {
			setTileSize("Width must be number");
		}
	}
}
