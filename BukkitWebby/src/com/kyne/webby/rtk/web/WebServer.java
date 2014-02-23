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

package com.kyne.webby.rtk.web;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URLDecoder;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.kyne.webby.commons.LogHelper;
import com.kyne.webby.commons.WebbyYAMLParser;
import com.kyne.webby.commons.protocol.ServerInfos;
import com.kyne.webby.commons.protocol.WebbyPlayer;
import com.kyne.webby.rtk.modules.WebbyRTKModule;
import com.kyne.webby.rtk.modules.WebbyRTKModule.LogMode;

public class WebServer extends Thread {

	private final ServerSocket serverSocket;
	private final WebbyRTKModule rtkModule;

	private final Map<String, Date> connectedAdmins;
	private final Map<InetAddress, String> lastUrlByIp;
	private final Map<String, byte[]> htmlCache;

	private final List<String> allowedRessources;

	public WebServer(final WebbyRTKModule rtkModule, final int port) throws IOException {
		LogHelper.initLogger("WebbyRTKModule:WebServer", "Minecraft");
		this.connectedAdmins = new HashMap<String, Date>();
		this.lastUrlByIp = new HashMap<InetAddress, String>();
		this.htmlCache = new HashMap<String, byte[]>();
		this.serverSocket = new ServerSocket(port);
		this.rtkModule = rtkModule;
		this.allowedRessources = this.getAllowedRessources();
	}

	@Override
	public void run() {
		LogHelper.info("Web server is listening on port : " + WebbyYAMLParser.getString("webby.port", this.rtkModule.getPluginConf(), "25567"));
		while(!this.serverSocket.isClosed()){
			Socket clientSocket;
			try {
				clientSocket = this.serverSocket.accept();
				final Thread connection = new Connection(this.rtkModule, clientSocket, this);
				connection.start();
			}
			catch (final SocketException e) {
				LogHelper.warn("Socket has been closed. If RemoteToolkit is restarting/reloading, this is normal");
			} catch (final IOException e) {
				LogHelper.error("An error occured while waiting for requests", e);
			}
		}
	}

	public void stopServer() {
		try {
			this.serverSocket.close();
		} catch (final IOException e) {
			LogHelper.error("Unable to close the Webserver socket", e);
		}
	}

	/**
	 * Read a print a static file (js, css, html or png).
	 * @param path the path to the file
	 * @param type the mimetype
	 * @param jsStates some javascripts variables that may be initialized in the printed content. Null if not required.
	 * @param clientSocket the client-side socket
	 */
	public void printStaticFile(final String path, final String type, final Socket clientSocket,
			final Map<String, String> jsStates) {
		try {
			final File htmlPage = new File(path);
			if(htmlPage.exists()) {
				final DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
				out.writeBytes("HTTP/1.1 200 OK\r\n");
				out.writeBytes("Content-Type: " + type + "; charset=utf-8\r\n");
				out.writeBytes("Cache-Control: no-cache \r\n");
				out.writeBytes("Server: Bukkit Webby\r\n");
				out.writeBytes("Connection: Close\r\n\r\n");
				if(jsStates != null) {
					out.writeBytes("<script type='text/javascript'>");
					for(final String var : jsStates.keySet()) {
						out.writeBytes("var " + var + " = '" + jsStates.get(var) + "';");
					}
					out.writeBytes("</script>");
				}
				if(!this.htmlCache.containsKey(path)) { //Pages are static, so we can "pre-read" them. Dynamic content will be rendered with javascript
					final FileInputStream fis = new FileInputStream(htmlPage);
					final byte fileContent[] = new byte[(int)htmlPage.length()];
					fis.read(fileContent);
					fis.close();
					this.htmlCache.put(path, fileContent);
				} else {
					LogHelper.debug("File will be added in Webby's cache");
				}
				out.write(this.htmlCache.get(path));
				out.flush();
				out.close();
			} else {
				LogHelper.warn("Requested file " + path + " can't be found");
			}
		} catch(final SocketException e) {
			/* Or not ! */
		} catch (final Exception e) {
			LogHelper.error(e.getMessage(), e);
		}
	}

