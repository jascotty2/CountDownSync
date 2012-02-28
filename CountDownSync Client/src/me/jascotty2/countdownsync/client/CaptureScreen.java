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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

public class CaptureScreen extends Canvas {

	static Rectangle screenRectangle = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()); //1152, 864);//
	Robot myRobot;
	BufferedImage screenImage;

	public CaptureScreen() {
		try {
			myRobot = new Robot();
		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}

	public BufferedImage screenCap() {
		screenRectangle = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
		return screenImage = myRobot.createScreenCapture(screenRectangle);
	}

	public BufferedImage screenCap(Rectangle r) {
//		try {
//			screenImage = javax.imageio.ImageIO.read(new File("D:\\Jacob\\Programs\\Java\\CountDownSync Client\\shot_001.jpg"));
//			BufferedImage img = new BufferedImage(r.width, r.height, screenImage.getType());
//			for(int x = r.x; x < r.x + r.width; ++x) {
//				for(int y = r.y; y < r.y + r.height; ++y) {
//					img.setRGB(x - r.x, y - r.y, screenImage.getRGB(x, y));
//				}
//			}
//			return screenImage = img;
//		} catch (IOException ex) {
//			Logger.getLogger(CaptureScreen.class.getName()).log(Level.SEVERE, null, ex);
//		}
		return screenImage = myRobot.createScreenCapture(r);
	}

	public void save(File sv) {
		try {
			// create a graphics context from the BufferedImage and draw the icon's image into it.
			Graphics g = screenImage.createGraphics();
			g.drawImage(screenImage, 0, 0, null);
			g.dispose();
			
			if(!sv.getAbsolutePath().toLowerCase().endsWith(".png")) {
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
