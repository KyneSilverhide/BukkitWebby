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

package com.kyne.webby.rtk.helper;

/**
 * Code inspired from MilkAdmin, by Sharkiller
 * http://forums.bukkit.org/threads/admn-info-web-milkadmin-v1-4-08-04-administration-backup-server-data-whitelist-banlist-1060.17249/
 */
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class WebbyFormatter extends Formatter {
	private final SimpleDateFormat dformat;

	public WebbyFormatter(){
		dformat = new SimpleDateFormat("HH:mm:ss");
	}

	@Override
	public String format(final LogRecord record) {
		final StringBuffer buf = new StringBuffer();
		buf.append(dformat.format(new Date(record.getMillis())))
		.append(" [").append(record.getLevel().getName()).append("] ")
		.append(this.formatMessage(record)).append('\n');
		if (record.getThrown() != null){
			buf.append('\t').append(record.getThrown().toString()).append('\n');
		}
		return buf.toString();
	}

}