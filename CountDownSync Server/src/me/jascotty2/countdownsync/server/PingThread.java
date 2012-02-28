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
package me.jascotty2.countdownsync.server;

import java.util.logging.Level;
import java.util.logging.Logger;

public class PingThread implements Runnable {

	Responder resp;
	long lastBroadcast;
	Thread thread;

	public PingThread(Responder callback) {
		resp = callback;
	}
	
	public int getPing() {
		return (int) (System.currentTimeMillis() - lastBroadcast);
	}
	
	public void start() {
		if(thread == null) {
			thread = new Thread(this);
			thread.start();
		}
	}

	public void stop() {
		if(thread != null) {
			try {
				thread.interrupt();
				thread.join();
				thread = null;
			} catch (InterruptedException ex) {
				Logger.getAnonymousLogger().log(Level.SEVERE, null, ex);
			}
		}
	}
	
	@Override
	public void run() {
		try {
			while(true) {
				Thread.sleep(2000);
				lastBroadcast = System.currentTimeMillis();
				resp.server.broadcastMessage("ping");
			}
		} catch (Exception e) {
		}
	}
}
