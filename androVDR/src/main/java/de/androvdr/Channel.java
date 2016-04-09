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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import de.androvdr.Epgs.NoScheduleException;

public class Channel implements Comparable<Channel> {
	public static transient Logger logger = LoggerFactory.getLogger(Channel.class);

	public static final String TAG = "Channel";
	
	private static final long EPG_UPDATE_PERIOD = 1;
	
	public static final String logoDir = Preferences.getLogoDirName();
	
	public interface OnCurrentEpgChangedListener {
		public void OnCurrentEpgChanged(Channel channel);
	}
	
	private OnCurrentEpgChangedListener mChangedListener;
	private long mLastEpgUpdate = 0;
	private Epg mNext = null;
	private Epg mNow = null;
	private Epg mSearchResult = null;
	
	public String name;
	public String zusatz;
	public int nr;
	public final Bitmap logo;
	public boolean isTemp = false;

	public Epg viewEpg = null;		// used by EpdataController
	
	public Channel(int number, String name, String zusatz) throws IOException {
		this.nr = number;
		this.name = name;
		this.zusatz = zusatz;
		
        mNow = new Epg(nr, true);
        mNext = new Epg(nr, true);
        logo = initLogo();
	}
	
	public Channel(org.hampelratte.svdrp.responses.highlevel.Channel channel) throws IOException {
        name = channel.getName();
        zusatz = channel.getShortName();
        nr = channel.getChannelNumber();
        
        mNow = new Epg(nr, true);
        mNext = new Epg(nr, true);
        logo = initLogo();
    }

	public void cleanupEpg() {
		if(mNow != null && (System.currentTimeMillis() / 1000) > (mNow.startzeit + mNow.dauer)) {
			mNow = new Epg(nr, true);
			mNext = new Epg(nr, true);
		}
	}

	@Override
	public int compareTo(Channel another) {
		return ((Integer) nr).compareTo(another.nr);
	}
	
	public ArrayList<Epg> get(int count) throws IOException {
		try {
			return new Epgs(nr).get(count);
		} catch (NoScheduleException e) {
			return new ArrayList<Epg>();
		}
	}
	
	public ArrayList<Epg> getAll() throws IOException {
		return new Epgs(nr).getAll();
	}
	
	public Epg getAt(long time) throws IOException {
		return new Epgs(nr).getAt(time);
	}
	
	public long getMillisToNextUpdate() {
		if (mLastEpgUpdate == 0)
			return 0;
		else
			return ((mLastEpgUpdate + 1) * 60 * 1000) - System.currentTimeMillis() + 5000; 
	}
	
	public Epg getNext() {
		return mNext;
	}

	public Epg getNow() {
		return mNow;
	}

	public Epg getSearchResult() {
		return mSearchResult;
	}
	
	public boolean hasLogo() {
		return (logo != null);
	}
	
	private Bitmap initLogo() throws IOException {
		if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))
			return null;
		
		String filename = logoDir + "/" + name + ".png";
		Bitmap image = null;
		File imagefile = new File(filename);
		try {
			if (imagefile.exists()) {
				image = BitmapFactory.decodeFile(filename);
			}
		} catch (OutOfMemoryError e) {
			image = null;
			logger.error("initLogo {} out of memory", name);
			throw new IOException("Logo " + name + " too large");
		}
		return image;
	}
	
	public void setOnCurrentEpgChangedListener(OnCurrentEpgChangedListener listener) {
		mChangedListener = listener;
	}
	
	public void searchEpgAt(long time) throws IOException {
		mSearchResult = new Epgs(nr).getAt(time);
	}
	
	public void updateEpg(boolean next) throws IOException {
		long systemTime = (long) System.currentTimeMillis() / 60000;
		if ((mLastEpgUpdate == 0)
				|| (systemTime - mLastEpgUpdate >= EPG_UPDATE_PERIOD)) {
			if (mNow.isEmpty
					|| ((System.currentTimeMillis() / 1000) >= (mNow.startzeit + mNow.dauer - 60))) {
				Epg epg = new Epgs(nr).getNow();
				if (mChangedListener != null)
					mChangedListener.OnCurrentEpgChanged(this);
				mNow = epg;
				
				// --- refresh channel name from epg ---
				if (! mNow.isEmpty && ! name.equals(mNow.channelName))
					// TODO: update database
					name = mNow.channelName;
				
				if (next)
					mNext = new Epgs(nr).getNext();
			}
		}
		if (next && mNext.isEmpty) {
			mNext = new Epgs(nr).getNext();
		}
		mLastEpgUpdate = systemTime;
		mNow.calculatePercentDone();
	}
}