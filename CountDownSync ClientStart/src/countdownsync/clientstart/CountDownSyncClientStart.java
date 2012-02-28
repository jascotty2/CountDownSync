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
package countdownsync.clientstart;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CountDownSyncClientStart {
	
	static final String VERSION = "1.1.0";

	public static void main(String[] args) throws IOException {
		String savefolder = System.getenv("APPDATA") + "\\CountDownSync\\";
		File libFolder = new File(savefolder + "lib");
		if(!libFolder.exists()) {
			libFolder.mkdirs();
		}
		File f = new File(savefolder + "lib\\appframework-1.0.3.jar");
		if(!f.exists()) {
			extractResource("appframework-1.0.3.jar", f);
		}
		f = new File(savefolder + "lib\\swing-worker-1.1.jar");
		if(!f.exists()) {
			extractResource("swing-worker-1.1.jar", f);
		}
		f = new File(savefolder + "CountDownSync Client.jar");
		if(!f.exists()) {
			extractResource("CountDownSync Client.jar", f);
		} else {
			try {
				File verFile = new File(savefolder + "VERSION");
				Scanner in = new Scanner(verFile);
				String v = in.nextLine().trim();
				if(versionHigher(v, VERSION)) {
					System.out.println("updating older version");
					extractResource("CountDownSync Client.jar", f, true);
				}
			} catch (FileNotFoundException ex) {
				extractResource("CountDownSync Client.jar", f, true);
			}
		}
		Runtime rt = Runtime.getRuntime();
		rt.exec("java -jar \"" + f.getAbsolutePath() + "\"");
	}
	
	
	/**
	 * compare two version strings, like "1.4.2" !> "1.4.3" | "1.4.2b"
	 * @param version first version string
	 * @param compare version string to compare to
	 * @return true if compare is > than first version parameter
	 */
	static boolean versionHigher(String version, String compare) {
		if (version == null || compare == null) {
			return false;
		}
		String v1[] = version.trim().toLowerCase().split("\\."),
				v2[] = compare.trim().toLowerCase().split("\\.");
		int n1, n2;
		for (int i = 0; i < v1.length && i < v2.length; ++i) {
			if (CheckInput.IsInt(v1[i]) && CheckInput.IsInt(v2[i])) {
				n1 = CheckInput.GetInt(v1[i], 0);
				n2 = CheckInput.GetInt(v2[i], 0);
				if (n1 != n2) {
					return n2 > n1;
				}
			} else {
				String a1 = "", a2 = "";
				if (CheckInput.IsInt(v1[i])) {
					n1 = CheckInput.GetInt(v1[i], 0);
				} else if (java.util.regex.Pattern.matches("(\\p{Digit}+)[a-z]", v1[i])) {
					n1 = CheckInput.GetInt(v1[i].substring(0, v1[i].length() - 1), 0);
					a1 = v1[i].substring(v1[i].length() - 1);
				} else {
					n1 = -1;
				}
				if (CheckInput.IsInt(v2[i])) {
					n2 = CheckInput.GetInt(v2[i], 0);
				} else if (java.util.regex.Pattern.matches("(\\p{Digit}+)[a-z]", v2[i])) {
					n2 = CheckInput.GetInt(v2[i].substring(0, v2[i].length() - 1), 0);
					a2 = v2[i].substring(v2[i].length() - 1);
				} else {
					n2 = -1;
				}

				if (n1 != n2) {
					return n2 > n1;
				} else if (a1.length() != a2.length()) {
					return a2.length() > a1.length();
				} else if (a1.length() > 0 && a2.charAt(0) != a1.charAt(0)) {
					return a2.charAt(0) > a1.charAt(0);
				}

			}
		}
		return v2.length > v1.length;
	}
	
	protected static void extractResource(String fname, File dest) {
		extractResource(fname, dest, false);
	}

	protected static void extractResource(String fname, File dest, boolean force) {
		File of = dest;
		if (dest.isDirectory()) {
			String fn = new File(fname).getName();
			of = new File(dest, fn);
		} else if (of.exists() && !of.isFile()) {
			return;
		}

		if (of.exists() && !force) {
			return;
		}

		OutputStream out = null;
		try {
			// Got to jump through hoops to ensure we can still pull messages from a JAR
			// file after it's been reloaded...

			URL res = CountDownSyncClientStart.class.getResource("/lib/" + fname);
			if (res == null) {
				Logger.getAnonymousLogger().log(Level.WARNING, "can't find " + fname + " in plugin JAR file"); //$NON-NLS-1$
				return;
			}
			URLConnection resConn = res.openConnection();
			resConn.setUseCaches(false);
			InputStream in = resConn.getInputStream();

			if (in == null) {
				Logger.getAnonymousLogger().log(Level.WARNING, "can't get input stream from " + res); //$NON-NLS-1$
			} else {
				out = new FileOutputStream(of);
				byte[] buf = new byte[1024];
				int len;
				while ((len = in.read(buf)) > 0) {
					out.write(buf, 0, len);
				}
				in.close();
				out.close();
			}
		} catch (Exception ex) {
			Logger.getAnonymousLogger().log(Level.SEVERE, null, ex);
		} finally {
			try {
				if (out != null) {
					out.close();
				}
			} catch (Exception ex) { //IOException
				// ChessCraft.log(Level.SEVERE, null, ex);
			}
		}
	}
}
