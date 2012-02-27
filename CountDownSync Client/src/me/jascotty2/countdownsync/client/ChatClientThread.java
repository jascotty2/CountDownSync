package me.jascotty2.countdownsync.client;

/**
 * http://pirate.shu.edu/~wachsmut/Teaching/CSAS2214/Virtual/Lectures/chat-client-server.html
 */
import java.net.*;
import java.io.*;


public class ChatClientThread extends Thread {

	private Socket socket = null;
	private ChatClient client = null;
	private DataInputStream streamIn = null;

	public ChatClientThread(ChatClient _client, Socket _socket) {
		client = _client;
		socket = _socket;
		open();
		start();
	}

	public final void open() {
		try {
			streamIn = new DataInputStream(socket.getInputStream());
		} catch (IOException ioe) {
			System.out.println("Error getting input stream: " + ioe);
			client.stop();
		}
	}

	public void close() {
		try {
			if (streamIn != null) {
				streamIn.close();
			}
		} catch (IOException ioe) {
			System.out.println("Error closing input stream: " + ioe);
		}
	}

	@Override
	public void run() {
		while (true) {
			try {
				client.handle(streamIn.readUTF());
			} catch (IOException ioe) {
				try {
					streamIn.close();
				} catch (IOException ex) {
				}
				System.out.println("Listening error: " + ioe.getMessage());
				client.handle("disconnect");
				if(client.isConnected()) {
					socket = client.getSocket();
					open();
				} else {
					client.stop();
					return;
				}
			}
		}
	}
}