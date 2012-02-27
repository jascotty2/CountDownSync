/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package me.jascotty2.countdownsync.server;

/**
 *
 * @author Jacob
 */
public class Main {

	static int port = 3210;

	public static void main(String args[]) {
		Responder serv = new Responder(new ChatServer(port));
		serv.run();
	}
}
