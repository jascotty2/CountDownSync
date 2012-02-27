package me.jascotty2.countdownsync.client;

import com.sparkedia.valrix.netstats.Property;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Settings {

	Property settingsFile;
	String savefolder;
	File tickSound, clickSound;

	public Settings() {
		savefolder = System.getenv("APPDATA") + "\\CountDownSync\\";
		settingsFile = new Property(savefolder + "settings.ini");
		tickSound = new File(savefolder + "sound99.wav");
		if (!tickSound.exists()) {
			extractResource("sound99.wav", tickSound);
		}
		clickSound = new File(savefolder + "sound82_2.wav");
		if (!clickSound.exists()) {
			extractResource("sound82_2.wav", clickSound);
		}
		tickSound = new File(savefolder + "sound82.wav");
		if (!tickSound.exists()) {
			extractResource("sound82.wav", tickSound);
		}
		// default sound
		File sound = settingsFile.keyExists("tickSound")
				? new File(settingsFile.getString("tickSound")) : null;
		if (!settingsFile.keyExists("tickSound") || (sound != null && !sound.exists())) {
			settingsFile.set("tickSound", tickSound.getAbsolutePath());
			if (!settingsFile.keyExists("usetick")) {
				settingsFile.set("usetick", true);
			}
			settingsFile.save();
		} else {
			tickSound = sound;
		}
		File sound2 = settingsFile.keyExists("doneSound")
				? new File(settingsFile.getString("doneSound")) : null;
		if (!settingsFile.keyExists("doneSound") || (sound2 != null && !sound2.exists())) {
			settingsFile.set("doneSound", clickSound.getAbsolutePath());
			settingsFile.save();
		} else {
			clickSound = sound2;
		}
	}

	public Property getSettings() {
		return settingsFile;
	}
	
	public File getTickSound() {
		return tickSound;
	}
	
	public File getDoneSound() {
		return clickSound;
	}
	
	public void save() {
		settingsFile.save();
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

			URL res = Settings.class.getResource("resources/" + fname);
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
