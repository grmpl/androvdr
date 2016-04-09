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

import android.os.Parcel;
import android.os.Parcelable;

public class EpgSearch implements Parcelable {
	public String search;
	public boolean inTitle = true;
	public boolean inSubtitle = true;
	public boolean inDescription = false;

	public static final Parcelable.Creator<EpgSearch> CREATOR = new Parcelable.Creator<EpgSearch>() {
		public EpgSearch createFromParcel(Parcel in) {
			return new EpgSearch(in);
		}

		public EpgSearch[] newArray(int size) {
			return new EpgSearch[size];
		}
	};
	
	public EpgSearch() { }
	
	public EpgSearch(Parcel in) {
		search = in.readString();
		inTitle = in.readInt() == 1;
		inSubtitle = in.readInt() == 1;
		inDescription = in.readInt() == 1;
	}
	
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(search);
		dest.writeInt(inTitle ? 1 : 0);
		dest.writeInt(inSubtitle ? 1 : 0);
		dest.writeInt(inDescription ? 1 : 0);
	}

}
