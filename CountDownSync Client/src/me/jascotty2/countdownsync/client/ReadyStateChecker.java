package me.jascotty2.countdownsync.client;

import java.awt.Point;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReadyStateChecker implements Runnable {

	Thread thread = null;
	SyncClient client;
	boolean is_ready = false, found = false;

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
			while (true) {
				Thread.sleep(2000);
				if (!FindButton.still_is_button()) {
					if (!found) {
						Point pt = FindButton.findPoint();
						if (pt.x > 0) {
							found = true;
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
		} catch (InterruptedException ex) {
		}
		thread = null;
	}
}
