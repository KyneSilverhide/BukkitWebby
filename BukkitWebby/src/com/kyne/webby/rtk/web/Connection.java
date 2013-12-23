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

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.URLDecoder;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONObject;

import com.drdanick.McRKit.ToolkitAction;
import com.kyne.webby.commons.LogHelper;
import com.kyne.webby.commons.WebbyKeys;
import com.kyne.webby.commons.WebbyYAMLParser;
import com.kyne.webby.rtk.modules.WebbyRTKModule;
public class Connection extends Thread {

	private final Socket clientSocket;
	private final WebbyRTKModule rtkModule;
	private final WebServer webServer;
	private final Pattern urlRegex;

	public Connection(final WebbyRTKModule rtkModule, final Socket clientSocket, final WebServer webServer) {
		LogHelper.initLogger("WebbyRTKModule:Connection", "Minecraft");
		this.clientSocket = clientSocket;
		this.rtkModule = rtkModule;
		this.webServer = webServer;
		this.urlRegex = Pattern.compile("([^\\?]*)([^#]*)");
	}

	@Override
	public void run() {
		InputStream in = null;
		try {
			in = this.clientSocket.getInputStream();
			String url = null;
			String params = null;

			final StringBuffer buff = new StringBuffer();
			final int b = in.read();
			if(b < 0) {
				return;
			}
			buff.appendCodePoint(b);
			while (0 != in.available()) {
				buff.appendCodePoint( in.read());
			}
			final String httpContent = buff.toString();
			final StringTokenizer tokenizer = new StringTokenizer(httpContent, "\n");
			final String firstLine = tokenizer.nextToken();
			final String[] splittedFirstLine = firstLine.split(" ");
			if(splittedFirstLine.length > 1) {
				final String requestUrl = (firstLine.split(" "))[1]; //GET /url?params HTTP/1.X   or   //POST /url HTTP/1.X
				final Matcher result = this.urlRegex.matcher(requestUrl);
				if(result.find()) {
					url = result.group(1);
					params = result.group(2);
				} else {
					LogHelper.warn("Invalid URL format : " + requestUrl);
				}
				if(httpContent.startsWith("POST")){
					String lastLine = null;
					while(tokenizer.hasMoreTokens()) {
						lastLine = tokenizer.nextToken();
					}
					params = "?" + lastLine;
				}
			} else {
				LogHelper.warn("Empty Request with HttpContent = " + httpContent);
			}
			
			final boolean isAllowedRessource;
			if(url == null) {
				LogHelper.warn("Null url " + url);
				isAllowedRessource = false;
			} else {
				isAllowedRessource = this.isRestrictedUrl(url) || this.isContextualCallUrl(url) || 
						this.webServer.isAllowedRessource(url) || this.isPredefinedUrl(url);
			}
			if(isAllowedRessource) {
				if(url != null && params != null) {
					this.handleRequest(url, params, this.clientSocket);
				}
			} else {
				this.handleRequest("/404", params, clientSocket); //Forward to 404
			}
		} catch(final SocketException e) {
			/* Pics or it didn't happen ! */
		} catch (final Exception e) {
			LogHelper.error(e.getMessage(), e);
		} finally {
			if(in != null) {
				try {
					in.close();
				} catch (final IOException e) { /**/ }
			}
		}
	}

	private boolean isPredefinedUrl(final String url) {
		return "/login".equals(url) || "/logout".equals(url) || "/404".equals(url) || "/favicon.ico".equals(url);
	}

