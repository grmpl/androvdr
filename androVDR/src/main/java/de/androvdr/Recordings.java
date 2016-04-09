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
import java.util.ArrayList;
import java.util.Collections;
import java.util.StringTokenizer;

import org.hampelratte.svdrp.Response;
import org.hampelratte.svdrp.commands.LSTR;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.androvdr.svdrp.VDRConnection;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;

public class Recordings {
	private static transient Logger logger = LoggerFactory.getLogger(Recordings.class);
	private static final DBHelper sDBHelper = new DBHelper(AndroApplication.getAppContext());
	
	private ArrayList<RecordingViewItem> mItems = new ArrayList<RecordingViewItem>();
	
	public Recordings() throws IOException {
		init();
	}
	
	public static void clearIds() {
		clearIds(Preferences.getVdr().getId());
	}
	
	public static void clearIds(long vdrid) {
		synchronized (sDBHelper) {
			try {
				SQLiteDatabase database = sDBHelper.getWritableDatabase();
				int i = database.delete(RecordingIdsTable.TABLE_NAME,
						RecordingIdsTable.VDR_ID + "=?", 
						new String[] { Long.toString(vdrid) });
				i++;
			} catch (SQLiteException e) {
				logger.error("clearIds", e);
			}
		}
	}
	
	public ArrayList<RecordingViewItem> getItems() {
		return mItems;
	}
	
	private RecordingViewItem getFolder(String name) {
		RecordingViewItem result = null;
		for (int i = 0; i < mItems.size(); i++) {
			RecordingViewItem item = mItems.get(i);
			if (item.isFolder && item.folderName.equals(name)) {
				result = mItems.get(i);
				break;
			}
		}
		return result;
	}
	
	private void init() throws IOException {
		Response response = VDRConnection.send(new LSTR());
		if(response.getCode() == 250) {
		    String message = response.getMessage();
		    StringTokenizer st = new StringTokenizer(message, "\n");
		    while(st.hasMoreTokens()) {
		        try {
		            Recording recording = new Recording(st.nextToken());
		            RecordingViewItem item;
		            if (recording.inFolder()) {
		                if ((item = getFolder(recording.folders.get(0))) == null) {
		                    item = new RecordingViewItem(recording.folders.get(0));
		                    mItems.add(item);
		                }
		                recording.folders.remove(0);
		                item.add(recording);
		            }
		            else {
		                item = new RecordingViewItem(recording);
		                mItems.add(item);
		            }
		        } catch (Exception e) {
		            logger.error("Invalid recording format", e);
		            continue;
		        }
		    } 
		} else {
		    throw new IOException(response.getCode() + " - " + response.getMessage().replaceAll("\n$", ""));
		}
	}
	
	public static void deleteUnusedIds(ArrayList<Recording> currentRecordings) {
		synchronized (sDBHelper) {
			Cursor cursor = null;
			Recording searchRecording = new Recording();
			Collections.sort(currentRecordings);
			try {
				SQLiteDatabase database = sDBHelper.getWritableDatabase();
				SQLiteStatement deleteStmt = database
						.compileStatement("DELETE FROM " + RecordingIdsTable.TABLE_NAME
								+ " WHERE " + RecordingIdsTable.ID + " = ? AND " + RecordingIdsTable.VDR_ID + " = ?");
				
				cursor = database.query(RecordingIdsTable.TABLE_NAME,
						new String[] { RecordingIdsTable.ID },
						RecordingIdsTable.VDR_ID + " = ?",
						new String[] { Long.toString(Preferences.getVdr().getId()) }, null,
						null, null);
				while (cursor.moveToNext()) {
					searchRecording.id = cursor.getString(0);
					if (Collections.binarySearch(currentRecordings, searchRecording) < 0) {
						deleteStmt.bindString(1, searchRecording.id);
						deleteStmt.execute();
					}
				}
			} catch (SQLiteException e) {
				logger.error("deleteUnusedIds", e);
			} finally {
				if (cursor != null)
					cursor.close();
			}
		}
	}
}
