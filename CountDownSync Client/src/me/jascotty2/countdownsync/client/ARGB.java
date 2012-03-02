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

public class ARGB {

	private int argb, a, r, g, b; // can't do unsigned byte in java :(

	public ARGB(int r, int g, int b, int a) {
		testColorValueRange(r, g, b, a);
		this.a = a;
		this.r = r;
		this.g = g;
		this.b = b;
		argb = toARGB();
	}

	public ARGB(int r, int g, int b) {
		testColorValueRange(r, g, b, 255);
		this.r = r;
		this.g = g;
		this.b = b;
		this.a = 255;
		argb = toARGB();
	}

	public ARGB(int argb) {
		a = ((argb >> 24) & 0xFF);
		b = ((argb) & 0xFF);
		g = ((argb >> 8) & 0xFF);
		r = ((argb >> 16) & 0xFF);
	}

	public static ARGB fromARGB(int argb) {
		return new ARGB(argb);
	}

	public int getAlpha() {
		return a;
	}

	public int getArgb() {
		return argb;
	}

	public int getBlue() {
		return b;
	}

	public int getGreen() {
		return g;
	}

	public int getRed() {
		return r;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		if (this.argb != ((ARGB) obj).argb) {
			return false;
		}
		return true;
	}
	
	public boolean equals(int argb) {
		return this.argb == argb;
	}
	
	public boolean is_close(int argb, int delta) {
		return is_close(fromARGB(argb), delta);
	}
	
	public boolean is_close(ARGB other, int delta) {
		return Math.abs(other.a - a) <= delta
				&& Math.abs(other.r - r) <= delta
				&& Math.abs(other.g - g) <= delta
				&& Math.abs(other.b - b) <= delta;
	}
	
	public boolean is_close(int argb, int dr, int dg, int db) {
		return is_close(fromARGB(argb), dr, dg, db);
	}
	
	public boolean is_close(ARGB other, int dr, int dg, int db) {
		return Math.abs(other.r - r) <= dr
				&& Math.abs(other.g - g) <= dg
				&& Math.abs(other.b - b) <= db;
	}
	
	public boolean is_close(int argb, int dr, int dg, int db, int da) {
		return is_close(fromARGB(argb), dr, dg, db, da);
	}
	
	public boolean is_close(ARGB other, int dr, int dg, int db, int da) {
		return Math.abs(other.a - a) <= da
				&& Math.abs(other.r - r) <= dr
				&& Math.abs(other.g - g) <= dg
				&& Math.abs(other.b - b) <= db;
	}

	@Override
	public int hashCode() {
		int hash = 3;
		hash = 41 * hash + this.argb;
		return hash;
	}

	private int toARGB() {
		//0xAARRGGBB
		return ((a & 0xFF) << 24)
				| ((r & 0xFF) << 16)
				| ((g & 0xFF) << 8)
				| (b & 0xFF);
	}

	// From java.awt.Color.java, 10 Feb 1997
	/**
	 * Checks the color integer components supplied for validity.
	 * Throws an {@link IllegalArgumentException} if the value is out of
	 * range.
	 * @param r the Red component
	 * @param g the Green component
	 * @param b the Blue component
	 **/
	private static void testColorValueRange(int r, int g, int b, int a) {
		boolean rangeError = false;
		String badComponentString = "";

		if (a < 0 || a > 255) {
			rangeError = true;
			badComponentString = badComponentString + " Alpha";
		}
		if (r < 0 || r > 255) {
			rangeError = true;
			badComponentString = badComponentString + " Red";
		}
		if (g < 0 || g > 255) {
			rangeError = true;
			badComponentString = badComponentString + " Green";
		}
		if (b < 0 || b > 255) {
			rangeError = true;
			badComponentString = badComponentString + " Blue";
		}
		if (rangeError == true) {
			throw new IllegalArgumentException("Color parameter outside of expected range:"
					+ badComponentString);
		}
	}
	
}
