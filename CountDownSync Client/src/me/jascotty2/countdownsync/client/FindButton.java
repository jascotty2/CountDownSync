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
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

public class FindButton {

	static int colorDelta = 50;
	static double closeEpsilon = 0.65, // % match
			foundEpsilon = 0.95; // when to assume is absolutely the button (stop re-evaluating)
	static CaptureScreen scr = new CaptureScreen();
	static Rectangle buttonBounds = new Rectangle(0, 0, 155, 38);
	static Point lastPoint = new Point(0, 0);
	static BufferedImage target1, target2;
	static boolean button_found = false, positive_found = false;
	static final ARGB avg = new ARGB(182, 29, 26);
	static final int dR = 40 /*28*/, dG = 10 /*2*/, dB = 10 /*2*/;
	private static File findWot = null;
	protected static String wotFolder = null;
	private static File wotScr = null;

	public static void setFindWOT(String path) {
		findWot = new File(path);
	}

	static Point findPoint(boolean workaround) throws InterruptedException, IOException {
		Point pt = new Point(-1, -1);
		if (target1 == null || target2 == null) {
			target1 = ImageIO.read(FindButton.class.getResource("resources/target.png"));
			target2 = ImageIO.read(FindButton.class.getResource("resources/target_2.png"));
		}
		if (!wot_active(workaround)) {
			return pt;
		}
		if (!still_is_button(workaround) && !positive_found) { // !button_found) { //

			BufferedImage search;
			Rectangle searchBounds = new Rectangle(scr.getScreenRes().width / 3, 5, 0, 170);
			searchBounds.width = searchBounds.x;

			// trying to search for the red in the box

			if (workaround) {
				search = printScreenWorkaround(searchBounds);
				if (search == null) {
					return pt;
				}
			} else {
				search = scr.screenCap(searchBounds);
			}

			// now that have image, search for the button
			// first search for first red color match
			button_found = false;
			for (int y = 0; y < searchBounds.height - buttonBounds.height && !button_found; ++y) {
				for (int x = 0; x < searchBounds.width - buttonBounds.width && !button_found; ++x) {
					if (avg.is_close(ARGB.fromARGB(search.getRGB(x, y)), dR, dG, dB)) {
						//System.out.println("close at " + (x + searchBounds.x) + ", " + (y + searchBounds.y));
						// check a 16x16 square, see if all also close
						ColorAverage testavg = new ColorAverage();
						for (int x2 = x; x2 < 16 + x; ++x2) {
							for (int y2 = y; y2 < 16 + y; ++y2) {
								testavg.addColor(search.getRGB(x2, y2));
							}
						}
						if (!avg.is_close(testavg.getArgb(), dR, dG, dB)) {
							x += 16;
							continue;
						}

						// box will be towards the right of the corner, so move left some
						x -= 5;

						double per = imageMatch(target1, x, y, search, colorDelta);
						double per2 = imageMatch(target2, x, y, search, colorDelta);

						if (per >= closeEpsilon || per2 >= closeEpsilon) { //is_close(search, colorDelta)) {
							buttonBounds.x = x + searchBounds.x;
							buttonBounds.y = y + searchBounds.y;
							pt.x = buttonBounds.x + buttonBounds.width / 2;
							pt.y = buttonBounds.y + buttonBounds.height / 2;
							lastPoint = pt;
							button_found = true;
							positive_found = per >= foundEpsilon || per2 >= foundEpsilon;
							scr.screenImage = CaptureScreen.subImage(search, buttonBounds);
							System.out.println("button found at " + (x + searchBounds.x) + ", " + (y + searchBounds.y)
									+ String.format("  (%2.1f%% sure)", per > per2 ? per * 100 : per2 * 100));

						} else {
							x += 16 + 5;
						}
					}
				}
			}
			
			// if still not found, try old method
			if (!button_found) {
				// guesses bassed off of my screen:
				// x 603 - 758 (155) (-9) / 1370
				// y 28 - 66 (38) / 621

				// chaos screen:
				// x 496 - 650 (154) (-3) / 1152
				// y 28 - 67 (39) / 864

				// lucky w/ 1024 / 768
				// x 432 - 586 (154) (-3) / 1024
				// y 28 - 67 (39) / 768

				buttonBounds.x = (int) ((scr.getScreenRes().width / 2.) - (buttonBounds.width / 2.)) - 2;
				buttonBounds.y = 28;

				//scr.screenCap(buttonBounds);
				// use image from prior method


				for (int i = 5; i > 0 && !button_found; --i) {
					double per = imageMatch(target1,
							buttonBounds.x - searchBounds.x, buttonBounds.y - searchBounds.y,
							search, colorDelta);
					double per2 = imageMatch(target2,
							buttonBounds.x - searchBounds.x, buttonBounds.y - searchBounds.y,
							search, colorDelta);
					if (per < closeEpsilon && per2 < closeEpsilon) {//!is_close(scr.screenImage)) {
						if (i == 1) {
							// make a guess for the image
							buttonBounds.y = 28;
							scr.screenImage = CaptureScreen.subImage(search,
									new Rectangle(buttonBounds.x - searchBounds.x,
									buttonBounds.y - searchBounds.y,
									buttonBounds.width, buttonBounds.height));
							return pt;
						} else {
							buttonBounds.y += 5;
						}
					} else {
						// assume is the button
						pt.x = buttonBounds.x + buttonBounds.width / 2;
						pt.y = buttonBounds.y + buttonBounds.height / 2;

						button_found = true;
						positive_found = per >= foundEpsilon || per2 >= foundEpsilon;

						scr.screenImage = CaptureScreen.subImage(search,
								new Rectangle(buttonBounds.x - searchBounds.x,
								buttonBounds.y - searchBounds.y,
								buttonBounds.width, buttonBounds.height));
					}
				}
				lastPoint = pt;
			}
		}
		return pt;
	}

