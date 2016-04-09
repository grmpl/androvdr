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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
	private static transient Logger logger = LoggerFactory.getLogger(DBHelper.class);
	
	public static final String DATABASE_NAME = "AndroVDR.db";
	public static final int DATABASE_VERSION = 11;
	
	public DBHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(RecordingIdsTable.SQL_CREATE);
		db.execSQL(DevicesTable.SQL_CREATE);
		db.execSQL(ChannelsTable.SQL_CREATE);
		db.execSQL(ChannelsTable.SQL_CREATE_INDEX);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		switch (oldVersion) {
		case 1:
			logger.debug("Upgrading database from version 1 to 2");
			db.execSQL("ALTER TABLE " + DevicesTable.TABLE_NAME 
					+ " ADD COLUMN " + DevicesTable.TIMEOUT + " INT DEFAULT 7500");
		case 2:
			logger.debug("Upgrading database from version 2 to 3");
			db.execSQL("ALTER TABLE " + DevicesTable.TABLE_NAME 
					+ " ADD COLUMN " + DevicesTable.SSHKEY + " BLOB DEFAULT NULL");
		case 3:
			logger.debug("Upgrading database from version 3 to 4");
			// --- change to svdrpl4j ---
			db.execSQL(RecordingIdsTable.SQL_CLEAR);
			// --- livetv ---
			db.execSQL("ALTER TABLE " + DevicesTable.TABLE_NAME 
					+ " ADD COLUMN " + DevicesTable.STREAMINGPORT + " INT DEFAULT 3000");
		case 4:
			logger.debug("Upgrading database from version 4 to 5");
			db.execSQL(ChannelsTable.SQL_CREATE);
		case 5:
			logger.debug("Upgrading database from version 5 to 6");
			db.execSQL(ChannelsTable.SQL_DROP);
			db.execSQL(ChannelsTable.SQL_CREATE);
			db.execSQL(ChannelsTable.SQL_CREATE_INDEX);
		case 6:
			logger.debug("Upgrading database from version 6 to 7");
			db.execSQL("ALTER TABLE " + DevicesTable.TABLE_NAME
					+ " ADD COLUMN " + DevicesTable.BROADCASTADDRESS + " STRING DEFAULT '255.255.255.255'");
		case 7:
			logger.debug("Upgrading database from version 7 to 8");
			db.execSQL("ALTER TABLE " + DevicesTable.TABLE_NAME
					+ " ADD COLUMN " + DevicesTable.EXTREMUX + " STRING DEFAULT 'false'");
			db.execSQL("ALTER TABLE " + DevicesTable.TABLE_NAME
					+ " ADD COLUMN " + DevicesTable.EXTREMUX_PARAM + " STRING");
			db.execSQL("ALTER TABLE " + DevicesTable.TABLE_NAME
					+ " ADD COLUMN " + DevicesTable.REMOTE_STREAMINGPORT + " INT DEFAULT 3000");
		case 8:
			logger.debug("Upgrading database from version 8 to 9");
			db.execSQL("ALTER TABLE " + DevicesTable.TABLE_NAME
					+ " ADD COLUMN " + DevicesTable.EXTREMUX_COMMAND + " STRING DEFAULT 'EXT'");
		case 9:
			logger.debug("Upgrading database from version 9 to 10");
			db.execSQL("UPDATE " + DevicesTable.TABLE_NAME + " SET "
					+ DevicesTable.REMOTE_STREAMINGPORT + "=35551");
		case 10:
			logger.debug("Upgrading database from version 10 to 11");
			db.execSQL("ALTER TABLE " + DevicesTable.TABLE_NAME
					+ " ADD COLUMN " + DevicesTable.VDRADMIN + " STRING DEFAULT 'false'");
			db.execSQL("ALTER TABLE " + DevicesTable.TABLE_NAME
					+ " ADD COLUMN " + DevicesTable.VDRADMIN_PORT + " INT DEFAULT 8001");
			db.execSQL("ALTER TABLE " + DevicesTable.TABLE_NAME
					+ " ADD COLUMN " + DevicesTable.REMOTE_VDRADMIN_PORT + " INT DEFAULT 35552");
			db.execSQL("ALTER TABLE " + DevicesTable.TABLE_NAME
					+ " ADD COLUMN " + DevicesTable.GENERALSTREAMING + " STRING DEFAULT 'false'");
			db.execSQL("ALTER TABLE " + DevicesTable.TABLE_NAME
					+ " ADD COLUMN " + DevicesTable.GENERALSTREAMING_URL + " STRING");

			break;
		default:
			logger.debug("Upgrading database from version {} to {}", oldVersion, newVersion);
			db.execSQL(RecordingIdsTable.SQL_DROP);
			db.execSQL(DevicesTable.SQL_DROP);
			db.execSQL(ChannelsTable.SQL_DROP);
			onCreate(db);
		}
	}
}
