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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

public class ReadyStateChecker implements Runnable {

	Thread thread = null;
	SyncClient client;
	protected boolean is_ready = false;

	public ReadyStateChecker(SyncClient client) {
		this.client = client;
	}

	public void start() {
		if (thread == null) {
			thread = new Thread(this);
			thread.start();
		}
	}

	public void stop() {
		if (thread != null) {
			try {
				thread.interrupt();
				thread.join();
			} catch (InterruptedException ex) {
				Logger.getLogger(CountDownSyncClientApp.class.getName()).log(Level.SEVERE, null, ex);
			}
			thread = null;
		}
	}
	
	@Override
	public void run() {
		try {
//			int count = 0;
			while (true) {
				boolean workaround = client.app.conf.getSettings().getBool("useWorkaround");
				Thread.sleep(workaround ? 10000 : 2000);
				if (!client.app.in_countdown) {
					if (!FindButton.still_is_button(workaround)) {
						if (!FindButton.positive_found) {
							Point pt = FindButton.findPoint(workaround);
							if (pt.x > 0) {
								if (!is_ready) {
									client.sendReady(is_ready = true);
								}
							} else if (is_ready) {
								client.sendReady(is_ready = false);
							}
						} else if (is_ready) {
							client.sendReady(is_ready = false);
						}
					} else if (!is_ready) {
						client.sendReady(is_ready = true);
					}
				}
			}
		} catch (InterruptedException ex) {
		} catch (Throwable t) {
			Logger.getLogger(CountDownSyncClientApp.class.getName()).log(Level.SEVERE, 
					"Unexpected Error in Button Scan: " + t.getMessage(), t);
			JOptionPane.showMessageDialog(client.app.getFrame(), "Unexpected Error in Button Scan: " + t.getMessage(),
					"Program Error", JOptionPane.ERROR_MESSAGE);
		}
		thread = null;
	}
}
