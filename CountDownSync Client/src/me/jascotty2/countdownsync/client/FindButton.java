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

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

public class FindButton {

	static int colorDelta = 45;
	static double closeEpsilon = 0.65; // % match
	static CaptureScreen scr = new CaptureScreen();
	static Rectangle buttonBounds = null;
	static Point lastPoint = new Point(0, 0);
	static BufferedImage target1, target2;
	static boolean has_found = false;

	static Point findPoint() {
		return findPoint(false);
	}

	static Point findPoint(boolean extensive) {
		Point pt = new Point(-1, -1);
		boolean running = false;
		try {
			Process p = Runtime.getRuntime().exec(System.getenv("windir") + "\\system32\\" + "tasklist.exe");

			BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line;
			while ((line = input.readLine()) != null && !running) {
				if (line.startsWith("WorldOfTanks.exe")) {
					running = true;
				}
			}
			input.close();

		} catch (IOException ex) {
			Logger.getLogger(FindButton.class.getName()).log(Level.SEVERE, null, ex);
		}
		if (running && !has_found) { // !still_is_button()) {
			// don't know if its possible to get the active window, so assume that is active & fullscreen
			buttonBounds = new Rectangle(0, 0, 0, 0);
			// guesses bassed off of my screen:
			// x 603 - 758 (155) (-9) / 1370
			// y 28 - 66 (38) / 621
			buttonBounds.width = 155; //(int) (scr.screenRectangle.width / 8.8);
			buttonBounds.height = 38; //buttonBounds.width / 4;
			buttonBounds.x = (int) ((CaptureScreen.screenRectangle.width / 2.) - (buttonBounds.width / 2.)) - 2;
			buttonBounds.y = 28; //(int) (CaptureScreen.screenRectangle.height / 22.2) - 6;

			// chaos screen:
			// x 496 - 650 (154) (-3) / 1152
			// y 28 - 67 (39) / 864
			
			// lucky w/ 1024 / 768
			// x 432 - 586 (154) (-3) / 1024
			// y 28 - 67 (39) / 768

			scr.screenCap(buttonBounds);
			for (int i = 0; i < 5; ++i) {
				if (!is_close(scr.screenImage)) {
					buttonBounds.y += 5;
					scr.screenCap(buttonBounds);
					if (i == 4) {
						return pt;
					}
				} else {
					// assume is the button
					pt.x = buttonBounds.x + buttonBounds.width / 2;
					pt.y = buttonBounds.y + buttonBounds.height / 2;
				}
			}
			
			if (pt.x < 0) {
				// make a guess for the image
				buttonBounds.y = 28;
				scr.screenCap(buttonBounds);
			}

			if (extensive) {
				Point p = extensiveSearch();
				if (p != null) {
					buttonBounds.x = p.x;
					buttonBounds.y = p.y;
					pt.x = buttonBounds.x + buttonBounds.width / 2;
					pt.y = buttonBounds.y + buttonBounds.height / 2;
				}
			}

			// else,
			lastPoint = pt;
		}
		return pt;
	}

	static Point extensiveSearch() {
		BufferedImage img = scr.screenCap();
		HashMap<Point, Double> finds = new HashMap<Point, Double>();

		if (target1 == null || target2 == null) {
			try {
				target1 = ImageIO.read(FindButton.class.getResource("resources/target.png"));
				target2 = ImageIO.read(FindButton.class.getResource("resources/target_2.png"));
				// these must have the same dimensions..
				if (target1.getWidth() != target2.getWidth() || target1.getWidth() != target2.getWidth()) {
					Logger.getAnonymousLogger().log(Level.SEVERE, null, new Exception("compare images are not the same size"));
					return null;
				}
			} catch (IOException ex) {
				Logger.getAnonymousLogger().log(Level.SEVERE, null, ex);
				return null;
			}
		}

		for (int x = 0; x < img.getWidth() - target1.getWidth(); ++x) {
			for (int y = 0; y < img.getHeight() - target1.getHeight(); ++y) {
				double per = imageMatch(img, x, y, target1, 10);
				double per2 = imageMatch(img, x, y, target2, 10);
				if (per > closeEpsilon || per2 > closeEpsilon) {
					if (per2 > per) {
						finds.put(new Point(x, y), per2);
					} else {
						finds.put(new Point(x, y), per);
					}
				}
			}
		}
		if (finds.size() > 0) {
			Point[] points = finds.keySet().toArray(new Point[0]);
			double max = finds.get(points[0]);
			int maxi = 0;
			for (int i = 1; i < points.length; ++i) {
				double d = finds.get(points[i]);
				if (d > max) {
					max = d;
					maxi = i;
				}
			}
			return points[maxi];
		}
		return null;
	}

	static boolean is_close(BufferedImage img) {
		return is_close(img, colorDelta);
	}

