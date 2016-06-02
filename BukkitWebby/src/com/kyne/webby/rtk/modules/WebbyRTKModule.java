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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;

import com.drdanick.McRKit.ToolkitAction;
import com.drdanick.McRKit.ToolkitEvent;
import com.drdanick.McRKit.Wrapper;
import com.drdanick.McRKit.api.RTKInterface;
import com.drdanick.McRKit.api.RTKInterface.CommandType;
import com.drdanick.McRKit.api.RTKInterfaceException;
import com.drdanick.McRKit.api.RTKListener;
import com.drdanick.McRKit.module.Module;
import com.drdanick.McRKit.module.ModuleLoader;
import com.drdanick.McRKit.module.ModuleMetadata;
import com.esotericsoftware.yamlbeans.YamlReader;
import com.esotericsoftware.yamlbeans.YamlWriter;
import com.kyne.webby.commons.BackupUtils;
import com.kyne.webby.commons.LogHelper;
import com.kyne.webby.commons.WebbyYAMLParser;
import com.kyne.webby.commons.protocol.ServerInfos;
import com.kyne.webby.commons.protocol.ServerStatus;
import com.kyne.webby.rtk.dto.User;
import com.kyne.webby.rtk.helper.WebbyFormatter;
import com.kyne.webby.rtk.web.WebServer;

public class WebbyRTKModule extends Module implements RTKListener {

	// Interface to Bukkit Plugin and RemoteToolkit
	private BukkitInterface bukkitInterface;
	private RTKInterface rtkInterface;

	// WebServer that handles HTTP requests
	private WebServer webServer;

	// Configuration file from BukkitWebby module
	private Map<String, Object> pluginConf;

	// Defined users
	private final List<User> definedUsers = new ArrayList<User>();

	public WebbyRTKModule(final ModuleMetadata moduleMetadata, final ModuleLoader moduleLoader, final ClassLoader classLoader) {
		super(moduleMetadata, moduleLoader, classLoader, ToolkitEvent.ON_TOOLKIT_START, ToolkitEvent.NULL_EVENT);

		//Fix formatting on logger (Based on MilkAdmin)
		final Logger rootlog = Logger.getLogger("");
		for (final Handler h : rootlog.getHandlers()){ //remove all handlers
			h.setFormatter(new WebbyFormatter());
		}
		LogHelper.initLogger("WebbyRTKModule", "Minecraft");
	}

	@Override
	protected void onEnable() {
		try {
			// Init configurations
			this.initRTKModule();

			// Init and start webserver
			final int webPort = WebbyYAMLParser.getInt("webby.port", this.pluginConf, 25567);
			this.webServer = new WebServer(this, webPort);
			this.webServer.start();
		} catch (final Exception e) {
			LogHelper.error("The WebServer couldn't be initialized due to the following error :" , e);
			e.printStackTrace();
		}
	}
	@SuppressWarnings("unchecked")
	private void initRTKModule() {
		final File configFile = new File("plugins/BukkitWebby/config.yml");

		// Check if file exists
		if(!configFile.exists()) {
			LogHelper.warn("Config.yml can't be found. If this is the first start, it's normal, and we will create it for you.");
			// Create the file
			FileWriter writer = null;
			try {
				writer = new FileWriter(configFile);
				writer.write(this.getConfigFileContent());
			} catch (final IOException ex) {
				LogHelper.error("Can't write the new new configuration file", ex);
			} finally {
				IOUtils.closeQuietly(writer);
			}
		}

		FileReader fileReader = null;
		try {
			fileReader = new FileReader(configFile);
			final YamlReader reader = new YamlReader(fileReader);
			this.pluginConf = (Map<String, Object>) reader.read();
		} catch (final Exception e) {
			LogHelper.error("Unable to read configuration file", e);
		} finally {
			IOUtils.closeQuietly(fileReader);
		}

		//Check for new values
		final Object avatars = WebbyYAMLParser.getObject("webby.show_avatars", this.pluginConf, null);
		if(avatars == null) {
			LogHelper.info("Updating configuration file with value 'show_avatars'");
			final Map<String, Object> webbyConf = (Map<String, Object>) this.pluginConf.get("webby");
			webbyConf.put("show_avatars", "true");
			FileWriter fileWriter = null;
			try {
				fileWriter = new FileWriter(configFile);
				YamlWriter writer = new YamlWriter(fileWriter);
				writer.write(pluginConf);
				writer.close();
			} catch (Exception e) {
				LogHelper.error("Unable to update the configuration file", e);
			} finally {
				IOUtils.closeQuietly(fileWriter);
			}
		}

		// Extract users
		final Map<String, Object> users = (Map<String, Object>) WebbyYAMLParser.getObject("webby.users", this.pluginConf, null);
		for(final Object userObjects : users.values()) {
			final Map<String, Object> userValues = (Map<String, Object>) userObjects;
			final User user = new User((String)userValues.get("login"), (String)userValues.get("password"));
			this.definedUsers.add(user);
		}
		if(this.definedUsers.size() == 0) {
			LogHelper.error("No user found in the configuration file. Nobody will be able to authenticate to BukkitWebby !!");
		}
	}

