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

import java.util.Map;

/**
 * Custom YAML parser that can read the Bukkit and/or plugins configuration files.
 */
public class WebbyYAMLParser {

	@SuppressWarnings("unchecked")
	public static Object getObject(final String key, final Map<String, Object> ymlFile, final Object defValue) {
		Map<String, Object> curMap = ymlFile;
		final String[] subKeys = key.split("\\.");
		for(int i=0; i<subKeys.length; i++) {
			final String subKey = subKeys[i];
			if(!curMap.containsKey(subKey)) {
				LogHelper.warn("Missing key in YML : " + subKey + " from " + key + " can't be found in the file. Returning " + defValue);
				return defValue;
			} else if(i == subKeys.length-1){ //Last subKey -> return a value
				return curMap.get(subKey);
			} else { //Proceed with the next key
				curMap = (Map<String, Object>)curMap.get(subKey);
			}
		}
		return null;
	}

	public static String getString(final String key, final Map<String, Object> ymlFile, final Object defValue) {
		return String.valueOf(getObject(key, ymlFile, defValue));
	}

	public static Integer getInt(final String key, final Map<String, Object> ymlFile, final Object defValue) {
		return Integer.valueOf(getString(key, ymlFile, defValue));
	}
}