	static boolean is_close(BufferedImage img, int delta) {
		try {
			if (target1 == null || target2 == null) {
				target1 = ImageIO.read(FindButton.class.getResource("resources/target.png"));
				target2 = ImageIO.read(FindButton.class.getResource("resources/target_2.png"));
			}
			long num = 0, match = 0, match2 = 0;
			for (int x = 0; x < img.getWidth() && x < target1.getWidth(); ++x) {
				for (int y = 0; y < img.getHeight() && y < target1.getHeight(); ++y) {
					++num;
					int argb = img.getRGB(x, y);
					//int r = (argb) & 0xFF;
					//int g = (argb >> 8) & 0xFF;
					//int b = (argb >> 16) & 0xFF;
					//int a = (argb >> 24) & 0xFF;
					int argb1 = target1.getRGB(x, y);
					if (Math.abs(((argb1) & 0xFF) - ((argb) & 0xFF)) <= delta
							&& Math.abs(((argb1 >> 8) & 0xFF) - ((argb >> 8) & 0xFF)) <= delta
							&& Math.abs(((argb1 >> 16) & 0xFF) - ((argb >> 16) & 0xFF)) <= delta) {
						++match;
					}
					int argb2 = target2.getRGB(x, y);
					if (Math.abs(((argb2) & 0xFF) - ((argb) & 0xFF)) <= delta
							&& Math.abs(((argb2 >> 8) & 0xFF) - ((argb >> 8) & 0xFF)) <= delta
							&& Math.abs(((argb2 >> 16) & 0xFF) - ((argb >> 16) & 0xFF)) <= delta) {
						++match2;
					}
				}
			}
//			System.out.println(match / (double) num);
//			System.out.println(match2 / (double) num);
			return match / (double) num >= closeEpsilon || match2 / (double) num >= closeEpsilon;
		} catch (Exception ex) {
			Logger.getLogger(FindButton.class.getName()).log(Level.SEVERE, null, ex);
		}
		return false;
	}

	static double imageMatch(BufferedImage img, int ix, int iy, BufferedImage img2, int delta) {
		long num = 0, match = 0;
		for (int x = 0; x + ix < img.getWidth() && x < img2.getWidth(); ++x) {
			for (int y = 0; y + iy < img.getHeight() && y < img2.getHeight(); ++y) {
				++num;
				int argb1 = img.getRGB(x + ix, y + iy);
				int argb2 = target1.getRGB(x, y);
				//int r = (argb) & 0xFF;
				//int g = (argb >> 8) & 0xFF;
				//int b = (argb >> 16) & 0xFF;
				//int a = (argb >> 24) & 0xFF;
				if (Math.abs(((argb1) & 0xFF) - ((argb2) & 0xFF)) <= delta
						&& Math.abs(((argb1 >> 8) & 0xFF) - ((argb2 >> 8) & 0xFF)) <= delta
						&& Math.abs(((argb1 >> 16) & 0xFF) - ((argb2 >> 16) & 0xFF)) <= delta) {
					++match;
				}
			}
		}
		return match / (double) num;
	}

	static boolean still_is_button() {
		if (buttonBounds == null || buttonBounds.width <= 0) {
			return false;
		}
		scr.screenCap(buttonBounds);
		return is_close(scr.screenImage);
	}

	static void move_mouse() {
		if (lastPoint.x > 0) {
			Robot mover;
			try {
				mover = new Robot();
				mover.mouseMove(lastPoint.x, lastPoint.y);
			} catch (Exception exception) {
				Logger.getLogger(FindButton.class.getName()).log(Level.SEVERE, null, exception);
			}
		}
	}

	static void clickButton() {
		if (lastPoint.x > 0) {
			Robot mover;
			try {
				mover = new Robot();
				mover.mouseMove(lastPoint.x, lastPoint.y);
				mover.delay(200);
				mover.mousePress(InputEvent.BUTTON1_MASK);
				mover.delay(500);
				mover.mouseRelease(InputEvent.BUTTON1_MASK);
			} catch (Exception exception) {
				Logger.getLogger(FindButton.class.getName()).log(Level.SEVERE, null, exception);
			}
		}
	}
//
//	public static void main(String args[]) throws InterruptedException {
//
//		//Thread.sleep(5000);
//
//		Point pt = findPoint();
//		if (pt.x > 0) {
//			System.out.println("found!");
//			move_mouse();
//			//clickButton();
//			//Thread.sleep(3000);
//		}
//
//		javax.swing.JFrame myFrame = new javax.swing.JFrame("Capture Screen");
//
//		myFrame.add(scr);
//
//		try {
//			BufferedImage target = ImageIO.read(FindButton.class.getResource("resources/target.png"));
//			myFrame.setIconImage(target);
//		} catch (Exception ex) {
//			Logger.getLogger(FindButton.class.getName()).log(Level.SEVERE, null, ex);
//		}
//
//
//		myFrame.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
//		myFrame.setSize(buttonBounds.width, buttonBounds.height);
//
//		System.out.println(buttonBounds.x + ", " + buttonBounds.y + " - " + buttonBounds.width + " x " + buttonBounds.height);
//		myFrame.setVisible(true);
//
//	}
}
