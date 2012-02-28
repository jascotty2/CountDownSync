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