	@SuppressWarnings("unchecked")
	private void handleRequest(final String url, final String params, final Socket clientSocket) throws IOException, ClassNotFoundException {
		try {
			LogHelper.debug("Processing new request : " + url + " with params : " + params + ". ClientSocket = " + clientSocket);
			// Clean too old authentications
			this.cleanOldAuthentications();
			// If still connected, update
			this.updateLastAction(clientSocket.getInetAddress());

			// Favicons
			if(url.startsWith("/favicon.ico")) {
				return; // Ignore
			}
			// JS, CSS and images
			else if(url.startsWith("/js/")){
				this.webServer.printStaticFile(this.htmlDir() + url, "text/javascript", clientSocket, null);
				return;
			}
			else if(url.startsWith("/css/")){
				this.webServer.printStaticFile(this.htmlDir() + url, "text/css", clientSocket, null);
				return;
			}
			else if(url.startsWith("/images/")) {
				if(url.endsWith("png")) {
					this.webServer.printStaticFile(this.htmlDir() + url, "images/png", clientSocket, null);
				} else if(url.endsWith("gif")) {
					this.webServer.printStaticFile(this.htmlDir() + url, "images/gif", clientSocket, null);
				}
				return;
			}
			// Unrestricted HTML Pages
			if("/login".equals(url)) { // Login form
				final String login = this.getParam("login", params);
				final String pass = this.getParam("password", params);
				if(login == null && pass == null) { // Show form
					this.webServer.printStaticFile(this.htmlDir() + "login.html", "text/html", clientSocket, this.asMap("errorMsg:none"));
					return;
				} else {
					// Try to authenticate...
					if(this.checkAuthentication(login, pass)) {
						this.logUser(clientSocket.getInetAddress());
						final Map<InetAddress, String> lastUrlByIp = this.webServer.getLastUrlByIp();
						final String redirectUrl = lastUrlByIp.get(clientSocket.getInetAddress());
						this.webServer.clearLastUrlFor(clientSocket.getInetAddress());
						final String finalUrl = redirectUrl == null ? "/index" : redirectUrl;
						LogHelper.info("Login successful. Redirect to " + finalUrl);
						this.handleRequest(finalUrl, params, clientSocket);
						return;
					} else {
						LogHelper.info("Wrong login / password");
						this.webServer.printStaticFile(this.htmlDir() + "login.html", "text/html", clientSocket, this.asMap("errorMsg:Wrong Login/Password"));
						return;
					}
				}
			} else if("/logout".equals(url)) {
				this.logOut(clientSocket.getInetAddress().getHostAddress());
				this.webServer.printStaticFile(this.htmlDir() + "login.html", "text/html", clientSocket, null);
				return;
			}
			else if("/404".equals(url)) {
				this.webServer.printStaticFile(this.htmlDir() + "404.html", "text/html", clientSocket, null);
				return;
			}
			//'Contextual calls'
			else if(isContextualCallUrl(url)) {
				LogHelper.info("Contextual call detected : " + url);
				final String login = getParam("login", params);
				final String password = getParam("password", params);
				boolean authenticated = this.checkAuthentication(login, password);
				if(!authenticated) {
					LogHelper.info("Wrong login / password");
					this.webServer.printStaticFile(this.htmlDir() + "login.html", "text/html", clientSocket, this.asMap("errorMsg:Wrong Login/Password"));
					return;
				} else {
					// Handle the contextual call
					if(url.equals("/backupRemote")) {
						this.rtkModule.backupServer(false, webServer.getDefaultWorldName());
						this.webServer.printJSONObject(new JSONObject(), clientSocket);
						return;
					}
				}
			}
			// Restricted HTML Pages
			else if(this.isRestrictedUrl(url)) {
				if(this.isUserLoggedIn(clientSocket.getInetAddress())) {
					// User may access any pages
					if("/".equals(url) || "/index".equals(url)) { // Index
						this.webServer.printStaticFile(this.htmlDir() + "index.html", "text/html", clientSocket, this.asMap("errorMsg:none;infoMsg:none;warningMsg:none"));
						return;
					} else if("/indexJSON".equals(url)) {
						this.webServer.handleIndexJSON(clientSocket);
						return;
					} else if("/execCommand".equals(url)) {
						final String command = this.getParam("command", params);
						this.rtkModule.handleCommand(command);
						this.webServer.printJSONObject(new JSONObject(), clientSocket);
						return;
					} else if("/execBukkitCommand".equals(url)) {
						final String command = this.getParam("command", params);
						this.rtkModule.handleBukkitCommand(command);
						this.webServer.printJSONObject(new JSONObject(), clientSocket);
						return;
					} else if("/backupServer".equals(url)) {
						this.rtkModule.backupServer(false, webServer.getDefaultWorldName());
						this.webServer.printJSONObject(new JSONObject(), clientSocket);
						return;
					} else if("/backups".equals(url)) {
						this.webServer.printStaticFile(this.htmlDir() + "backups.html", "text/html", clientSocket, null);
						return;
					} else if("/backupsJSON".equals(url)) {
						final JSONObject backupJSON = this.webServer.getBackupJSON();
						this.webServer.printJSONObject(backupJSON, clientSocket);
						return;
					} else if("/deleteBackup".equals(url)) {
						final String fileName = this.getParam("fileName", params);
						final boolean success = this.webServer.deleteBackup(fileName);
						final JSONObject response = new JSONObject();
						response.put("success", success);
						this.webServer.printJSONObject(response, clientSocket);
						return;
					} else if("/restoreBackup".equals(url)) {
						// First, backup the server
						this.rtkModule.backupServer(true, webServer.getDefaultWorldName());
						
						// Then, shutdown the server
						this.rtkModule.handleCommand(ToolkitAction.HOLD.name());
						this.checkForShutdown();
						
						// Restore backup
						final String fileName = this.getParam("fileName", params);
						boolean success = this.rtkModule.restoreBackup(fileName);
						JSONObject response = new JSONObject();
						response.put("success", success);
						
						//Restart
						this.rtkModule.handleCommand(ToolkitAction.UNHOLD.name());
						this.webServer.printJSONObject(response, clientSocket);
						return;
					} else if("/about".equals(url)) {
						this.webServer.printStaticFile(this.htmlDir() + "about.html", "text/html", clientSocket, this.asMap("version:" + WebbyKeys.VERSION));
						return;
					} else if("/license".equals(url)) {
						final String license = this.webServer.getLicenseTxt();
						this.webServer.printPlainText(license, clientSocket);
						return;
					} else if("/bukkitconf".equals(url)) {
						this.webServer.printStaticFile(this.htmlDir() + "bukkitconf.html", "text/html", clientSocket, null);
						return;
					} else if("/saveConf".equals(url)) {
						this.webServer.saveBukkitConf(getParam("dataConf", params));
						this.webServer.printJSON(new JSONObject(), clientSocket);
						return;
					} else if("/bukkitconfJSON".equals(url)) {
						final Map<String, Object> conJSON = this.webServer.getConfJSON();
						this.webServer.printJSON(conJSON, clientSocket);
						return;
					}
				} else {
					this.webServer.saveLastUrlFor(clientSocket.getInetAddress(), url);
					this.handleRequest("/login", params, clientSocket); // Redirect to login
					return;
				}
			}
			// Not found -> wrong url
			LogHelper.warn("Unknow/Unsupported URL requested : " + url + ". Params = " + params);
			this.handleRequest("/404", params, clientSocket); //Forward to 404
		} catch(final SocketException e) { /* Go to hell */ }
		catch(final Exception e) {
			LogHelper.error("An error occured while processing a new HTTP request", e);
			e.printStackTrace();
		}
	}

