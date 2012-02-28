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

/**
 * based off of
 * http://pirate.shu.edu/~wachsmut/Teaching/CSAS2214/Virtual/Lectures/chat-client-server.html
 */
import java.net.*;
import java.io.*;
import java.util.Observable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChatClient extends Observable implements Runnable {

	static final boolean DEBUG = false;
	
	private Socket socket = null;
	private Thread sendThread = null;
	private DataOutputStream streamOut = null;
	private ChatClientThread clientThread = null;
	protected String msgToSend = "";
	protected boolean isClosing = false;
	// for reconnecting
	private String server;
	private int port;

	public ChatClient(String serverName, int serverPort) throws UnknownHostException, IOException {
		System.out.println("Establishing connection. Please wait ...");
//		try {
		socket = new Socket(serverName, serverPort);
		server = serverName;
		port = serverPort;
		System.out.println("Connected: " + socket);
		start();
//		} catch (UnknownHostException uhe) {
//			System.out.println("Host unknown: " + uhe.getMessage());
//			//setChanged();
//			//notifyObservers(".connectError");
//			socket = null;
//		} catch (IOException ioe) {
//			System.out.println("Unexpected exception: " + ioe.getMessage());
//		}
	}

	public boolean isConnected() {
		return socket != null;
	}

	protected Socket getSocket() {
		return socket;
	}

	public void run() {  //*
		while (sendThread != null && !isClosing) {
			if (msgToSend.length() == 0) {
				try {
					Thread.sleep(10000);
				} catch (InterruptedException iee) {
					// interrupts are expected here
					//Logger.getLogger(Main.class.getName()).log(Level.SEVERE, "Thread sleep inturrupted", iee);
				}
			}
			if (!isClosing && msgToSend.length() > 0) {
				try {
					if (msgToSend.indexOf("\0") != -1) { // \n~~~~<>~~~~\n")!=-1){
						for (String msg : msgToSend.split("\0")) {//"\n~~~~<>~~~~\n")){
							streamOut.writeUTF(msg);//console.readLine());
							streamOut.flush();
							System.out.println("Sending: " + msg);
						}
					} else {
						streamOut.writeUTF(msgToSend);//console.readLine());
						streamOut.flush();
					}
					msgToSend = "";
				} catch (IOException ioe) {
					System.out.println("Sending error: " + ioe.getMessage());
					stop();
				}
			}
		}
		//*/
	}

	public void sendMessage(String msg) {
		if (msgToSend.length() != 0) {
			//msgToSend+="\n~~~~<>~~~~\n" + msg;
			msgToSend += "\0" + msg;
		} else {
			msgToSend = msg;
		}

		if (sendThread != null) {
			this.sendThread.interrupt();
		}
	}

	public void handle(String msg) {
		setChanged();
		if (msg.equals("disconnect")) {
			notifyObservers("reconnect");
			try {
				isClosing = true;
				sendThread.interrupt();
				sendThread = null;
				try {
					if (streamOut != null) {
						streamOut.close();
					}
					if (socket != null) {
						socket.close();
						socket = null;
					}
				} catch (IOException ioe) {  //System.out.println("Error closing ..."); 
					Logger.getLogger(CountDownSyncClientApp.class.getName()).log(Level.SEVERE, "Error closing part of client", ioe);
				}

				System.out.println("disconnected.. retrying connection");
				for (int i = 3; i > 0; --i) {
					Thread.sleep(1500);
					try {
						socket = new Socket(server, port);
						System.out.println("Connected: " + socket);
						start(true);
						setChanged();
						notifyObservers("connect");
						return;
					} catch (Exception e) {
						System.out.println("connection failed.. retrying " + i + " more times");
					}
				}
				System.out.println("reconnect failed");
				setChanged();
				notifyObservers("disconnect");
				stop();
			} catch (Exception e) {
			}
			return;
		}
		if (DEBUG) System.out.println("Message Recieved: " + msg);
		notifyObservers(msg);
	}

	public final void start() throws IOException {
		start(false);
	}

	public final void start(boolean restarting) throws IOException {
		if (sendThread == null) {
			streamOut = new DataOutputStream(socket.getOutputStream());
			if (!restarting) {
				clientThread = new ChatClientThread(this, socket);
			}
			isClosing = false;
			sendThread = new Thread(this);
			sendThread.start();
		}
	}

	public void stop() //throws InterruptedException
	{
		try {
			if (streamOut != null) {
				streamOut.close();
			}
			if (socket != null) {
				socket.close();
			}
		} catch (IOException ioe) {  //System.out.println("Error closing ..."); 
			Logger.getLogger(CountDownSyncClientApp.class.getName()).log(Level.SEVERE, "Error closing part of client", ioe);
		}
		if (clientThread != null) {
			clientThread.close();
			clientThread.stop();
		}
		if (sendThread != null) {  //thread.stop();
			try {
				//thread.stop();
				isClosing = true;
				sendThread.interrupt();
				sendThread.join();
				sendThread = null;
			} catch (InterruptedException ex) {
				Logger.getLogger(ChatClient.class.getName()).log(Level.SEVERE, "Error stopping client thread", ex);
			}
		}
	}
	/*public static void main(String args[])
	{  ChatClient client = null;
	if (args.length != 2)
	System.out.println("Usage: java ChatClient host port");
	else
	client = new ChatClient(args[0], Integer.parseInt(args[1]));
	}*/
}
