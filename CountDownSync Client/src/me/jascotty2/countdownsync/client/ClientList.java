package me.jascotty2.countdownsync.client;

import java.util.HashMap;
import java.util.Map;

public class ClientList {

	private final Map<String, ClientData> list = new HashMap<String, ClientData>();

	public ClientData[] getList() {
		return list.values().toArray(new ClientData[0]);
	}

	public void addClient(String nick) {
		list.put(nick, new ClientData(nick));
	}

	public void addClient(ClientData c) {
		if (c != null) {
			list.put(c.nick, c);
		}
	}

	public void removeClient(String nick) {
		list.remove(nick);
	}
	
	public ClientData getClient(String nick) {
		return list.get(nick);
	}

	public ClientData getClient(int index) {
		if (index < 0 || index >= list.size()) {
			throw new IndexOutOfBoundsException("Index out of bounds: " + index);
		}
		return list.values().toArray(new ClientData[0])[index];
	}

	public void clear() {
		list.clear();
	}
	
	public int size() {
		return list.size();
	}
}
