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

package com.kyne.webby.bukkit;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.kyne.webby.commons.BackupUtils;
import com.kyne.webby.commons.BackupUtils.BackupMode;
import com.kyne.webby.commons.LogHelper;
import com.kyne.webby.commons.protocol.ServerInfos;
import com.kyne.webby.commons.protocol.WebbyLocalData;
import com.kyne.webby.commons.protocol.WebbyPlayer;

/**
 * This class will listen to all requests from the RTK module (and WebbyWebServer) and answer with informations
 */
public class RTKModuleSocket extends Thread {

	private final BukkitWebbyPlugin plugin;
	private final ServerSocket serverSocket;
//	private int requestCount = 0;

	public RTKModuleSocket(final int port, final BukkitWebbyPlugin plugin) throws IOException {
		this.serverSocket = new ServerSocket(port);
		this.plugin = plugin;
		LogHelper.initLogger("BukkitWebby", "Minecraft");
	}

	@Override
	public void run() {
		LogHelper.info("Webby Socket (BukkitPlugin) is listening on port : " + this.serverSocket.getLocalPort());
		while(!this.serverSocket.isClosed()) {
//			this.requestCount++;
			Socket clientSocket = null;
			ObjectInputStream ois = null;
			ObjectOutputStream oos = null;
			try {
				clientSocket = this.serverSocket.accept();
				ois = new ObjectInputStream(clientSocket.getInputStream());
				oos = new ObjectOutputStream(clientSocket.getOutputStream());

				final WebbyLocalData request = (WebbyLocalData) ois.readObject();
				final WebbyLocalData response = this.handleRequest(request);
				oos.writeObject(response);
			} catch (final SocketException e) {
				LogHelper.warn("Socket has been closed. If bukkit is stopping or restarting, this is normal");
			} catch (final IOException e) {
				LogHelper.error("An error occured while waiting for connections", e);
			} catch (final ClassNotFoundException e) {
				LogHelper.error("Unsupported object was sent to Webby ", e);
			} finally {
				IOUtils.closeQuietly(ois);
				IOUtils.closeQuietly(oos);
				IOUtils.closeQuietly(clientSocket);
			}
		}
	}

	/**
	 * As Bukkit doens't like multithread, we will only handle one request at a time to avoid concurrent modifications
	 * @return
	 */
	private synchronized WebbyLocalData handleRequest(final WebbyLocalData request) {
		final Map<String, Object> responseParams = new HashMap<String, Object>();
		final CommandSender sender = Bukkit.getConsoleSender();
		switch(request.getRequestType()) {
		case PING:
			responseParams.put("DATA", true);
			break;
		case GET_INFOS:
			getInfos(responseParams);
			break;
		case BACKUP:
			backupServer(sender, (String)request.getRequestParams().get("DEFAULT_WORLD_NAME"));
			if(request.getRequestParams().containsKey("NOTIFY_RESTORE")) {
				boolean notify = (Boolean) request.getRequestParams().get("NOTIFY_RESTORE");
				if(notify && Bukkit.getOnlinePlayers().size() > 0) {
					Bukkit.broadcastMessage("A full server restore has been planned. The server will now be restarted.");
				}
			}
			break;
		case SEND_COMMAND:
			final String command = (String) request.getRequestParams().get("COMMAND");
			LogHelper.info("Sending command " + command);
			// Special commands
			if(command != null && command.startsWith("heal")) {
				final String playerName = command.replaceFirst("heal ", "");
				if(playerName == null || "".equals(playerName.trim())) {
					LogHelper.warn("Empty or null player name given");
				}
				Player matchingPlayer = null;
				for(final Player player : Bukkit.getOnlinePlayers()) {
					if(player.getName().trim().equalsIgnoreCase(playerName.trim())) {
						matchingPlayer = player;
						break;
					}
				}
				if(matchingPlayer != null) {
					matchingPlayer.setHealth(matchingPlayer.getMaxHealth());
				} else {
					LogHelper.warn("Can't heal player " + playerName + " because it can't be found");
				}
			} 
			// Real commands
			else {
				Bukkit.dispatchCommand(sender, command);
			}
			break;
		case RELOAD_BUKKIT:
			Bukkit.reload();
			break;
		case STOP:
			Bukkit.shutdown();
			break;
		case SAVE_WORLDS:
			LogHelper.info("Saving all worlds...");
			List<World> worlds = new ArrayList<World>(Bukkit.getWorlds());
			for(final World world : worlds) {
				world.save();
			}
			LogHelper.info("Saving all online players...");
			Collection<? extends Player> players = Bukkit.getOnlinePlayers();
			for(final Player player : players) {
				player.saveData();
			}
			LogHelper.info("All words and players saved.");
		default:
			responseParams.put("ERROR", "Unsupported request type");
			break;
		}
		return new WebbyLocalData(request.getRequestType(), responseParams);
	}

	private void getInfos(final Map<String, Object> responseParams) {
		final List<WebbyPlayer> playerList = new ArrayList<WebbyPlayer>();
		for(final Player player : Bukkit.getOnlinePlayers()) {
			playerList.add(new WebbyPlayer(player.getName(), player.isOp()));
		}
		final Runtime runtime = Runtime.getRuntime();
		final long freeMemory = runtime.freeMemory();
		final long totalMemory = runtime.totalMemory();
		final long maxMemory = runtime.maxMemory();
		final int mb = 1024*1024;
		
		final ServerInfos serverInfos = new ServerInfos(
				Bukkit.getVersion(),
				Bukkit.getMaxPlayers(),
				playerList.size(),
				playerList, freeMemory/mb, totalMemory/mb, maxMemory/mb);
		responseParams.put("DATA", serverInfos);
	}

	private void backupServer(final CommandSender sender, final String defaultWorldName) {
		LogHelper.info("Starting backup!");
		// Warn users and save their stuff
		if(Bukkit.getOnlinePlayers().size() > 0) {
			Bukkit.broadcastMessage("Server backup is starting. You may experiment some lag.");
			for(final Player player : Bukkit.getOnlinePlayers()) {
				player.saveData();
			}
		}
		// Save, then disable auto-save
		List<World> worlds = new ArrayList<World>(Bukkit.getWorlds());
		for(final World world : worlds) {
			world.save();
		}
		Bukkit.dispatchCommand(sender, "save-off");
		//Start the backup
		final BackupMode backupMode = BackupMode.valueOf(plugin.getConfig().getString("webby.backup_mode", "SMP"));
		if(backupMode == BackupMode.BUKKIT) {
			BackupUtils.startBukkitBackup(this.plugin.getServer());
		} else {
			BackupUtils.startSMPBackup(this.plugin.getServer(), defaultWorldName);
		}
		//Enable auto save
		Bukkit.dispatchCommand(sender, "save-on");
		LogHelper.info("All backup tasks completed !");
	}

	public void closeSocket() {
		try {
			this.serverSocket.close();
		} catch (final IOException e) {
			LogHelper.error("Unable to close the Webby socket", e);
		}
	}
}