	/**
	 * Print the given String as plain text
	 * @param text the text to print
	 * @param clientSocket the client-side socket
	 */
	public void printPlainText(final String text, final Socket clientSocket) {
		try {
			final DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
			out.writeBytes("HTTP/1.1 200 OK\r\n");
			out.writeBytes("Content-Type: text/plain; charset=utf-8\r\n");
			out.writeBytes("Cache-Control: no-cache \r\n");
			out.writeBytes("Server: Bukkit Webby\r\n");
			out.writeBytes("Connection: Close\r\n\r\n");
			out.writeBytes(text);
			out.flush();
			out.close();

		} catch(final SocketException e) {
			/* .. */
		} catch(final Exception e) {
			LogHelper.error(e.getMessage(), e);
		}
	}

	@SuppressWarnings("unchecked")
	public void printJSON(final Map<String, Object> data, final Socket clientSocket) {
		try {
			final DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
			out.writeBytes("HTTP/1.1 200 OK\r\n");
			out.writeBytes("Content-Type: application/json; charset=utf-8\r\n");
			out.writeBytes("Cache-Control: no-cache \r\n");
			out.writeBytes("Server: Bukkit Webby\r\n");
			out.writeBytes("Connection: Close\r\n\r\n");
			final JSONObject json = new JSONObject();
			json.putAll(data);
			out.writeBytes(json.toJSONString());
			out.flush();
			out.close();
		}
		catch(final SocketException e) {
			/* .. */
		} catch(final Exception e) {
			LogHelper.error(e.getMessage(), e);
		}
	}

	public void printJSONObject(final JSONObject data, final Socket clientSocket) {
		try {
			final DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
			out.writeBytes("HTTP/1.1 200 OK\r\n");
			out.writeBytes("Content-Type: application/json; charset=utf-8\r\n");
			out.writeBytes("Cache-Control: no-cache \r\n");
			out.writeBytes("Server: Bukkit Webby\r\n");
			out.writeBytes("Connection: Close\r\n\r\n");
			out.writeBytes(data.toJSONString());
			out.flush();
			out.close();
		}
		catch(final SocketException e) {
			/* .. */
		} catch(final Exception e) {
			LogHelper.error(e.getMessage(), e);
		}
	}

	@SuppressWarnings("unchecked")
	public void handleIndexJSON(final Socket clientSocket) throws IOException, ClassNotFoundException {
		final Map<String, Object> data = new HashMap<String, Object>();
		data.put("status", this.rtkModule.askForServerStatus().name());
		final JSONArray logLines = new JSONArray();
		LogMode logMode = LogMode.valueOf(WebbyYAMLParser.getString("webby.logMode", rtkModule.getPluginConf(), "NEW"));
		for(final String line : this.rtkModule.readConsoleLog(logMode)) {
			final JSONObject lineJSON = new JSONObject();
			lineJSON.put("line", line);
			lineJSON.put("isWarn", line.contains("[WARNING]"));
			lineJSON.put("isError", (line.contains("[ERROR]") || line.contains("[SEVERE]")));
			logLines.add(lineJSON);
		}
		data.put("log", logLines);
		final ServerInfos infos = this.rtkModule.askForServerInfos();
		if(infos != null) {
			data.put("bukkitVersion", infos.getBukkitVersion());
			data.put("maxPlayers", infos.getMaxPlayers());
			data.put("curPlayers", infos.getOnlinePlayerCount());

			// Player details
			final JSONArray playersJSON = new JSONArray();
			for(final WebbyPlayer player : infos.getPlayers()) {
				final JSONObject playerJSON = new JSONObject();
				playerJSON.put("name", player.getName());
				playerJSON.put("op", player.isOp());
				playersJSON.add(playerJSON);
			}
			data.put("playerList", playersJSON);

			final double usedMemory = infos.getTotalMemory() - infos.getFreeMemory();
			data.put("usedMemory", usedMemory);
			final double totalMemory = infos.getTotalMemory();
			data.put("totalMemory", totalMemory);
			data.put("maxMemory", infos.getMaxMemory());

			final double percentage = usedMemory/totalMemory * 100.0;
			data.put("memPercentage", percentage);
		}
		final String showAvatar = WebbyYAMLParser.getString("webby.show_avatars", this.rtkModule.getPluginConf(), "true");
		data.put("showAvatars", showAvatar);
		this.printJSON(data, clientSocket);
	}

