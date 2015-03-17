package com.kdi.editor;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class Block {
	
	BufferedImage image;
	int x;
	int y;

	public Block(BufferedImage image) {
		this.image = image;
	}

	public void setPosition(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public BufferedImage getImage() {
		return image;
	}

	public void draw(Graphics2D graphics) {
		graphics.drawImage(image, x, y, null);
	}
}
