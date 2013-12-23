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

package com.kyne.webby.rtk.modules;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import com.kyne.webby.commons.LogHelper;
import com.kyne.webby.commons.protocol.ServerInfos;
import com.kyne.webby.commons.protocol.ServerStatus;
import com.kyne.webby.commons.protocol.WebbyLocalData;
import com.kyne.webby.commons.protocol.WebbyLocalData.RequestType;

/**
 * This class handle all communications between the RTK module and the Bukkit plugin. The communication is made
 * with a basic socket on a user defined port.
 */
public class BukkitInterface {

	private final int localPort;
	private ServerStatus lastKnownStatus;

	protected BukkitInterface(final int localPort) throws IOException {
		this.localPort = localPort;
		this.lastKnownStatus = ServerStatus.ON;
	}

	protected ServerStatus getServerStatus() {
		return this.lastKnownStatus;
	}

	protected void saveAllWorlds() throws IOException {
		this.sendRequestAndCastResponse(RequestType.SAVE_WORLDS, null, Void.class);
	}

	protected boolean pingServer() throws IOException {
		final Object pong = this.sendRequestAndCastResponse(RequestType.PING, null, Void.class);
		if(pong == null) {
			return false;
		}
		return (Boolean)pong;
	}

	protected void dispatchCommand(final String command) throws IOException {
		final Map<String, Object> params = new HashMap<String, Object>();
		params.put("COMMAND", command);
		this.sendRequestAndCastResponse(RequestType.SEND_COMMAND, params, Void.class);
	}

	protected void reloadServer() throws IOException {
		this.sendRequestAndCastResponse(RequestType.RELOAD_BUKKIT, null, Void.class);
	}

	protected void stopServer() throws IOException {
		this.sendRequestAndCastResponse(RequestType.STOP, null, Void.class);
	}

	protected ServerInfos getServerInfos() throws IOException {
		return this.sendRequestAndCastResponse(RequestType.GET_INFOS, null, ServerInfos.class);
	}

	protected void backupServer(final boolean notifyForRestore, final String defaultWorldName) throws IOException {
		final Map<String, Object> params = new HashMap<String, Object>();
		params.put("NOTIFY_RESTORE", notifyForRestore);
		params.put("DEFAULT_WORLD_NAME", defaultWorldName);
		this.sendRequestAndCastResponse(RequestType.BACKUP, params, Void.class);
	}

	@SuppressWarnings("unchecked")
	private <T> T sendRequestAndCastResponse(final RequestType type, final Map<String, Object> params,
			final Class<T> responseDataType) throws IOException {
		Socket clientSocket = null;
		ObjectOutputStream serverOutputStream = null;
		ObjectInputStream serverInputStream = null;

		final WebbyLocalData request = new WebbyLocalData(type, params);
		try {
			clientSocket = new Socket("localhost", this.localPort);
			serverOutputStream = new ObjectOutputStream(clientSocket.getOutputStream());
			serverInputStream = new ObjectInputStream(clientSocket.getInputStream());
			serverOutputStream.writeObject(request);
			final WebbyLocalData response = (WebbyLocalData) serverInputStream.readObject();
			if(this.lastKnownStatus != ServerStatus.RESTARTING) {
				this.lastKnownStatus = ServerStatus.ON;
			}
			return (T) response.getRequestParams().get("DATA");
		} catch (final Exception e) {
			this.lastKnownStatus = ServerStatus.OFF;
			LogHelper.debug("Bukkit is offline or restarting");
			return null;
		} finally {
			IOUtils.closeQuietly(serverInputStream);
			IOUtils.closeQuietly(serverOutputStream);
			IOUtils.closeQuietly(clientSocket);
		}
	}

	protected void setLastKnownStatus(final ServerStatus lastKnownStatus) {
		this.lastKnownStatus = lastKnownStatus;
	}
}