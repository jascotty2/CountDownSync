package me.jascotty2.countdownsync.server;

import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Responder implements Observer {

	protected final ChatServer server;
	protected final Map<Integer, ClientData> list = new HashMap<Integer, ClientData>();

	public Responder(ChatServer serv) {
		server = serv;
		server.addObserver(this);
	}

	public void run() {
		server.run();
	}

	@Override
	public void update(Observable o, Object arg) {
		try {
			if (!(arg instanceof String)) {
				Logger.getAnonymousLogger().log(Level.WARNING, "Unexpected Non-String Recieved..", new Exception());
				return;
			}
			String msg = (String) arg;
			int id = -1;
			if (!msg.contains("\t")) {
				Logger.getAnonymousLogger().log(Level.WARNING, "Client ID ommited from update..", new Exception());
				return;
			}
			try {
				id = Integer.parseInt(msg.substring(0, msg.indexOf("\t")));
				msg = msg.substring(msg.indexOf("\t") + 1);
			} catch (Exception e) {
				Logger.getAnonymousLogger().log(Level.WARNING, "Error parsing Client ID from: " + msg, new Exception());
				return;
			}

			String action = msg.contains(" ") ? msg.substring(0, msg.indexOf(" ")) : msg;
			msg = msg.contains(" ") ? msg.substring(msg.indexOf(" ") + 1) : "";

			if (action.equals("bye")) {
				if (list.containsKey(id)) {
					server.broadcastMessage("del " + list.get(id).nick);
				}
				list.remove(id);
			} else if (action.equals("nick")) {
				if (list.keySet().contains(id)) {
					return; // ignore nick changes (for now)
				}
				boolean used = false;
				for (ClientData c : list.values()) {
					if (c.nick.equalsIgnoreCase(msg)) {
						used = true;
						break;
					}
				}
				server.sendMessage(used ? "nonick" : "hello", id);
				if (!used) {
					ClientData c = new ClientData(msg);
					list.put(id, c);
					addNew(id, c);
					sendList(id);
				}
			} else if (action.equals("leader")) {
				ClientData c = list.get(id);
				if (c == null) {
					Logger.getAnonymousLogger().log(Level.WARNING, "Error: leader update from unregistered client", new Exception());
					return;
				}
				if (msg.isEmpty()) {
					c.isLeader = true;
					c.leader = "";
				} else {
					c.isLeader = false;
					c.leader = msg;
				}
				sendUpdate(c);
			} else if (action.equals("start")) {
				sendStart(id, msg.isEmpty() ? 5 : Integer.parseInt(msg));
			} else if (action.equals("ready")) {
				ClientData c = list.get(id);
				if (c == null) {
					Logger.getAnonymousLogger().log(Level.WARNING, "Error: ready update from unregistered client", new Exception());
					return;
				}
				boolean b = Boolean.parseBoolean(msg);
				if(c.ready != b) {
					c.ready = b;
					sendUpdate(c);
				}
			} else if (action.equals("refresh")) {
				sendList(id);
			}
		} catch (Throwable t) {
			Logger.getAnonymousLogger().log(Level.SEVERE, "Unexpected Error: " + t.getMessage(), t);
		}
	}

	public void addNew(int idSkip, ClientData info) {
		for (int id : list.keySet()) {
			if (id != idSkip) {
				server.sendMessage("new " + info.toString(), id);
			}
		}
	}

	public void sendList(int toID) {
		server.sendMessage("refresh", toID);
		for (int id : list.keySet()) {
			server.sendMessage("new " + list.get(id).toString(), toID);
		}
	}

	public void sendUpdate(ClientData info) {
		server.broadcastMessage("update " + info.toString());
	}

	public void sendStart(int leaderID, int seconds) {
		synchronized (list) {
			String n = list.get(leaderID).nick;
			for (Map.Entry<Integer, ClientData> e : list.entrySet()) {
				if (e.getKey() == leaderID // of course send to leader ;)
						|| (!e.getValue().isLeader && e.getValue().leader.equalsIgnoreCase(n))) {
					server.sendMessage("start", e.getKey());
				}
			}
		}
	}
}
