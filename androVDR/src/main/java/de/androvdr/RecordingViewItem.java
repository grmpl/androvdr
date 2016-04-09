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

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

public class RecordingViewItem implements Parcelable {
	public static final Parcelable.Creator<RecordingViewItem> CREATOR = new Parcelable.Creator<RecordingViewItem>() {
		public RecordingViewItem createFromParcel(Parcel in) {
			return new RecordingViewItem(in);
		}

		public RecordingViewItem[] newArray(int size) {
			return new RecordingViewItem[size];
		}
	};
	
	public final boolean isFolder;
	public final String title;
	public String folderName;
	public ArrayList<RecordingViewItem> folderItems;
	public Recording recording;
	
	public RecordingViewItem(Recording recording) {
		this.isFolder = false;
		this.title = recording.title;
		this.recording = recording;
	};

	public RecordingViewItem(String folderName) {
		this.isFolder = true;
		this.title = folderName;
		this.folderName = folderName;
	}
	
	public RecordingViewItem(Parcel in) {
		isFolder = (in.readInt() == 0);
		title = in.readString();
		folderName = in.readString();
		
		Parcelable[] ra = in.readParcelableArray(RecordingViewItem.class.getClassLoader());
		if (ra != null && ra.length > 0) {
			folderItems = new ArrayList<RecordingViewItem>();
			for (int i = 0; i < ra.length; i++)
				folderItems.add((RecordingViewItem) ra[i]);
		}
		recording = in.readParcelable(Recording.class.getClassLoader());
	}
	
	public void add(Recording recording) throws Exception {
		if (! isFolder)
			throw new Exception("RecordingViewItem isn't a folder");
		
		if (folderItems == null)
			folderItems = new ArrayList<RecordingViewItem>();

		if (recording.inFolder()) {
			RecordingViewItem item = null;
			for (int i = 0; i < folderItems.size(); i++) {
				RecordingViewItem aItem = folderItems.get(i);
				if (aItem.isFolder && aItem.folderName.equals(recording.folders.get(0))) {
					item = aItem;
					break;
				}
			}
			if (item == null) {
				item = new RecordingViewItem(recording.folders.get(0));
				folderItems.add(item);
			}
			recording.folders.remove(0);
			item.add(recording);
		}
		else {
			folderItems.add(new RecordingViewItem(recording));
		}
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		if (isFolder)
			dest.writeInt(0);
		else
			dest.writeInt(1);
		dest.writeString(title);
		dest.writeString(folderName);

		if (folderItems != null) {
			RecordingViewItem[] ra = new RecordingViewItem[folderItems.size()];
			for (int i = 0; i < folderItems.size(); i++)
				ra[i] = folderItems.get(i);
			dest.writeParcelableArray(ra, flags);
		}
		else
			dest.writeParcelable(null, flags);

		dest.writeParcelable(recording, flags);
	}
}
