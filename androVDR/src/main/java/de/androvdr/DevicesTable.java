/*
 * Copyright (c) 2010-2011 by androvdr <androvdr@googlemail.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * For more information on the GPL, please go to:
 * http://www.gnu.org/copyleft/gpl.html
 */

package de.androvdr;

public class DevicesTable {
	public static final String TABLE_NAME = "devices";
	
	public static final String ID = "_id";
	public static final String CLASS = "class";
	public static final String NAME = "name";
	public static final String HOST = "host";
	public static final String PORT = "port";
	public static final String TIMEOUT = "timeout";
	public static final String USER = "user";
	public static final String PASSWORD = "password";
	public static final String MACADDRESS = "macaddress";
	public static final String BROADCASTADDRESS = "broadcastaddress";
	public static final String REMOTE_HOST = "remote_host";
	public static final String REMOTE_USER = "remote_user";
	public static final String REMOTE_PORT = "remote_port";
	public static final String REMOTE_LOCAL_PORT = "remote_local_port";
	public static final String REMOTE_TIMEOUT = "remote_timeout";
	public static final String REMOTE_STREAMINGPORT = "remote_streaming_port";
	public static final String CHANNELLIST = "channellist";
	public static final String EPGMAX = "epgmax";
	public static final String CHARACTERSET = "characterset";
	public static final String MARGIN_START = "margin_start";
	public static final String MARGIN_STOP = "margin_stop";
	public static final String VPS = "vps";
	public static final String SSHKEY = "sshkey";
	public static final String STREAMINGPORT = "streamingport";
	public static final String EXTREMUX = "extremux";
	public static final String EXTREMUX_PARAM = "extremux_param";
	public static final String EXTREMUX_COMMAND = "extremux_command";
	public static final String VDRADMIN = "vdradmin";
	public static final String VDRADMIN_PORT = "vdradmin_port";
	public static final String REMOTE_VDRADMIN_PORT = "remote_vdradmin_port";
	public static final String GENERALSTREAMING = "generalstreaming";
	public static final String GENERALSTREAMING_URL = "generalstreaming_url";
	
	public static final String[] ALL_COLUMNS = new String[] { ID, CLASS, NAME, HOST,
		PORT, USER,	PASSWORD, MACADDRESS, REMOTE_HOST, REMOTE_USER, REMOTE_PORT,
		REMOTE_LOCAL_PORT, REMOTE_TIMEOUT, CHANNELLIST, EPGMAX, CHARACTERSET,
		MARGIN_START, MARGIN_STOP, VPS, TIMEOUT, SSHKEY, STREAMINGPORT, BROADCASTADDRESS,
		EXTREMUX, EXTREMUX_PARAM, REMOTE_STREAMINGPORT, EXTREMUX_COMMAND,
		VDRADMIN, VDRADMIN_PORT, REMOTE_VDRADMIN_PORT, GENERALSTREAMING, GENERALSTREAMING_URL};
	
	public static final String SQL_CREATE =
		"CREATE TABLE " + TABLE_NAME + " ("
		+ ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
		+ CLASS + " STRING DEFAULT 'VDR',"
		+ NAME + " STRING,"
		+ HOST + " STRING,"
		+ PORT + " INTEGER,"
		+ USER + " STRING,"
		+ PASSWORD + " STRING,"
		+ MACADDRESS + " STRING,"
		+ REMOTE_HOST + " STRING,"
		+ REMOTE_USER + " STRING,"
		+ REMOTE_PORT + " INTEGER,"
		+ REMOTE_LOCAL_PORT + " INTEGER,"
		+ REMOTE_TIMEOUT + " INTEGER,"
		+ CHANNELLIST + " STRING,"
		+ EPGMAX + " INTEGER,"
		+ CHARACTERSET + " STRING,"
		+ MARGIN_START + " INT,"
		+ MARGIN_STOP + " INT,"
		+ VPS + " STRING DEFAULT 'false',"
		+ TIMEOUT + " INT DEFAULT 7500,"
		+ SSHKEY + " BLOB DEFAULT NULL,"
		+ STREAMINGPORT + " INT DEFAULT 3000,"
		+ BROADCASTADDRESS + " STRING DEFAULT '255.255.255.255',"
		+ EXTREMUX + " STRING DEFAULT 'false',"
		+ EXTREMUX_PARAM + " STRING,"
		+ REMOTE_STREAMINGPORT + " INT DEFAULT 35551,"
		+ EXTREMUX_COMMAND + " STRING DEFAULT 'EXT',"
		+ VDRADMIN + " STRING DEFAULT 'false',"
		+ VDRADMIN_PORT + " INTEGER DEFAULT 8001,"
		+ REMOTE_VDRADMIN_PORT + " INTEGER DEFAULT 35552,"
		+ GENERALSTREAMING + " STRING DEFAULT 'false',"
		+ GENERALSTREAMING_URL + " STRING"
		+ ")";
	
	public static final String SQL_DROP =
		"DROP TABLE IF EXISTS " + TABLE_NAME;
}
