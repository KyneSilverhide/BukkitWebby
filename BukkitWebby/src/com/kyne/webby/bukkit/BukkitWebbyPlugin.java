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

import java.io.File;

import org.bukkit.configuration.Configuration;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import com.kyne.webby.commons.LogHelper;

/**
 * This class is the main entry of the plugin. It check all configurations files and interacts with Bukkit.
 * See the rtk package for all business related to the Webserver and the RTK wrapper.
 */
public class BukkitWebbyPlugin extends JavaPlugin {

	private PluginDescriptionFile description;
	private Configuration pluginConfiguration;

	private RTKModuleSocket localCommThread;

	@Override
	public void onLoad() { //Logger need to be initialized before the onEnable
		description = this.getDescription();
		pluginConfiguration = this.getConfig();
		LogHelper.initLogger("BukkitWebby", "Minecraft");
	}

	@Override
	public void onEnable() {
		try {
			LogHelper.info("Loading...");
			// Check files and configuration
			final int localPort = pluginConfiguration.getInt("webby.localPort", 25564);
			localCommThread = new RTKModuleSocket(localPort, this);
			localCommThread.start();
			final boolean success = this.initPlugin();
			if(success) {
				LogHelper.info("Successfully loaded !");
			} else {
				LogHelper.error("Plugin is OFF. See warnings for more informations", new RuntimeException("Failed to initialize plugin"));
			}
		} catch(final Exception e) {
			LogHelper.error("An error occured while starting the plugin", e);
		}
	}

	private boolean initPlugin() {
		if(pluginConfiguration.getValues(false).size() == 0) {
			LogHelper.warn("No config.yml found in the plugin directory. This plugin won't be loaded");
			return false;
		}
		if(!new File("plugins").exists()) {
			LogHelper.warn("No Bukkit plugins directory found. This plugin won't be loaded");
			return false;
		}
		if(!new File("plugins/" + description.getName() + "/html").exists()) {
			LogHelper.warn("No html directory found. Installation must have been done wrong. This plugin won't be loaded");
			return false;
		}
		return true;
	}

	@Override
	public void onDisable() {
		localCommThread.closeSocket();
		LogHelper.info("Webby plugin disabled. The Webserver will still be ON.");
	}

	public RTKModuleSocket getLocalCommThread() {
		return localCommThread;
	}
}
