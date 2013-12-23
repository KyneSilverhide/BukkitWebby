/*
 * Copyright KyneSilverhide 2010-2011
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.kyne.webby.commons.protocol;

import java.io.Serializable;
import java.util.List;

/**
 * Class that contains all required informations from Bukkit in a single object (avoid multiple requests on a new socket each time)
 */
public class ServerInfos implements Serializable {

	private static final long serialVersionUID = -8966763177877518080L;

	private final String bukkitVersion;
	private final int maxPlayers;
	private final int onlinePlayerCount;
	private final List<WebbyPlayer> players;
	private final long freeMemory;
	private final long totalMemory;
	private final long maxMemory;

	public ServerInfos(final String bukkitVersion, final int maxPlayers, final int onlinePlayerCount,
			final List<WebbyPlayer> players, final long freeMemory, final long totalMemory, final long maxMemory) {
		this.bukkitVersion = bukkitVersion;
		this.maxPlayers = maxPlayers;
		this.onlinePlayerCount = onlinePlayerCount;
		this.players = players;
		this.maxMemory = maxMemory;
		this.totalMemory = totalMemory;
		this.freeMemory = freeMemory;
	}

	public int getMaxPlayers() {
		return maxPlayers;
	}

	public int getOnlinePlayerCount() {
		return onlinePlayerCount;
	}

	public String getBukkitVersion() {
		return bukkitVersion;
	}

	public List<WebbyPlayer> getPlayers() {
		return players;
	}

	public long getFreeMemory() {
		return freeMemory;
	}

	public long getTotalMemory() {
		return totalMemory;
	}

	public long getMaxMemory() {
		return maxMemory;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((bukkitVersion == null) ? 0 : bukkitVersion.hashCode());
		result = prime * result + maxPlayers;
		result = prime * result + onlinePlayerCount;
		result = prime * result + ((players == null) ? 0 : players.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ServerInfos other = (ServerInfos) obj;
		if (bukkitVersion == null) {
			if (other.bukkitVersion != null)
				return false;
		} else if (!bukkitVersion.equals(other.bukkitVersion))
			return false;
		if (maxPlayers != other.maxPlayers)
			return false;
		if (onlinePlayerCount != other.onlinePlayerCount)
			return false;
		if (players == null) {
			if (other.players != null)
				return false;
		} else if (!players.equals(other.players))
			return false;
		return true;
	}
}
