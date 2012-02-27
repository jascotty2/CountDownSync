package com.sparkedia.valrix.netstats;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Property {

	private Map<String, String> props = new LinkedHashMap<String, String>();
	private Map<String, String> keys = new HashMap<String, String>();
	private String file;

	public Property(final String file) {
		if (file == null) {
			return;
		}
		this.file = file;
		if (new File(file).exists()) {
			load();
		}
	}

	// Load data from file into ordered HashMap
	public Property load() {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
			String line;
			byte cc = 0; // # of comments
			int delim;

			// While there are lines to read (auto-breaks)
			while ((line = br.readLine()) != null) {
				// Is a comment, store it
				if (line.charAt(0) == '#') {
					props.put("#" + cc, line.substring(line.indexOf(' ') + 1).trim());
					cc++;
					continue;
				}
				// Isn't a comment, store the key and value
				while ((delim = line.indexOf('=')) != -1) {
					final String key = line.substring(0, delim).trim();
					props.put(key.toLowerCase(), line.substring(delim + 1).trim());
					keys.put(key.toLowerCase(), key);
					break;
				}
			}
		} catch (final FileNotFoundException e) {
			Logger.getAnonymousLogger().log(Level.SEVERE, "Couldn't find file " + file, e);
		} catch (final IOException e) {
			Logger.getAnonymousLogger().log(Level.SEVERE, "Unable to save " + file, e);
		} finally {
			// Close the reader
			try {
				if (null != br) {
					br.close();
				}
			} catch (final IOException e) {
				Logger.getAnonymousLogger().log(Level.SEVERE, "Unable to save " + file, e);
			}
		}
		return this;
	}

	// Save data from LinkedHashMap to file
	public Property save() {
		BufferedWriter bw = null;
		try {
			{ // addition by jascotty2 - ensure that the file and directory structure exists
				File f = new File(file);
				if(!f.exists()) {
					File dir = new File(file.substring(0, file.lastIndexOf(File.separator)));
					if(!dir.exists()) {
						dir.mkdirs();
					}
					f.createNewFile();
				}
			}
			// Construct the BufferedWriter object
			bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));

			// Save all the props one at a time, only if there's data to write
			if (props.size() > 0) {
				String key;
				String val;
				// Iterate through all keys
				for (final String k : props.keySet()) {
					key = keys.get(k); // Case-preserved key
					val = props.get(k);

					// If it starts with "#", it's a comment so write it as such
					if (key.charAt(0) == '#') {
						// Writing a comment to the file
						bw.write("# " + val);
						bw.newLine();
						continue;
					}
					// Otherwise write the key and value pair as key=value
					bw.write(key + '=' + val);
					bw.newLine();
				}
			}
		} catch (final FileNotFoundException e) {
			Logger.getAnonymousLogger().log(Level.SEVERE, "Couldn't find file " + file, e);
		} catch (final IOException e) {
			Logger.getAnonymousLogger().log(Level.SEVERE, "Unable to save " + file, e);
		} finally {
			// Close the BufferedWriter
			try {
				if (null != bw) {
					bw.close();
				}
			} catch (final IOException e) {
				Logger.getAnonymousLogger().log(Level.SEVERE, "Unable to close buffer for " + file, e);
			}
		}
		return this;
	}

	// Return all keys
	public Set<String> getKeys() {
		return props.keySet();
	}

	// Turn the Property into a JSON string
	public String toJSON() {
		String json = "{";
		short i = 0;
		for (String key : props.keySet()) {
			json += key + ':' + '"' + props.get(key) + '"';
			if (i < props.keySet().size()) {
				json += ",";
			}
			i++;
		}
		return json + '}';
	}

	// Return current map
	public List<Map<String, String>> copy() {
		List<Map<String, String>> o = new ArrayList<Map<String, String>>();
		o.add(props);
		o.add(keys);
		return o;
	}

	// Convert current map to specified map
	public Property paste(final List<Map<String, String>> list) {
		props.putAll(list.get(0));
		keys.putAll(list.get(1));
		return this;
	}

	// Extend ability to clear all mappings
	public Property clear() {
		props.clear();
		keys.clear();
		return this;
	}

	// Check if the key exists or not
	public boolean keyExists(final String key) {
		return props.containsKey(key.toLowerCase());
	}

	// Check if the key no value
	public boolean isEmpty(final String key) {
		return ((String) props.get(key.toLowerCase())).isEmpty();
	}

	// Increment the key by 1
	public Property inc(final String key) {
		props.put(key.toLowerCase(), new BigDecimal((String) props.get(key.toLowerCase())).add(BigDecimal.ONE).toString());
		return this;
	}

	// Add given number to given key
	public Property add(final String key, final Number n) {
		props.put(key.toLowerCase(), new BigDecimal((String) props.get(key.toLowerCase())).add(new BigDecimal("" + n)).toString());
		return this;
	}

	// Subtract given number from given key
	public Number sub(final String key, final Number n) {
		final Number num = new BigDecimal((String) props.get(key.toLowerCase())).subtract(new BigDecimal("" + n));
		if (num.intValue() >= 0) {
			props.put(key, num.toString());
			return 1;
		}
		return 0;
	}

	// Remove key from map
	public Property remove(final String key) {
		props.remove(key.toLowerCase());
		keys.remove(key.toLowerCase());
		return this;
	}

	// Set property value
	public Property set(final String key, final Object value) {
		props.put(key.toLowerCase(), String.valueOf(value));
		keys.put(key.toLowerCase(), key);
		return this;
	}

	// Get property value as a String
	public String getString(final String key) {
		return props.containsKey(key.toLowerCase()) ? (String) props.get(key.toLowerCase()) : "";
	}

	// Get property value as a byte
	public byte getByte(final String key) {
		return props.containsKey(key.toLowerCase()) ? Byte.parseByte((String) props.get(key.toLowerCase())) : 0;
	}

	// Get property value as a short
	public short getShort(final String key) {
		return props.containsKey(key) ? Short.parseShort((String) props.get(key.toLowerCase())) : 0;
	}

	// Get property value as an int
	public int getInt(final String key) {
		return props.containsKey(key.toLowerCase()) ? Integer.parseInt((String) props.get(key.toLowerCase())) : 0;
	}

	// Get property value as a double
	public double getDouble(final String key) {
		return props.containsKey(key.toLowerCase()) ? Double.parseDouble((String) props.get(key.toLowerCase())) : 0.0D;
	}

	// Get property value as a long
	public long getLong(final String key) {
		return props.containsKey(key.toLowerCase()) ? Long.parseLong((String) props.get(key.toLowerCase())) : 0L;
	}

	// Get property value as a float
	public float getFloat(final String key) {
		return props.containsKey(key.toLowerCase()) ? Float.parseFloat((String) props.get(key.toLowerCase())) : 0F;
	}

	// Get property value as a BigDecimal
	public Number getNumber(final String key) {
		return props.containsKey(key.toLowerCase()) ? new BigDecimal((String) props.get(key.toLowerCase())) : 0;
	}

	// Get property value as a boolean
	public boolean getBool(final String key) {
		return props.containsKey(key.toLowerCase()) ? Boolean.parseBoolean((String) props.get(key.toLowerCase())) : false;
	}
}