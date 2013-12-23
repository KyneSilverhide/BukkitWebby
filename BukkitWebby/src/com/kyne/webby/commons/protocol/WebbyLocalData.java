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
import java.util.Map;

public class WebbyLocalData implements Serializable {

	private static final long serialVersionUID = 4496100786768111591L;
	private final RequestType requestType; 
	private final Map<String, Object> requestParams;
	
	public WebbyLocalData(RequestType requestType,
			Map<String, Object> requestParams) {
		this.requestType = requestType;
		this.requestParams = requestParams;
	}
	
	public RequestType getRequestType() {
		return requestType;
	}

	public Map<String, Object> getRequestParams() {
		return requestParams;
	}
	
	public static enum RequestType {
		PING, GET_INFOS, SEND_COMMAND, RELOAD_BUKKIT, SAVE_WORLDS, STOP, BACKUP
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((requestParams == null) ? 0 : requestParams.hashCode());
		result = prime * result
				+ ((requestType == null) ? 0 : requestType.hashCode());
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
		WebbyLocalData other = (WebbyLocalData) obj;
		if (requestParams == null) {
			if (other.requestParams != null)
				return false;
		} else if (!requestParams.equals(other.requestParams))
			return false;
		if (requestType != other.requestType)
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("WebbyLocalData [requestType=").append(requestType)
				.append(", requestParams=").append(requestParams).append("]");
		return builder.toString();
	}
}
