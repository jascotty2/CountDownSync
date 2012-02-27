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
		try {
			for (int i = sec; i > 0; --i) {
				app.setStatus("starting in " + i + " seconds..");
				playThreadedTick();
				if(i == 1) {
					FindButton.move_mouse();
				} 
				Thread.sleep(998);
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
