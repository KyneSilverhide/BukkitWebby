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

package com.kyne.webby.commons;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Basic helper to handle... basic logs :)
 */
public class LogHelper {

	private static Logger log;
	private static String logHeader;

	public static void initLogger(final String pluginName, final String loggerName) {
		logHeader = "[" + pluginName + "] ";
		log = Logger.getLogger(loggerName);
	}

	public static void info(final String message) {
		log.log(Level.INFO, logHeader + message);
	}

	public static void error(final String message, final Throwable t) {
		log.log(Level.SEVERE, logHeader + message, t);
		t.printStackTrace();
	}
	
	public static void error(final String message) {
		log.log(Level.SEVERE, logHeader + message);
	}

	public static void warn(final String message) {
		log.log(Level.WARNING, logHeader + message);
	}

	public static void debug(final String message) {
		log.log(Level.FINER, logHeader + message);
	}
}