	public Map<String, Object> getConfJSON() {
		final Map<String, Object> data = new HashMap<String, Object>();
		FileInputStream in = null;
		try {
			final Properties properties = new Properties();
			in = new FileInputStream("server.properties");
			properties.load(in);

			for(final Object key : properties.keySet()) {
				data.put((String)key, properties.getProperty((String)key));
			}
		} catch (final FileNotFoundException e) {
			LogHelper.error("Unable to find bukkit configuration file", e);
		} catch (final IOException e) {
			LogHelper.error("Unable to read bukkit configuration file", e);
		} finally {
			try {
				if (in != null) {
					in.close();
				}
			} catch (final IOException ex) { /**/ }
		}
		return data;
	}
	
	public String getDefaultWorldName() {
		FileInputStream in = null;
		try {
			final Properties properties = new Properties();
			in = new FileInputStream("server.properties");
			properties.load(in);

			for(final Object key : properties.keySet()) {
				if("level-name".equals(key)) {
					return properties.getProperty((String) key);
				}
			}
		} catch (final FileNotFoundException e) {
			LogHelper.error("Unable to find bukkit configuration file", e);
		} catch (final IOException e) {
			LogHelper.error("Unable to read bukkit configuration file", e);
		} finally {
			try {
				if (in != null) {
					in.close();
				}
			} catch (final IOException ex) { /**/ }
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public JSONObject getBackupJSON() {

		final JSONArray dateListJSON = new JSONArray();
		final File backupDir = new File("Backups");
		if(!backupDir.exists()) {
			backupDir.mkdir();
		}
		final File[] backupFiles = backupDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(final File dir, final String name) {
				return new File(dir, name).isFile() && name.endsWith("zip");
			}
		});
		final SimpleDateFormat zipFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
		final SimpleDateFormat friendlyFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		final SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd");

		final Map<Date, List<File>> backupByDays = new HashMap<Date, List<File>>();
		for(final File backupFile : backupFiles) {
			Date backupDate = null;
			Date backupDay = null;
			try {
				backupDate = zipFormat.parse(backupFile.getName());
				final String day = dayFormat.format(backupDate);
				backupDay = dayFormat.parse(day);
			} catch (final ParseException e) {
				LogHelper.warn("The backup file " + backupFile.getName() + " is not well formed. Format should be yyyyMMdd_HHmmss.zip");
			}
			if(!backupByDays.containsKey(backupDay)) {
				backupByDays.put(backupDay, new ArrayList<File>());
			}
			backupByDays.get(backupDay).add(backupFile);
		}
		final ArrayList<Date> sortedDays = new ArrayList<Date>(backupByDays.keySet());
		Collections.sort(sortedDays, new Comparator<Date>() {
			@Override
			public int compare(final Date o1, final Date o2) {
				return o2.compareTo(o1);
			}
		});

		final double MB = 1024*1024;


		for(final Date day : sortedDays) {
			final String dayStr = dayFormat.format(day);
			final JSONObject dateJSON = new JSONObject();
			final JSONArray backups = new JSONArray();
			for(final File backupFile : backupByDays.get(day)) {
				final JSONObject backup = new JSONObject();
				Date backupDate = null;
				try {
					backupDate = zipFormat.parse(backupFile.getName());
				} catch (final ParseException e) { /**/ }
				backup.put("date", backupDate == null ? "???" : friendlyFormat.format(backupDate));
				final long byteSize = backupFile.length();
				backup.put("size", NumberFormat.getInstance().format(byteSize/MB) + " MB");
				backup.put("name", backupFile.getName());
				backups.add(backup);
			}
			dateJSON.put("day", dayStr);
			dateJSON.put("backups", backups);

			dateListJSON.add(dateJSON);
		}
		final JSONObject backupJSON = new JSONObject();
		backupJSON.put("dates", dateListJSON);

		return backupJSON;
	}