	static File newFile(File[] before, File[] after) {
		File nf = null;
		for (int i = 0; i < after.length; ++i) {
			boolean in = false;
			for (int j = 0; j < before.length; ++j) {
				if (after[i].equals(before[j])) {
					in = true;
					break;
				}
			}
			if (!in) {
				nf = after[i];
			}
		}
		return nf;
	}

	protected static BufferedImage printScreenWorkaround(Rectangle cap) throws InterruptedException {
		// assume already checked
//		if(!wot_active(true)) {
//			return null;
//		}
		BufferedImage ret = null;
		File[] before = wotScr.listFiles(), after;
		scr.sendPrintScr();
		Thread.sleep(300);
		after = wotScr.listFiles();
		for (int i = 0; before.length == after.length && i < 20; ++i) {
			Thread.sleep(100);
			after = wotScr.listFiles();
		}
		if (before.length == after.length) {
			System.out.println("Something seems to have gone wrong while looking for the PrintScreen file (no new file found)");
			return null;
		}
		// find new file
		File nf = newFile(before, after);
		if (nf == null) {
			System.out.println("Something went wrong with looking for the PrintScreen file...");
			return null;
		}
		if ((ret = scr.loadFromFile(nf, cap)) == null) {
			// try again
			Thread.sleep(500);
			if ((ret = scr.loadFromFile(nf, cap)) == null) {
				System.out.println("Error Loading " + nf.getAbsoluteFile());
			}
		}
		nf.delete();
		return ret;
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
					ARGB argb = ARGB.fromARGB(img.getRGB(x, y));
					ARGB argb1 = ARGB.fromARGB(target1.getRGB(x, y));
					if (argb.is_close(argb1, delta)) {
						++match;
					}
					ARGB argb2 = ARGB.fromARGB(target2.getRGB(x, y));
					if (argb.is_close(argb2, delta)) {
						++match2;
					}
				}
			}
