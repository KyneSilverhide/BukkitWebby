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

/**
 * Store only the required informations about a Bukkit player
 */
public class WebbyPlayer implements Serializable {

	private static final long serialVersionUID = -5485726983658593255L;

	private final String name;
	private final boolean isOp;

	public WebbyPlayer(final String name, final boolean isOp) {
		this.name = name;
		this.isOp = isOp;
	}

	public String getName() {
		return name;
	}

	public boolean isOp() {
		return isOp;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (isOp ? 1231 : 1237);
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		final WebbyPlayer other = (WebbyPlayer) obj;
		if (isOp != other.isOp) {
			return false;
		}
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		return true;
	}
}
