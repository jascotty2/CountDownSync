package me.jascotty2.countdownsync.client;

public class ClientData {

	public String nick, leader;
	boolean isLeader = true; // by default, all clients are leaders
	boolean ready = false;

	public ClientData() {
	}

	public ClientData(String nick) {
		this.nick = nick;
	}

	public static ClientData fromString(String str) {
		String args[] = str.split(",");
		if (args.length == 4) {
			return (new ClientData()).updateFromStr(str);
		}
		return null;
	}
	
	public ClientData updateFromStr(String str) {
		String args[] = str.split(",");
		if (args.length == 4) {
			nick = args[0];
			leader = args[1];
			isLeader = Boolean.parseBoolean(args[2]);
			ready = Boolean.parseBoolean(args[3]);
		}
		return this;
	}

	@Override
	public String toString() {
		return "<html><span style='color:" + (isLeader ? "blue" : (ready ? "green" : "red")) + ";'>"
				+ (isLeader ? nick : padRight(nick, CountDownSyncClientView.MAXNAMELEN + 5, "&nbsp;")
				+ "</span>(" + leader + ")") + "</html>";
	}
	
	String padRight(String str, int len, String padStr) {
		if (str == null) {
			str = "";
		}
		int pad = len - str.length() ;
		for (int i = 0; i < pad; ++i) {
			str += padStr;
		}
		return str;
	}
}