//			System.out.println(match / (double) num);
//			System.out.println(match2 / (double) num);
			return (match / (double) num) >= closeEpsilon || (match2 / (double) num) >= closeEpsilon;
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
				ARGB argb1 = ARGB.fromARGB(img.getRGB(x + ix, y + iy));
				ARGB argb2 = ARGB.fromARGB(img2.getRGB(x, y));
				if (argb1.is_close(argb2, delta)) {
					++match;
				}
			}
		}
		return match / (double) num;
	}

	static boolean wot_active(boolean ensure_active) {
		boolean running = false;
		try {
			if (findWot != null && findWot.exists()) {
				Process p = Runtime.getRuntime().exec(findWot.getAbsolutePath());

				BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String line;
				int n = 0;
				while ((line = input.readLine()) != null) {
					if (line.equalsIgnoreCase("true")) {
						running = true;
					} else if (n == 1) {
						running = false;
						if (wotFolder != null) {
							break;
						}
					} else if (n == 2 && wotFolder == null && line.contains(File.separator)) {
						// line is path to exe
						wotFolder = line.substring(0, line.lastIndexOf(File.separator) + 1);
						wotScr = new File(wotFolder + "screenshots");
					}
					if (++n > 2) {
						break;
					}
				}
				input.close();
			} else if (!ensure_active) { // workaround only works if know where WoT is
				Process p = Runtime.getRuntime().exec(System.getenv("windir") + "\\system32\\" + "tasklist.exe");

				BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String line;
				while ((line = input.readLine()) != null && !running) {
					if (line.startsWith("WorldOfTanks.exe")) {
						// don't know if its possible to get the active window, so assume that is active & fullscreen
						running = true;
					}
				}
				input.close();
			}
		} catch (IOException ex) {
			Logger.getLogger(FindButton.class.getName()).log(Level.SEVERE, null, ex);
		}
		return running;
	}

	static boolean still_is_button(boolean workaround) throws IOException {
		if (!wot_active(workaround)) {
			return false;
		}
		if (workaround) {
			try {
				scr.screenImage = printScreenWorkaround(buttonBounds);
				if (scr.screenImage == null) {
					return false;
				}
			} catch (InterruptedException ex) {
				Logger.getLogger(FindButton.class.getName()).log(Level.SEVERE, null, ex);
				return false;
			}
		} else {
			scr.screenCap(buttonBounds);
		}
		if (target1 == null || target2 == null) {
			target1 = ImageIO.read(FindButton.class.getResource("resources/target.png"));
			target2 = ImageIO.read(FindButton.class.getResource("resources/target_2.png"));
		}
		return button_found = is_close(scr.screenImage);
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
//	// much too slow..
//	static long st = 0;
//	static Point extensiveSearch() {
//		if (st != 0) {
//			return null;
//		}
//		System.out.println("running larger scan ...");
//		st = System.currentTimeMillis();
//		BufferedImage img = scr.screenCap();
//		HashMap<Point, Double> finds = new HashMap<Point, Double>();
//
//		if (target1 == null || target2 == null) {
//			try {
//				target1 = ImageIO.read(FindButton.class.getResource("resources/target.png"));
//				target2 = ImageIO.read(FindButton.class.getResource("resources/target_2.png"));
//				// these must have the same dimensions..
//				if (target1.getWidth() != target2.getWidth() || target1.getWidth() != target2.getWidth()) {
//					Logger.getAnonymousLogger().log(Level.SEVERE, null, new Exception("compare images are not the same size"));
//					st = 0;
//					return null;
//				}
//			} catch (IOException ex) {
//				Logger.getAnonymousLogger().log(Level.SEVERE, null, ex);
//				st = 0;
//				return null;
//			}
//		}
//
//		for (int x = 0; x < img.getWidth() - target1.getWidth() - 10; x += 2) {
//			for (int y = 0; y < (img.getHeight() / 2) - target1.getHeight() - 10; y += 2) {
//				double per = imageMatch(img, x, y, target1, 10);
//				double per2 = imageMatch(img, x, y, target2, 10);
//				if (per > closeEpsilon || per2 > closeEpsilon) {
//					if (per2 > per) {
//						finds.put(new Point(x, y), per2);
//					} else {
//						finds.put(new Point(x, y), per);
//					}
//				}
//			}
//		}
//		System.out.println("extensive scan done: " + (System.currentTimeMillis() - st) + " ms, " + finds.size() + " results");
//		st = 0;
//
//		if (finds.size() > 0) {
//			Point[] points = finds.keySet().toArray(new Point[0]);
//			double max = finds.get(points[0]);
//			int maxi = 0;
//			for (int i = 1; i < points.length; ++i) {
//				double d = finds.get(points[i]);
//				if (d > max) {
//					max = d;
//					maxi = i;
//				}
//			}
//			return points[maxi];
//		}
//		return null;
//	}
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
//	public static void main(String args[]) throws IOException {
//
//		{ // trying to search for the red in the box
//			Rectangle searchBounds = new Rectangle(scr.getScreenRes().width / 3, 5, 0, 170);
//			searchBounds.width = searchBounds.x;
//			BufferedImage search = scr.screenCap(searchBounds);
//
//			// use save image instead of current, for testing
//			try {
//				search = javax.imageio.ImageIO.read(new java.io.File("D:\\Jacob\\Programs\\Java\\CountDownSync\\shot_001.jpg"));
//				searchBounds.x = search.getWidth() / 3;
//				searchBounds.width = searchBounds.x;
//				BufferedImage img = new BufferedImage(searchBounds.width, searchBounds.height, search.getType());
//				for (int x = searchBounds.x; x < searchBounds.x + searchBounds.width; ++x) {
//					for (int y = searchBounds.y; y < searchBounds.y + searchBounds.height; ++y) {
//						img.setRGB(x - searchBounds.x, y - searchBounds.y, search.getRGB(x, y));
//					}
//				}
//				search = img;
//			} catch (IOException ex) {
//				Logger.getLogger(CaptureScreen.class.getName()).log(Level.SEVERE, null, ex);
//			}
//			//if(false)
//			{ // find avg color of a region in the button
//				ColorAverage avg1 = new ColorAverage();
//				ColorAverage avg2 = new ColorAverage();
//				target1 = ImageIO.read(FindButton.class.getResource("resources/target.png"));
//				target2 = ImageIO.read(FindButton.class.getResource("resources/target_2.png"));
//				for (int x = 7; x < 16 + 7; ++x) {
//					for (int y = 5; y < 16 + 5; ++y) {
//						avg1.addColor(target1.getRGB(x, y));
//						avg2.addColor(target2.getRGB(x, y));
//					}
//				}
//				System.out.println("avg1: " + avg1.getRed() + ", " + avg1.getGreen() + ", " + avg1.getBlue());
//				System.out.println("min1: " + avg1.getMinR() + ", " + avg1.getMinG() + ", " + avg1.getMinB());
//				System.out.println("min1: " + avg1.getMaxR() + ", " + avg1.getMaxG() + ", " + avg1.getMaxB());
//				System.out.println("avg2: " + avg2.getRed() + ", " + avg2.getGreen() + ", " + avg2.getBlue());
//				System.out.println("min2: " + avg2.getMinR() + ", " + avg2.getMinG() + ", " + avg2.getMinB());
//				System.out.println("min2: " + avg2.getMaxR() + ", " + avg2.getMaxG() + ", " + avg2.getMaxB());
//				System.out.println("avg: "
//						+ ((avg1.getRed() + avg2.getRed()) / 2) + ", "
//						+ ((avg1.getGreen() + avg2.getGreen()) / 2) + ", "
//						+ ((avg1.getBlue() + avg2.getBlue()) / 2)
//						+ "  dev: "
//						+ Math.abs(((avg1.getRed() + avg2.getRed()) / 2) - avg1.getRed()) + ", "
//						+ Math.abs(((avg1.getGreen() + avg2.getGreen()) / 2) - avg1.getGreen()) + ", "
//						+ Math.abs(((avg1.getBlue() + avg2.getBlue()) / 2) - avg1.getBlue()));
//				System.out.println("========================================");
//			}
//			// now that have image, search for the button
//			// first search for first red color match
//			for (int y = 0; y < searchBounds.height - buttonBounds.height; ++y) {
//				for (int x = 0; x < searchBounds.width - buttonBounds.width; ++x) {
//					if (avg.is_close(ARGB.fromARGB(search.getRGB(x, y)), dR, dG, dB)) {
//						System.out.println("close at " + x + ", " + y);
//						// check a 16x16 square, see if all also close
//						ColorAverage testavg = new ColorAverage();
//						for (int x2 = x; x2 < 16 + x; ++x2) {
//							for (int y2 = y; y2 < 16 + y; ++y2) {
//								testavg.addColor(search.getRGB(x2, y2));
////								if (!avg.is_close(ARGB.fromARGB(search.getRGB(x2, y2)), dR, dG, dB)) {
////									all = false;
////								}
//							}
//						}
//
//						System.out.println("avg: " + testavg.getRed() + ", " + testavg.getGreen() + ", " + testavg.getBlue());
//						System.out.println("min: " + testavg.getMinR() + ", " + testavg.getMinG() + ", " + testavg.getMinB());
//						System.out.println("min: " + testavg.getMaxR() + ", " + testavg.getMaxG() + ", " + testavg.getMaxB());
//						System.out.println("========================================");
//						if (!avg.is_close(testavg.getArgb(), dR, dG, dB)) {
//							x += 16;
//							continue;
//						}
//						System.out.println("found box at " + x + ", " + y);
//
//						// box will be towards the right of the corner, so move left some
//						x -= 5;
//
//						double per = imageMatch(target1, x, y, search, colorDelta);
//						double per2 = imageMatch(target2, x, y, search, colorDelta);
//						System.out.println(per);
//						System.out.println(per2);
//						if (per >= closeEpsilon || per2 >= closeEpsilon) { //is_close(search, colorDelta)) {
//							System.out.println("button found!");
//						}
//
//						BufferedImage img =
//								new BufferedImage(searchBounds.width - x, searchBounds.height - y, search.getType());
//						for (int x2 = x; x2 < searchBounds.width; ++x2) {
//							for (int y2 = y; y2 < searchBounds.height; ++y2) {
//								img.setRGB(x2 - x, y2 - y, search.getRGB(x2, y2));
//							}
//						}
//						search = img;
//						y = searchBounds.height;
//						x = searchBounds.width;
//					}
//				}
//			}
//
//
//			javax.swing.JFrame myFrame = new javax.swing.JFrame("Capture Screen");
//			scr.screenImage = search;
//			myFrame.add(scr);
//			myFrame.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
//			myFrame.setSize(searchBounds.width, searchBounds.height);
//			myFrame.setVisible(true);
//		}
//	}
}
