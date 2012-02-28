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
