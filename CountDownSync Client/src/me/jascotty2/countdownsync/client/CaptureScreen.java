/**
 * Copyright (C) 2012 Jacob Scott <jascottytechie@gmail.com>
 * Description: ( TODO )
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package me.jascotty2.countdownsync.client;

import java.awt.Robot;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Canvas;
import java.awt.Graphics;

import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

public class CaptureScreen extends Canvas {

	Robot robot;
	BufferedImage screenImage;

	public CaptureScreen() {
		try {
			robot = new Robot();
		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}

	public Rectangle getScreenRes() {
		return new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
	}

	public BufferedImage screenCap() {
		return screenImage = robot.createScreenCapture(getScreenRes());
	}

	public BufferedImage screenCap(Rectangle r) {
		return screenImage = robot.createScreenCapture(r);
	}

	public void sendPrintScr() {
		robot.keyPress(KeyEvent.VK_PRINTSCREEN);
		robot.delay(200);
		robot.keyRelease(KeyEvent.VK_PRINTSCREEN);
	}

	public BufferedImage loadFromFile(File f) {
		return loadFromFile(f, null);
	}

	public BufferedImage loadFromFile(File f, Rectangle r) {
		try {
			screenImage = javax.imageio.ImageIO.read(f);
			if (r != null) {
				return screenImage = subImage(screenImage, r);
			} else {
			}
		} catch (IOException ex) {
			Logger.getLogger(CaptureScreen.class.getName()).log(Level.SEVERE, null, ex);
			screenImage = null;
		}
		return screenImage;
	}

	public void save(File sv) {
		System.out.println(sv.getAbsoluteFile());
		try {
			// create a graphics context from the BufferedImage and draw the icon's image into it.
			Graphics g = screenImage.createGraphics();
			g.drawImage(screenImage, 0, 0, null);
			g.dispose();

			if (!sv.getAbsolutePath().toLowerCase().endsWith(".png")) {
				sv = new File(sv.getAbsolutePath() + ".png");
			}

			sv.createNewFile();
			ImageIO.write(screenImage, "png", sv);
		} catch (IOException ex) {
			Logger.getLogger(CaptureScreen.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	@Override
	public void paint(Graphics g) {
		g.drawImage(screenImage, 0, 0, this);
	}
	
	public static BufferedImage subImage(BufferedImage img, Rectangle r) {
		if(img == null || r == null) {
			throw new NullPointerException("Error: null");
		}
		BufferedImage sv = new BufferedImage(r.width, r.height, img.getType());
		for (int x = r.x; x < r.x + r.width && x < img.getWidth(); ++x) {
			for (int y = r.y; y < r.y + r.height && y < img.getHeight(); ++y) {
				sv.setRGB(x - r.x, y - r.y, img.getRGB(x, y));
			}
		}
		return sv;
	}
	
	
//	public static void main(String[] args) {
//		CaptureScreen cs = new CaptureScreen();
//		
//		javax.swing.JFrame myFrame=new javax.swing.JFrame("Capture Screen");
//
//		myFrame.add(cs);
//		
//		cs.screenCap(screenRectangle);
//		
//		myFrame.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
//		myFrame.setSize(Toolkit.getDefaultToolkit().getScreenSize().width,Toolkit.getDefaultToolkit().getScreenSize().height);
//		myFrame.setVisible(true);
//	}
}
