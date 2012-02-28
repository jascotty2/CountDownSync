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

/**
 * based off of
 * http://pirate.shu.edu/~wachsmut/Teaching/CSAS2214/Virtual/Lectures/chat-client-server.html
 */
import java.net.*;
import java.io.*;
import java.util.Observable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChatServer extends Observable implements Runnable {

	public static int MAX_CONNECTIONS = 128;
	private ChatServerThread clients[] = new ChatServerThread[MAX_CONNECTIONS];
	private ServerSocket server = null;
	private Thread thread = null;
	private int clientCount = 0;

	public ChatServer(int port) {
		try {
			System.out.println("Binding to port " + port + ", please wait  ...");
			server = new ServerSocket(port);
			System.out.println("Server started: " + server);
			start();
		} catch (IOException ioe) {
			System.out.println("Can not bind to port " + port + ": " + ioe.getMessage());
			server = null;
		}
	}

	public boolean isRunning() {
		return server != null;
	}

	public void run() {
		while (thread != null) {
			try {
				if (Main.DEBUG) System.out.println("Waiting for a client ...");
				addThread(server.accept());
			} catch (IOException ioe) {
				System.out.println("Server accept error: " + ioe);
				stop();
			}
		}
	}

	public void start() {
		if (thread == null) {
			thread = new Thread(this);
			thread.start();
		}
	}

	public void stop() {
		if (thread != null) {
			thread.stop();
			thread = null;
		}
	}

	private int findClient(int ID) {  //for(int i=0; i<MAX_CONNECTIONS; ++i)
		for (int i = 0; i < clientCount; i++) {
			if (clients[i].getID() == ID) {
				return i;
			}
		}
		return -1;
	}

	public synchronized void handle(int ID, String input) {
		if (input.equals("bye")) {
			clients[findClient(ID)].send("bye");
			remove(ID);
		} else {
			setChanged();
			if (Main.DEBUG) System.out.println("Message Recieved from " + ID + ": " + input);
			notifyObservers(ID + "\t" + input);
		}
	}

	public synchronized void broadcastMessage(String msg) {
		if (Main.DEBUG) {
			System.out.println("Sending: " + msg);
		}
		for (int i = 0; i < clientCount; i++) {
			clients[i].send(msg);
		}
	}

	public synchronized void sendMessage(String msg, int uId) {
		int ID = findClient(uId);
		if (ID != -1) {//uId>=0 && uId<clientCount){
			if (Main.DEBUG) System.out.println("Sending to " + uId + ": " + msg);
			clients[ID].send(msg);
		}
	}

	public synchronized void remove(int ID) {
		int pos = findClient(ID);
		if (pos >= 0) {
			ChatServerThread toTerminate = clients[pos];
			if (Main.DEBUG) System.out.println("Removing client thread " + ID + " at " + pos);
			if (pos < clientCount - 1) {
				for (int i = pos + 1; i < clientCount; i++) {
					clients[i - 1] = clients[i];
				}
			}
			clientCount--;

			setChanged();
			notifyObservers(ID + "\tbye");
			try {
				toTerminate.close();
			} catch (IOException ioe) {  //System.out.println("Error closing thread: " + ioe);
				Logger.getLogger(Main.class.getName()).log(Level.SEVERE, "Error removing client thread from server", ioe);
			}
			toTerminate.stop();
		}
	}

	private void addThread(Socket socket) {
		if (clientCount < clients.length) {
			System.out.println("Client accepted: " + socket);
			clients[clientCount] = new ChatServerThread(this, socket);
			try {
				clients[clientCount].open();
				clients[clientCount].start();
				clientCount++;
			} catch (IOException ioe) {
				System.out.println("Error opening thread: " + ioe);
			}
		} else {
			System.out.println("Client refused: maximum " + clients.length + " reached.");
		}
	}
//	public static void main(String args[]) {
//		ChatServer server = null;
//		if (args.length != 1) //System.out.println("Usage: java ChatServer port");
//		//args[0]="9999";
//		{
//			args = new String[]{"9999"};
//		}
//		//else
//		server = new ChatServer(Integer.parseInt(args[0]));
//	}
}
