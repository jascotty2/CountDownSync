package me.jascotty2.countdownsync.server;

public class ClientData {

	public String nick, leader="";
	boolean isLeader = true;
	boolean ready = false;
	
	public ClientData() {
	}

	public ClientData(String nick) {
		this.nick = nick;
	}

	@Override
	public String toString() {
		return nick + "," + leader + "," + isLeader + "," + ready;
	}
	
}