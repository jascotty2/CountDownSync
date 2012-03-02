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

public class ColorAverage {

	long a, r, b, g, n;
	int minA, minR, minB, minG,
			maxA, maxR, maxB, maxG;

	public int getAlpha() { return (int) (a / n); }
	public int getRed() { return (int) (r / n); }
	public int getGreen() { return (int) (g / n); }
	public int getBlue() { return (int) (b / n); }

	public int getMaxA() { return maxA; }
	public int getMaxR() { return maxR; }
	public int getMaxG() { return maxG; }
	public int getMaxB() { return maxB; }

	public int getMinA() { return minA; }
	public int getMinR() { return minR; }
	public int getMinG() { return minG; }
	public int getMinB() { return minB; }

	public int getArgb() {
		return (new ARGB(getRed(), getGreen(), getBlue(), getAlpha())).getArgb();
	}
		
	public void addColor(int argb) {
		ARGB other = ARGB.fromARGB(argb);
		addColor(other.getRed(), other.getBlue(), other.getGreen(), other.getAlpha());
	}
	
	public void addColor(int red, int blue, int green, int alpha) {
		a += alpha;
		r += red;
		b += blue;
		g += green;
		if(alpha < minA || n == 0) minA = alpha;
		if(alpha > maxA) maxA = alpha;
		if(red < minR || n == 0) minR = red;
		if(red > maxR) maxR = red;
		if(blue < minB || n == 0) minB = blue;
		if(blue > maxB) maxB = blue;
		if(green < minG || n == 0) minG = green;
		if(green > maxG) maxG = green;
		++n;
	}
}
