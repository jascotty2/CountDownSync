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

import java.io.File;
import java.io.FileInputStream;
import sun.audio.AudioPlayer;
import sun.audio.AudioStream;

public class CountDownThread extends Thread {

	CountDownSyncClientView app;
	Thread countdown = null;
	int sec;

	public CountDownThread(CountDownSyncClientView callback) {
		app = callback;
	}

	@Override
	public void start() {
		start(5);
	}

	public void start(int secs) {
		if (countdown == null) {
			sec = secs;
			countdown = new Thread(this);
			countdown.start();
		}
	}

	@Override
	public void run() {
		int preload = app.client.getPing();
		if(preload > 998) preload = 998;
		try {
			for (int i = sec; i > 0; --i) {
				app.setStatus("starting in " + i + " seconds..");
				playThreadedTick();
				if (i == 1) {
					FindButton.move_mouse();
					Thread.sleep(998 - preload);
				} else {
					Thread.sleep(998);
				}
			}
			playThreadedClickSound();
			app.click();
		} catch (Exception e) {
		}
		countdown = null;
	}

	protected void playThreadedTick() {
		Thread tick = new Thread() {

			@Override
			public void run() {
				play(app.conf.getTickSound());
			}
		};
		tick.start();
	}

	protected void playThreadedClickSound() {
		Thread tick = new Thread() {

			@Override
			public void run() {
				play(app.conf.getDoneSound());
			}
		};
		tick.start();
	}

	private static void play(File f) {
		try {
			AudioStream clip = new AudioStream(
					new FileInputStream(f));
			AudioPlayer.player.start(clip);

		} catch (Exception e) {
			System.err.println(e.getMessage());
		}

	}
}
