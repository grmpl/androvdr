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

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.hampelratte.svdrp.Response;
import org.hampelratte.svdrp.commands.CHAN;
import org.hampelratte.svdrp.commands.LSTC;
import org.hampelratte.svdrp.parsers.ChannelParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;
import de.androvdr.devices.VdrDevice;
import de.androvdr.svdrp.VDRConnection;

public class Channels {
	private static transient Logger logger = LoggerFactory.getLogger(Channels.class);
	
	private static Boolean mIsInitialized = false;
	private static String mDefaults;
	private static ArrayList<Channel> mItems = new ArrayList<Channel>();
	
	public Channels(String defaults) throws IOException {
		if(mDefaults != null && defaults.compareTo(mDefaults) != 0)
			mIsInitialized = false;
		mDefaults = defaults;
		init();
	}
	
	public Channel addChannel(int kanal) throws IOException {
		Channel channel = null;
		boolean isTempChannel = (kanal == -1);
		
		Response response; 
		// determine the current channel
		if (kanal == -1) {
		    response = VDRConnection.send(new CHAN());
		    if(response.getCode() != 250) {
		        throw new IOException("Couldn't determine current channel number");
		    }
		    try {
				kanal = Integer.parseInt(response.getMessage().split(" ")[0]);
			} catch (NumberFormatException e) {
				logger.error("Couldn't determine channel number", e);
				throw new IOException("Couldn't determine channel number");
			}
		}
	    response = VDRConnection.send(new LSTC(kanal));
		if(response.getCode() != 250) {
		    throw new IOException(response.getCode() + " - " + response.getMessage().replaceAll("\n$", ""));
		}
		try {
			List<org.hampelratte.svdrp.responses.highlevel.Channel> channels = ChannelParser
					.parse(response.getMessage(), true);
			if (channels.size() > 0) {
				channel = new Channel(channels.get(0));
				channel.isTemp = isTempChannel;
				if (getChannel(channel.nr) == null)
					mItems.add(channel);
			} else {
				logger.error("Channel not found");
				throw new IOException("Channel not found");
			}
		} catch (IOException e) {
			logger.error("ungueltiger Kanaldatensatz",e);
			// faengt u A NumberformatExceptions ab
			throw new IOException("Couldn't parse channel details");
		} catch(ParseException pe) {
		    logger.error("Couldn't parse channel details", pe);
			throw new IOException("Couldn't parse channel details");
		}
		return channel;
	}

	public static void clear() {
		mIsInitialized = false;
		mItems.clear();
		
		VdrDevice vdr = Preferences.getVdr();
		if (vdr == null)
			return;
		
		DBHelper dbhelper = new DBHelper(AndroApplication.getAppContext());
		SQLiteDatabase db = null;
		try {
			db = dbhelper.getWritableDatabase();
			db.delete(ChannelsTable.TABLE_NAME, 
					ChannelsTable.VDR_ID + "=?", 
					new String[] { Long.toString(vdr.getId()) });
			logger.debug("channellist cleared");
		} catch (SQLiteException e) {
			logger.error("Couldn't clear channels table", e);
		} finally {
			if (db != null)
				db.close();
		}
	}
	
	public void deleteTempChannels() {
		for(ListIterator<Channel> itr = mItems.listIterator(); itr.hasNext();) {
			Channel channel = itr.next();
			if (channel.isTemp)
				itr.remove();
		}
	}
	
	public ArrayList<Channel> getItems() {
		deleteTempChannels();
		return mItems;
	}
	
	public Channel getChannel(int channel) {
		for(int i = 0; i < mItems.size(); i++)
			if(mItems.get(i).nr == channel)
				return mItems.get(i);
		return null;
	}
	
	public String getName(int channel) {
		for(int i = 0; i < mItems.size(); i++)
			if(mItems.get(i).nr == channel)
				return mItems.get(i).name;
		return "";
	}

	public void init() throws IOException {

		synchronized (mIsInitialized) {
			if (mIsInitialized)
				return;
			
			mItems.clear();
			mIsInitialized = false;

			// --- initialize from db ---
			VdrDevice vdr = Preferences.getVdr();
			DBHelper dbh = new DBHelper(AndroApplication.getAppContext());
			SQLiteDatabase db = null;
			Cursor cursor = null;
			try {
				db = dbh.getReadableDatabase();
				cursor = db.query(ChannelsTable.TABLE_NAME,
						new String[] { ChannelsTable.NUMBER, ChannelsTable.NAME, ChannelsTable.ZUSATZ},
						ChannelsTable.VDR_ID + "=?", 
						new String[] { Long.toString(vdr.getId()) },
						null, null, ChannelsTable.ID);
				
				while (cursor.moveToNext()) {
					Channel channel = new Channel(cursor.getInt(0), 
							cursor.getString(1), cursor.getString(2));
					mItems.add(channel);
				}			
				mIsInitialized = (mItems.size() > 0);
			} catch (SQLiteException e) {
				logger.error("Couldn't load channels from database", e);
			} finally {
				if (cursor != null)
					cursor.close();
				if (db != null)
					db.close();
			}
			
			// --- no need to request channellist from VDR ---
			if (mIsInitialized) {
				logger.debug("channels initialized from database");
				return;
			}
			
			String[] channelList = mDefaults.split(",");
			String[] bereich;
			
			db = null;
			try {
				db = dbh.getWritableDatabase();
				SQLiteStatement insert = db.compileStatement(
						"INSERT INTO " + ChannelsTable.TABLE_NAME +
						"(" + 
						  	ChannelsTable.VDR_ID + "," + ChannelsTable.NUMBER + "," +  
						  	ChannelsTable.NAME + "," + ChannelsTable.ZUSATZ +
						") " +
						"VALUES (?,?,?,?)");
				db.beginTransaction();

				int i = 0;
				for (i = 0; i < channelList.length; i++) { // Bereiche oder einzelne Kanaele
					try {
						if (channelList[i].contains("-")) { // hier sind die Bereiche
							bereich = channelList[i].split("-");
							int from = Integer.valueOf(bereich[0]);
							int to = Integer.valueOf(bereich[1]);
							for (int x = from; x <= to; x++) {
								addChannel(x);
							}
						} else { // einzelne Kanaele
							addChannel(Integer.valueOf(channelList[i]));
						}
					} catch (IOException e) {
						throw e;
					} catch (Exception e) {
						logger.error("invalid channellist: {}", mDefaults);
						continue;
					}
				}
				mIsInitialized = true;
				
				for (Channel channel : mItems) {
					insert.bindLong(1, vdr.getId());
					insert.bindLong(2, channel.nr);
					insert.bindString(3, channel.name);
					insert.bindString(4, channel.zusatz);
					insert.executeInsert();
				}
				logger.debug("channels stored into database");
				db.setTransactionSuccessful();
			} catch (IOException e) {
				logger.error("Couldn't initialize Channels", e);
				throw e;
			} catch (SQLiteException se) {
				logger.error("Couldn't store channels into database", se);
			} finally {
				if (db != null) {
					db.endTransaction();
					db.close();
				}
			}
		}
	}
	
	public static boolean isInitialized() {
		return mIsInitialized;
	}
}