	/**
	 * Check if the given login and password match one of the defined users.
	 */
	public boolean checkAuthentification(final String inputLogin, final String inputPassword) {
		final String userLogin = inputLogin == null? "" : inputLogin;
		final String userPassword = inputPassword == null? "" : inputPassword;
		for(final User user : this.definedUsers) {
			if(userLogin.equals(user.getLogin()) && userPassword.equals(user.getPassword())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Get the content of a new, default configuration file
	 */
	private String getConfigFileContent() {
		final String YAMLINDENT = "    "; //Can't use \t (tabulations) in YAML files
		final StringBuilder sBuilder = new StringBuilder();
		sBuilder.append("#BukkitWebby configuration file (DON'T USE TABULATIONS OR THIS FILE WON'T BE READABLE)\n")
		.append("#-------------------------------------------------------------------------------------\n")
		.append("\n")
		.append("#Webby options\n")
		.append("webby:\n")
		.append(YAMLINDENT + "#Webserver port used to listen to web connections (Ex: http://localhost:25567/login) (default = 25567) MUST BE DIFFERENT THAN ALL OTHER PORTS \n")
		.append(YAMLINDENT + "port: 25567\n")
		.append(YAMLINDENT + "#Local port used by the Webby RTKModule to communicate with the Webby Bukkit Plugin (default = 25564). MUST BE DIFFERENT THAN ALL OTHER PORTS \n")
		.append(YAMLINDENT + "localPort : 25564\n")
		.append(YAMLINDENT + "#Administration login\n")
		.append(YAMLINDENT + "users: \n")
		.append(YAMLINDENT + YAMLINDENT + "admin: #Simple name, only used to split each user \n")
		.append(YAMLINDENT + YAMLINDENT + YAMLINDENT + "login: admin\n")
		.append(YAMLINDENT + YAMLINDENT + YAMLINDENT + "password: admin\n")
		.append(YAMLINDENT + "#Timeout in minutes before a user has to relog after inactivity (default = 10 minutes)\n")
		.append(YAMLINDENT + "sessionTimeout: 10\n")
		.append(YAMLINDENT + "#Show user avatars next to their name, using Minotar.net service (requires Internet access) \n")
		.append(YAMLINDENT + "show_avatars: true\n")
		.append(YAMLINDENT + "#Switch the backup. If set to BUKKIT, each world is expected to have its own directory. if set to SMP, a unique directory called 'your_world_name' should exists.\n")
		.append(YAMLINDENT + "backupMode: BUKKIT\n")
		.append(YAMLINDENT + "#Switch the log. If set to OLD, Bukkit Webby will use the server.log file. If set to NEW (default), it will look into logs/latest.log\n")
		.append(YAMLINDENT + "logMode: NEW\n")
		.append("\n")
		.append("rtk:\n")
		.append(YAMLINDENT + "#RemoteToolkit port (Same as in remote.properties, default = 25561)\n")
		.append(YAMLINDENT + "port: 25561\n")
		.append(YAMLINDENT + "#Host. You shouldn't have to change this. (Default = localhost) \n")
		.append(YAMLINDENT + "host: localhost\n")
		.append(YAMLINDENT + "#RemoteToolkit login (See the rtoolkit.sh or .bat files, default = user)\n")
		.append(YAMLINDENT + "login: user\n")
		.append(YAMLINDENT + "#RemoteToolkit password  (See the rtoolkit.sh or .bat files, default = pass)\n")
		.append(YAMLINDENT + "password: pass\n");
		return sBuilder.toString();
	}

	@Override
	protected void onDisable() {
		this.webServer.stopServer();
		LogHelper.info("Webserver has been successfully disabled");
	}

	@Override
	public void onRTKStringReceived(final String message) {
		if(message.equals("RTK_TIMEOUT")){
			LogHelper.warn("Remote Toolkit is not responding. If this happen more than once, there probably is something wrong with your config.yml");
		} else {
			LogHelper.info("RemoteToolkit : " + message);
		}
	}

	/**
	 * Handle the given command name (it can be send to the RTK wrapper or to the bukkit server itself)
	 * @param command the command name (see RTK CommandType)
	 * @throws IOException
	 */
	public void handleCommand(final String command) throws IOException {
		LogHelper.debug("Sendng command " + command);
		try {
			// Bukkit commands
			if("RELOAD".equalsIgnoreCase(command)) {
				this.getBukkitInterface().reloadServer();
			} else if("STOP".equalsIgnoreCase(command)) {
				this.getRtkInterface().executeCommand(CommandType.DISABLE_RESTARTS, null);
				this.getBukkitInterface().stopServer();
			} else if("SAVE".equalsIgnoreCase(command)) {
				this.getBukkitInterface().saveAllWorlds();
				LogHelper.info("All worlds have been saved");
			}
			// RTK commands
			else {
				//Call to RTK Interface (no login/password required)
				if("RESTART".equalsIgnoreCase(command)) {
					this.bukkitInterface.setLastKnownStatus(ServerStatus.RESTARTING);
				}
				Wrapper.getInterface().performAction(ToolkitAction.valueOf(command),null);
			}
		} catch(final IllegalArgumentException e) {
			LogHelper.error("Unsupported command " + command + " will be ignored", e);
		} catch (final RTKInterfaceException e) {
			LogHelper.error("RTK error : unable to disable the wrapper 'restarts on stop'", e);
		}
	}

	public void backupServer(final boolean notifyForRestore, final String defaultWorldName) throws IOException {
		this.getBukkitInterface().backupServer(notifyForRestore, defaultWorldName);
	}

	public boolean restoreBackup(final String fileName) throws IOException {
		if(fileName == null) {
			LogHelper.error("Unable to restore backup : fileName is null !");
			return false;
		} else {
			// Checks
			final File backupRoot = new File("Backups/");
			final File backupFile = new File(backupRoot, fileName);
			if(!backupFile.exists()) {
				LogHelper.error("Backup file " + backupFile + " can't be found. Unable to restore the requested backup");
				return false;
			}

			// Restore backup
			boolean success = true;
			try {
				final File serverRoot = new File(".");
				BackupUtils.extractZip(backupFile, serverRoot);
			} catch (final Exception e) {
				success = false;
				LogHelper.error("An error occured while restoring the backup. Don't panic, you can still " +
						"try manually by unzipping the file " + fileName + " to yout server root", e);
				return false;
			}
			if(success) {
				LogHelper.info("Backup has been successfully restored. Restarting server");
			}
			return success;
		}
	}

	public void handleBukkitCommand(final String command) throws IOException {
		this.getBukkitInterface().dispatchCommand(command);
	}

	public boolean pingServer() throws IOException {
		return this.getBukkitInterface().pingServer();
	}

	public ServerStatus askForServerStatus() {
		try {
			return this.getBukkitInterface().getServerStatus();
		} catch (final IOException e) {
			LogHelper.error("Unable to interact with Bukkit", e);
			return ServerStatus.OFF;
		}
	}

	public ServerInfos askForServerInfos() {
		try {
			return this.bukkitInterface.getServerInfos();
		} catch (final IOException e) {
			LogHelper.error("Unable to interact with Bukkit", e);
			return null;
		}
	}

	public List<String> readConsoleLog(final LogMode logMode) {
		final List<String> logLines = new ArrayList<String>();
		String line = "";
		
		File logFile = null;
		if(logMode == LogMode.NEW) {
			logFile = new File("logs/latest.log");
		} else if(logMode == LogMode.OLD){
			logFile = new File("server.log");
		} else {
			throw new UnsupportedOperationException("Unsupported log mode " + logMode);
		}

		if(!logFile.exists()) {
			LogHelper.error("Unable to find the log file at " + logFile.getAbsolutePath());
			return Arrays.asList("Unable to find the log file");
		}
		
		RandomAccessFile randomFile = null;
		try{
			randomFile = new RandomAccessFile(logFile, "r");
			final long linesToRead = 100;
			final long fileLength = randomFile.length();
			long startPosition = fileLength - (linesToRead * 100);
			if(startPosition < 0) {
				startPosition = 0;
			}
			randomFile.seek(startPosition);
			while((line = randomFile.readLine()) != null) {
				logLines.add(line.replace("[0;30;22m", "")
								 .replace("[0;34;22m", "")
								 .replace("[0;32;22m", "")
								 .replace("[0;36;22m", "")
								 .replace("[0;31;22m", "")
								 .replace("[0;35;22m", "")
								 .replace("[0;33;22m", "")
								 .replace("[0;37;22m", "")
								 .replace("[0;30;1m", "")
								 .replace("[0;34;1m", "")
								 .replace("[0;32;1m", "")
								 .replace("[0;36;1m", "")
								 .replace("[0;31;1m", "")
								 .replace("[0;35;1m", "")
								 .replace("[0;33;1m", "")
								 .replace("[0;37;1m", "")
								 .replace("[m", "")
								 .replace("[5m", "")
								 .replace("[21m", "")
								 .replace("[9m", "")
								 .replace("[4m", "")
								 .replace("[3m", "")
								 .replace("[0;39m", "")
								 .replace("[0m", ""));
				
			}
		} catch(final IOException e) {
			LogHelper.error("Unable to read server.log", e);
		} finally {
			IOUtils.closeQuietly(randomFile);
		}
		return logLines;
	}

	public Map<String, Object> getPluginConf() {
		return this.pluginConf;
	}

	public void setPluginConf(final Map<String, Object> pluginConf) {
		this.pluginConf = pluginConf;
	}

	private BukkitInterface getBukkitInterface() throws IOException {
		if(this.bukkitInterface == null) {
			this.bukkitInterface = new BukkitInterface(WebbyYAMLParser.getInt("webby.localPort", this.pluginConf, 25564));
		}
		return this.bukkitInterface;
	}

	private RTKInterface getRtkInterface() throws RTKInterfaceException {
		if(this.rtkInterface == null) {
			//Init RTKInterface
			final int rtkPort = WebbyYAMLParser.getInt("rtk.port", this.pluginConf, 25561);
			final String host = WebbyYAMLParser.getString("rtk.host", this.pluginConf, "localhost");
			final String user = WebbyYAMLParser.getString("rtk.login", this.pluginConf, "user");
			final String password = WebbyYAMLParser.getString("rtk.password", this.pluginConf, "pass");
			this.rtkInterface = RTKInterface.createRTKInterface(rtkPort, host, user, password);
		}
		return this.rtkInterface;
	}
	
	public enum LogMode {
		OLD, NEW;
	}
}