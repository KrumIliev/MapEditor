package com.kdi.editor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class Panel extends JPanel implements Runnable, KeyListener, MouseListener, MouseMotionListener, MouseWheelListener {

	private static final long serialVersionUID = 2296476942571145158L;

	public static final int WIDTH = 1280; // Window width
	public static final int HEIGHT = 720; // Window height

	public int tileSize;
	public int numRows, numColumns;

	private String sheetPath;
	private BufferedImage tileset;
	private BufferedImage[][] tiles;
	private int numTiles;

	private Thread thread;
	private int FPS = 30;
	private long targetTime = 1000 / FPS;

	private BufferedImage image;
	private Graphics2D graphics;

	private Block[] blocks;
	private int currentBlock;

	private int[][] map;
	private int mapWidth;
	private int mapHeight;

	private int xmap;
	private int ymap;
	private boolean shiftDown;
	private int mmx;
	private int mmy;
	private int xblock;
	private boolean ctrlDown;
	private boolean altDown;

	private int mousex;
	private int mousey;
	private int tilex;
	private int tiley;

	public Panel(String sheetPath, int tileSize) {
		this.mapWidth = 10;
		this.mapHeight = 5;
		this.sheetPath = sheetPath;
		this.tileSize = tileSize;

		setPreferredSize(new Dimension(WIDTH, HEIGHT));
		setFocusable(true);
		requestFocus();
	}

	public void addNotify() {
		super.addNotify();
		if (thread == null) {
			thread = new Thread(this);
			thread.start();
		}
		addKeyListener(this);
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
	}

	@Override
	public void run() {
		init();

		long start;
		long elapsed;
		long wait;

		while (true) {
			start = System.nanoTime();

			//update();
			render();
			draw();

			elapsed = (System.nanoTime() - start) / 1000000;
			wait = targetTime - elapsed;
			if (wait < 0) wait = 10;

			try {
				Thread.sleep(wait);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void init() {
		image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
		graphics = (Graphics2D) image.getGraphics();
		map = new int[mapHeight][mapWidth];

		try {
			tileset = ImageIO.read(new File(sheetPath));
			numColumns = tileset.getWidth() / tileSize;
			numRows = tileset.getHeight() / tileSize;
			numTiles = numColumns * numRows;
			tiles = new BufferedImage[numRows][numColumns];

			for (int row = 0; row < numRows; row++) {
				for (int col = 0; col < numColumns; col++) {
					tiles[row][col] = tileset.getSubimage(tileSize * col, tileSize * row, tileSize, tileSize);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		blocks = new Block[numTiles];
		int counter = 0;
		for (int row = 0; row < numRows; row++) {
			for (int col = 0; col < numColumns; col++) {
				blocks[counter] = new Block(tiles[row][col]);
				blocks[counter].setPosition(col * tileSize, (HEIGHT - tileSize * numRows) + row * tileSize);
				counter++;
			}
		}
	}

	public void render() {
		graphics.setColor(Color.BLACK);
		graphics.fillRect(0, 0, WIDTH, HEIGHT);

		// draw map
		for (int row = 0; row < mapHeight; row++)
			for (int col = 0; col < mapWidth; col++) {
				try {
					graphics.drawImage(blocks[map[row][col]].getImage(), col * tileSize + xmap, row * tileSize + ymap, null);
				} catch (Exception e) {}
			}

		// draw map dimensions
		graphics.setColor(Color.RED);
		graphics.drawRect(xmap, ymap, mapWidth * tileSize, mapHeight * tileSize);

		// draw clickable blocks
		int bo = HEIGHT - numRows * tileSize;
		graphics.setColor(Color.WHITE);
		graphics.fillRect(0, bo, WIDTH, numRows * tileSize);
		for (int i = 0; i < numTiles; i++) {
			blocks[i].draw(graphics);
		}

		// draw current block
		graphics.setColor(Color.RED);
		int width = numTiles / numRows;
		try {
			graphics.drawRect((currentBlock % width) * tileSize + xblock, bo + tileSize * (currentBlock / width), tileSize, tileSize);
		} catch (Exception e) {}

		// draw current block number
		graphics.drawString("" + currentBlock, WIDTH - 100, 50);

		// draw position
		graphics.setColor(Color.RED);
		graphics.drawString(mousex + ", " + mousey, WIDTH - 100, 20);
		graphics.drawString(tilex + ", " + tiley, WIDTH - 100, 35);
	}

	private void draw() {
		Graphics g2 = getGraphics();
		g2.drawImage(image, 0, 0, WIDTH, HEIGHT, null);
		g2.dispose();
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		int notches = e.getWheelRotation();
		if (notches < 0) {
			currentBlock--;
		} else {
			currentBlock++;
		}
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (SwingUtilities.isLeftMouseButton(e)) {
			int y = e.getY();
			if (y >= HEIGHT - numRows * tileSize) {} else {
				y = e.getY() - ymap;
				int x = e.getX() - xmap;
				if (x > 0 && x < mapWidth * tileSize && y > 0 && y < mapHeight * tileSize) {
					map[y / tileSize][x / tileSize] = currentBlock;
				}
			}
		} else if (SwingUtilities.isRightMouseButton(e)) {
			int y = e.getY() - ymap;
			int x = e.getX() - xmap;
			if (x > 0 && x < mapWidth * tileSize && y > 0 && y < mapHeight * tileSize) {
				map[y / tileSize][x / tileSize] = 0;
			}
		} else if (SwingUtilities.isMiddleMouseButton(e)) {
			int y = e.getY();
			int x = e.getX();
			int dx = (x - mmx) / tileSize;
			int dy = (y - mmy) / tileSize;
			if (dx != 0 || dy != 0) {
				mmx = e.getX();
				mmy = e.getY();
				xmap += dx * tileSize;
				ymap += dy * tileSize;
			}
		}
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		mousex = e.getX() - xmap;
		mousey = e.getY() - ymap;
		tilex = mousex / tileSize;
		tiley = mousey / tileSize;
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (SwingUtilities.isLeftMouseButton(e)) {
			int x = e.getX();
			int y = e.getY();

			int b = 0;

			if (y >= HEIGHT - numRows * tileSize) {
				// TODO 
				b = x / tileSize;
				if (y >= HEIGHT - (numRows - 1) * tileSize) b = x / tileSize + numTiles - numColumns * 2;
				if (y >= HEIGHT - (numRows - 2) * tileSize) b = x / tileSize + numTiles - numColumns;
				currentBlock = b;
			} else {
				y = e.getY() - ymap;
				x = e.getX() - xmap;
				if (x > 0 && x < mapWidth * tileSize && y > 0 && y < mapHeight * tileSize) map[y / tileSize][x / tileSize] = currentBlock;
			}
		} else if (SwingUtilities.isRightMouseButton(e)) {
			int y = e.getY() - ymap;
			int x = e.getX() - xmap;
			if (x > 0 && x < mapWidth * tileSize && y > 0 && y < mapHeight * tileSize) {
				map[y / tileSize][x / tileSize] = 0;
			}
		} else if (SwingUtilities.isMiddleMouseButton(e)) {
			mmx = e.getX();
			mmy = e.getY();
		}
	}

	@Override
	public void keyPressed(KeyEvent key) {
		int k = key.getKeyCode();
		if (k == KeyEvent.VK_SHIFT) {
			shiftDown = true;
		}
		if (k == KeyEvent.VK_CONTROL) {
			ctrlDown = true;
		}
		if (k == KeyEvent.VK_ALT) {
			altDown = true;
		}

		if (k == KeyEvent.VK_N) {
			if (ctrlDown) {
				mapWidth = 10;
				mapHeight = 8;
				map = new int[mapHeight][mapWidth];
			}
		}

		if (k == KeyEvent.VK_S) {
			if (ctrlDown) {
				try {
					String str = JOptionPane.showInputDialog(null, "Save file name", "Save Map", 1);
					if (str == null) return;
					BufferedWriter bw = new BufferedWriter(new FileWriter(str));
					bw.write(mapWidth + "\n");
					bw.write(mapHeight + "\n");
					for (int row = 0; row < mapHeight; row++) {
						for (int col = 0; col < mapWidth; col++) {
							bw.write(map[row][col] + " ");
						}
						bw.write("\n");
					}
					bw.close();
					System.out.println(str);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		if (k == KeyEvent.VK_O) {
			if (ctrlDown) {
				String str = null;
				BufferedReader br = null;
				try {
					str = JOptionPane.showInputDialog(null, "Open file name", "Open Map", 1);
					if (str == null) return;
					br = new BufferedReader(new FileReader(str));
					mapWidth = Integer.parseInt(br.readLine());
					mapHeight = Integer.parseInt(br.readLine());;
					map = new int[mapHeight][mapWidth];
					String delim = "\\s+";
					for (int row = 0; row < mapHeight; row++) {
						String line = br.readLine();
						String[] tokens = line.split(delim);
						for (int col = 0; col < mapWidth; col++) {
							map[row][col] = Integer.parseInt(tokens[col]);
						}
					}
				} catch (Exception e) {
					System.out.println("Couldn't load maps/" + str);
					e.printStackTrace();
				} finally {
					if (br != null) try {
						br.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				ctrlDown = false;
			}
		}

		if (k == KeyEvent.VK_RIGHT) {
			if (shiftDown) {
				mapWidth++;
				int[][] temp = new int[mapHeight][mapWidth];
				for (int row = 0; row < mapHeight; row++) {
					for (int col = 0; col < mapWidth - 1; col++) {
						temp[row][col] = map[row][col];
					}
				}
				map = temp;
			} else if (ctrlDown) {
				xblock += tileSize;
				int width = numTiles / numRows;
				for (int i = 0; i < width; i++) {
					blocks[i].setPosition(i * tileSize + xblock, HEIGHT - 2 * tileSize);
					blocks[i + width].setPosition(i * tileSize + xblock, HEIGHT - tileSize);
				}
			} else if (altDown) {
				for (int row = 0; row < mapHeight; row++) {
					for (int col = mapWidth - 1; col > 0; col--) {
						map[row][col] = map[row][col - 1];
					}
					map[row][0] = 0;
				}
			} else {
				xmap -= tileSize;
			}
		}

		if (k == KeyEvent.VK_LEFT) {
			if (shiftDown) {
				mapWidth--;
				int[][] temp = new int[mapHeight][mapWidth];
				for (int row = 0; row < mapHeight; row++) {
					for (int col = 0; col < mapWidth; col++) {
						temp[row][col] = map[row][col];
					}
				}
				map = temp;
			} else if (ctrlDown) {
				xblock -= tileSize;
				int width = numTiles / numRows;
				for (int i = 0; i < width; i++) {
					blocks[i].setPosition(i * tileSize + xblock, HEIGHT - 2 * tileSize);
					blocks[i + width].setPosition(i * tileSize + xblock, HEIGHT - tileSize);
				}
			} else if (altDown) {
				for (int row = 0; row < mapHeight; row++) {
					for (int col = 0; col < mapWidth - 1; col++) {
						map[row][col] = map[row][col + 1];
					}
					map[row][mapWidth - 1] = 0;
				}
			} else {
				xmap += tileSize;
			}
		}
		if (k == KeyEvent.VK_UP) {
			if (shiftDown) {
				mapHeight--;
				int[][] temp = new int[mapHeight][mapWidth];
				for (int row = 0; row < mapHeight; row++) {
					for (int col = 0; col < mapWidth; col++) {
						temp[row][col] = map[row][col];
					}
				}
				map = temp;
			} else if (altDown) {
				for (int col = 0; col < mapWidth; col++) {
					for (int row = 0; row < mapHeight - 1; row++) {
						map[row][col] = map[row + 1][col];
					}
					map[mapHeight - 1][col] = 0;
				}
			} else {
				ymap += tileSize;
			}
		}
		if (k == KeyEvent.VK_DOWN) {
			if (shiftDown) {
				mapHeight++;
				int[][] temp = new int[mapHeight][mapWidth];
				for (int row = 0; row < mapHeight - 1; row++) {
					for (int col = 0; col < mapWidth; col++) {
						temp[row][col] = map[row][col];
					}
				}
				map = temp;
			} else if (altDown) {
				for (int col = 0; col < mapWidth; col++) {
					for (int row = mapHeight - 1; row > 0; row--) {
						map[row][col] = map[row - 1][col];
					}
					map[0][col] = 0;
				}
			} else {
				ymap -= tileSize;
			}
		}

	}

	@Override
	public void keyReleased(KeyEvent key) {
		int k = key.getKeyCode();
		if (k == KeyEvent.VK_SHIFT) {
			shiftDown = false;
		}
		if (k == KeyEvent.VK_CONTROL) {
			ctrlDown = false;
		}
		if (k == KeyEvent.VK_ALT) {
			altDown = false;
		}
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {}

	@Override
	public void mouseClicked(MouseEvent arg0) {}

	@Override
	public void mouseEntered(MouseEvent arg0) {}

	@Override
	public void mouseExited(MouseEvent arg0) {}

	@Override
	public void keyTyped(KeyEvent arg0) {}

}