	private boolean isContextualCallUrl(String url) {
		return "/backupRemote".equals(url); 
	}

	private void checkForShutdown() {
		boolean done = false;
		while(!done) {
			try {
				boolean serverOnline = this.rtkModule.pingServer();
				if(!serverOnline) { //Shutdown has been completed
					done = true;
					LogHelper.info("Server has been shut down.");
				} else { //Wait
					try {
						Thread.sleep(200);
						LogHelper.info("Waiting for server to shutdown...");
					} catch (InterruptedException e) { /**/ }
				}
			} catch (IOException e) {
				LogHelper.error("Unknown error : " + e);
				done = true;
			}
			
		}
	}
	
	private Map<String, String> asMap(final String keyValues) {
		return asMap(keyValues, ";", ":");
	}


	private Map<String, String> asMap(final String keyValues, final String lineSeparator, final String valSeparator) {
		final Map<String, String> map = new HashMap<String, String>();
		for(final String val : keyValues.split(lineSeparator)) {
			final String[] splittedVal = val.split(valSeparator);
			map.put(splittedVal[0], splittedVal[1]);
		}
		return map;
	}

	private boolean checkAuthentication(final String inputLogin, final String inputPassword) {
		return rtkModule.checkAuthentification(inputLogin, inputPassword);
	}

	private boolean isRestrictedUrl(final String url) {
		return "/index".equals(url) || "/".equals(url) || "/indexJSON".equals(url) || "/about".equals(url) || "/execCommand".equals(url)
				|| "/execBukkitCommand".equals(url) || "/license".equals(url) || "/bukkitconf".equals(url) || "/bukkitconfJSON".equals(url)
				|| "/backupServer".equals(url) || "/backups".equals(url) || "/backupsJSON".equals(url) || "/deleteBackup".equals(url)
				|| "/saveConf".equals(url) || "/restoreBackup".equals(url);
	}

	private String htmlDir() {
		return "plugins/BukkitWebby/html/";
	}

	private String getParam(final String key, final String params)
	{
		final Pattern regex = Pattern.compile("[\\?&]"+key+"=([^&#]*)");
		final Matcher result = regex.matcher(params);
		if(result.find()){
			try {
				return URLDecoder.decode(result.group(1), "UTF-8");
			} catch (UnsupportedEncodingException e) { 
				return result.group(1);
			}
		} else {
			return null;
		}
	}

	private boolean isUserLoggedIn(final InetAddress inetAddress) {
		return this.webServer.getConnectedAdmins().containsKey(inetAddress.getHostAddress());
	}

	private void logUser(final InetAddress inetAddress) {
		this.webServer.getConnectedAdmins().put(inetAddress.getHostAddress(), new Date());
	}

	public void updateLastAction(final InetAddress inetAddress) {
		if(this.isUserLoggedIn(inetAddress)) {
			this.logUser(inetAddress); //"re-log" with the current date
		} //else : "session timeout"
	}

	public void cleanOldAuthentications() {
		final Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MINUTE, -1 * WebbyYAMLParser.getInt("webby.sessionTimeout", this.rtkModule.getPluginConf(), 10));
		final Date limit = cal.getTime();
		for(final String userAdress : this.webServer.getConnectedAdmins().keySet()) {
			final Date date = this.webServer.getConnectedAdmins().get(userAdress);
			if(date.compareTo(limit) < 0) {
				LogHelper.info("User " + userAdress + " has been connected more than "
						+  WebbyYAMLParser.getInt("webby.sessionTimeout", this.rtkModule.getPluginConf(), 10) + " minute(s) ago, and will be disconnected");
				this.logOut(userAdress);
			}
		}
	}

	private void logOut(final String userAdress) {
		this.webServer.getConnectedAdmins().remove(userAdress);
	}
	
	public static void main(String args[]) throws UnsupportedEncodingException {
		final String s = "2012-07-07 20:17:21 [INFO] <*Console> H%C3%A9%C3%A9";
		System.out.println(s);
		System.out.println(new String(s.getBytes(), "UTF-8"));
		System.out.println(URLDecoder.decode(s, "UTF-8"));
	}
}
