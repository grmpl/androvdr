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

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Parcel;
import android.os.Parcelable;

public class Recording implements Parcelable, Comparable<Recording> {

	private static final SimpleDateFormat dateformatter = new SimpleDateFormat("dd.MM.yy HH:mm");
	private static final StringBuilder sb = new StringBuilder();
	
	private String mInfoId = null;
	
	public static final Parcelable.Creator<Recording> CREATOR = new Parcelable.Creator<Recording>() {
		public Recording createFromParcel(Parcel in) {
			return new Recording(in);
		}

		public Recording[] newArray(int size) {
			return new Recording[size];
		}
	};
	
	public static int VDRVersion = 10000;
	
	public String id;
	public int number;
	public long date;
	public boolean isNew = false;
	public String title;
	public ArrayList<String> folders = new ArrayList<String>();
	public String fullTitle;
	
	public Recording() {};
	
	public Recording(String vdrrecordinginfo) throws Exception {
		id = MD5.calculate(vdrrecordinginfo.substring(vdrrecordinginfo.indexOf(" ")).replace("*", " "));
		parse(vdrrecordinginfo);
	}
	
	public Recording(Parcel in) {
		id = in.readString();
		number = in.readInt();
		date = in.readLong();
		isNew = (in.readInt() == 0);
		title = in.readString();
		fullTitle = in.readString();
		in.readList(folders, null);
	}

	@Override
	public int compareTo(Recording recording) {
		return id.compareTo(recording.id);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public String getInfoId(SQLiteDatabase database) {
		if (mInfoId != null)
			return mInfoId;
		
		String result = null;
		Cursor cursor = null;
		try {
			cursor = database.query(
					RecordingIdsTable.TABLE_NAME,
					new String[] { RecordingIdsTable.INFO_ID },
					RecordingIdsTable.ID + " = ? AND " + RecordingIdsTable.VDR_ID + " = ?",
					new String[] { id, Long.toString(Preferences.getVdr().getId()) },
					null, null, null);
			if (cursor.getCount() == 1) {
				cursor.moveToFirst();
				result = cursor.getString(0);
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}		
		mInfoId = result;
		return result;
	}
	
	public boolean inFolder() {
		return (folders.size() > 0);
	}
	
	private void parse(String vdrrecordinginfo) throws Exception {
		String[] sa = vdrrecordinginfo.split(" ");
		sb.setLength(0);
		
		number = Integer.valueOf(sa[0]);
		
		// --- build date ---
		if (sa[2].endsWith("*")) {
			isNew = true;
			sa[2] = sa[2].substring(0, sa[2].indexOf("*"));
		}
		date = dateformatter.parse(sa[1] + " " + sa[2]).getTime() / 1000;
		
		if (VDRVersion >= 10721) {
			if (sa[3].endsWith("*"))
				isNew = true;
			
			// --- join title ---
			for (int i = 4; i < sa.length; i++) {
				sb.append(sa[i] + " ");
			}
		} else {
			// --- join title ---
			for (int i = 3; i < sa.length; i++) {
				sb.append(sa[i] + " ");
			}
		}
		
		fullTitle = sb.toString().trim();
		sa = sb.toString().split("~");
		if (sa.length > 1) {
			for (int i = 0; i < sa.length - 1; i++)
				folders.add(sa[i].trim());
			title = sa[sa.length - 1].trim();
		}
		else
		  title = sa[0].trim();
	}

	public void setInfoId(String id, SQLiteDatabase database) {
		if (id != null) {
			long sysTime = System.currentTimeMillis();
			String storedId = getInfoId(database);

			ContentValues values = new ContentValues();
			values.put(RecordingIdsTable.INFO_ID, id);
			values.put(RecordingIdsTable.UPDATED, sysTime);

			if (storedId == null) {
				values.put(RecordingIdsTable.ID, this.id);
				values.put(RecordingIdsTable.VDR_ID, Long.toString(Preferences.getVdr().getId()));
				database.insert(RecordingIdsTable.TABLE_NAME, null, values);
			} else if (id.compareTo(storedId) != 0) {
				database.update(RecordingIdsTable.TABLE_NAME, values,
						RecordingIdsTable.ID + " = ? AND " + RecordingIdsTable.VDR_ID + " = ?",
						new String[] { this.id, Long.toString(Preferences.getVdr().getId()) });
			}
		}
		mInfoId = id;
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(id);
		dest.writeInt(number);
		dest.writeLong(date);
		if (isNew)
			dest.writeInt(0);
		else 
			dest.writeInt(1);
		dest.writeString(title);
		dest.writeString(fullTitle);
		dest.writeList(folders);
	}
}