	public void saveBukkitConf(final String paramProperties) throws IOException {
		String decodedProperties = URLDecoder.decode(paramProperties, "UTF-8");
		FileOutputStream fos = null;
		OutputStreamWriter osw = null;
		try {
			final Properties properties = new Properties();
			fos = new FileOutputStream("server.properties");
			osw = new OutputStreamWriter(fos, "UTF8");
			for(final String property : decodedProperties.split("&")) {
				final String[] propertyArray = property.split("=");
				final String key = propertyArray[0];
				if(!"dataConf".equals(key)) { //System parameter
					final String value = propertyArray.length == 2 ? propertyArray[1] : "" /* empty value */;
					properties.setProperty(key, value);
				}
			}
			
			properties.store(osw, "Minecraft server properties");
		} finally {
			IOUtils.closeQuietly(osw);
		}
	}

	public boolean deleteBackup(final String fileName) {
		if(fileName == null || "".equals(fileName.trim())) {
			LogHelper.error("Filename is null or empty !");
			return false;
		}
		LogHelper.info("Deleting backup file " + fileName);
		final File backupDir = new File("Backups");
		final File backupFile = new File(backupDir, fileName);
		if(!backupFile.exists()) {
			LogHelper.error("File doesn't exist");
			return false;
		}
		final boolean success = backupFile.delete();
		if(success) {
			LogHelper.info("Backup successfully deleted.");
		} else {
			LogHelper.error("Unable to delete file");
		}
		return success;
	}

	public Map<String, Date> getConnectedAdmins() {
		return this.connectedAdmins;
	}

	public Map<String, byte[]> getHtmlCache() {
		return this.htmlCache;
	}

	public String getLicenseTxt() {
		FileInputStream fis = null;
		BufferedInputStream bis = null;
		try {
			final File license = new File("plugins/BukkitWebby/license.txt");
			final byte[] buffer = new byte[(int) license.length()];
			fis = new FileInputStream(license);
			bis = new BufferedInputStream(fis);
			bis.read(buffer);
			return new String(buffer);
		} catch (final FileNotFoundException e) {
			LogHelper.error("Unable to find the license file", e);
		} catch (final IOException e) {
			LogHelper.error("Unable to read the license file", e);
		} finally {
			if(fis != null) {
				try {
					fis.close();
				} catch (final IOException e) { /**/ }
			}
			if(bis != null) {
				try {
					bis.close();
				} catch (final IOException e) { /**/ }
			}
		}
		return null;
	}

	public Map<InetAddress, String> getLastUrlByIp() {
		return this.lastUrlByIp;
	}

	public void saveLastUrlFor(final InetAddress inetAddress, final String url) {
		if(!url.contains("JSON")) { //Save real url
			this.lastUrlByIp.put(inetAddress, url);
		}
	}

	public void clearLastUrlFor(final InetAddress inetAddress) {
		this.lastUrlByIp.remove(inetAddress);
	}

	public boolean isAllowedRessource(final String url) {
		final int lastSlashIndex = url.lastIndexOf('/');
		if(lastSlashIndex < url.length()) {
			final String requestedRessource = url.substring(lastSlashIndex+1, url.length());
			if(this.allowedRessources.contains(requestedRessource)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * List of file names in the HTML folder (recursive). Only these files can be requested by the webserver
	 */
	public List<String> getAllowedRessources() {
		final File htmlRoot = new File("plugins/BukkitWebby/html/");
		final Collection<File> allHtmlFiles = FileUtils.listFiles(htmlRoot, null /* all extension */, true /* recursive*/);
		final List<String> fileNames = new ArrayList<String>();
		for(final File htmlFile : allHtmlFiles) {
			fileNames.add(htmlFile.getName());
		}
		return fileNames;
	}
}
