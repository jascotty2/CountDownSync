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

import java.io.IOException;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SyncClient implements Observer {

	protected final ChatClient client;
	CountDownSyncClientView app;
	boolean is_ready = false;
	private int ping;
	String leader = "";

	public SyncClient(ChatClient client, CountDownSyncClientView callback) throws IOException {
		this.client = client;
		app = callback;
		client.addObserver(this);
		client.start();
	}

	public void sendMessage(String msg) {
		client.sendMessage(msg);
	}

	public void close() {
		client.stop();
	}

	public void update(Observable o, Object arg) {
		if (!(arg instanceof String)) {
			Logger.getAnonymousLogger().log(Level.WARNING, "Unexpected Non-String Recieved..", new Exception());
			return;
		}
		try {
			String msg = (String) arg;
			String action = msg.contains(" ") ? msg.substring(0, msg.indexOf(" ")) : msg;
			msg = msg.contains(" ") ? msg.substring(msg.indexOf(" ") + 1) : "";

			if (action.equals("start")) {
				app.startCount(msg.isEmpty() ? 5 : Integer.parseInt(msg));
			} else if (action.equals("ping")) {
				if(msg.isEmpty()) {
					client.sendMessage("pong");
				} else {
					ping = Integer.parseInt(msg);
					app.setStatus("Connected. (" + ping + "ms)");
				}
			} else if (action.equals("reconnect")) {
				app.disconnect(true);
			} else if (action.equals("disconnect")) {
				app.disconnect(false);
			} else if (action.equals("connect")) {
				app.reconnected();
				// assume have to reload all info server-side
				setNickname(app.nickname);
			} else if (action.equals("nonick")) {
				app.nick(false);
			} else if (action.equals("hello")) {
				app.nick(true);
				if(is_ready) {
					sendReady(true);
				}
				if(!leader.isEmpty()) {
					setLeader(leader);
				}
			} else if (action.equals("refresh")) {
				app.clients.clear();
			} else if (action.equals("new")) {
				app.clients.addClient(ClientData.fromString(msg));
				app.updateList();
			} else if (action.equals("update")) {
				String nick = msg.substring(0, msg.indexOf(","));
				app.clients.getClient(nick).updateFromStr(msg);
				app.updateList();
			} else if (action.equals("del")) {
				app.clients.removeClient(msg);
				app.updateList();
			} else if(action.equals("search")) {
				if(!app.checker.found) {
					app.checker.extensiveScan();
				}
			} else if(action.equals("nosearch")) {
				app.noRequestClientUpdate();
			}
		} catch (Exception e) {
			Logger.getAnonymousLogger().log(Level.SEVERE, "Unexpected Exception: " + e.getMessage(), e);
		}
	}
	
	public int getPing() {
		return ping;
	}

	public void setNickname(String nick) {
		client.sendMessage("nick " + nick);
	}

	public void setLeader(String nick) {
		leader = nick;
		client.sendMessage("leader " + (nick == null ? "" : nick));
	}

	public void sendStart() {
		client.sendMessage("start");
	}
	
	public void sendForceUpdate(String nick) {
		client.sendMessage("search " + nick);
	}
	
	public void sendReady(boolean ready) {
		is_ready = ready;
		client.sendMessage("ready " + ready);
	}
	
	public void requestUpdate() {
		client.sendMessage("refresh");
	}
}
