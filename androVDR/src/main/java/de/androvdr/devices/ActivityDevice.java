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

package de.androvdr.devices;

import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import de.androvdr.Preferences;
import de.androvdr.activities.ChannelsActivity;
import de.androvdr.activities.EpgdataActivity;
import de.androvdr.activities.EpgsdataActivity;
import de.androvdr.activities.RecordingInfoActivity;
import de.androvdr.activities.RecordingsActivity;
import de.androvdr.activities.TimersActivity;

public class ActivityDevice implements IActuator {
	private static String DISPLAYCLASSNAME = "Activity";

	@SuppressWarnings("unchecked")
	private static Class[] sVdrActivities = { ChannelsActivity.class, 
		EpgsdataActivity.class,	EpgdataActivity.class, 
		RecordingInfoActivity.class, RecordingsActivity.class,
		TimersActivity.class };
	
	private long mId;
	private String mLastError;
	private String mName;
	private Activity mParentActivity;
	
	@Override
	public String getLastError() {
		return mLastError;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean write(String command) {
		mLastError = null;
		Class<?> c = null;
		try {
			c = Class.forName(command);
			if (Preferences.getVdr() == null) {
				for (Class activity : sVdrActivities) {
					if (command.equals(activity.getName()))
						throw new IOException("No VDR defined");
				}
			}
			mParentActivity.startActivityForResult(new Intent(mParentActivity, c), 1);
		} catch (ClassNotFoundException e) {
			mLastError = e.toString();
			return false;
		} catch (IOException e) {
			mLastError = e.toString();
			return false;
		}
		return true;
	}

	@Override
	public void disconnect() {
	}

	@Override
	public ArrayList<String> getCommands() {
		return null;
	}

	@Override
	public String getDisplayClassName() {
		return DISPLAYCLASSNAME;
	}
	
	@Override
	public long getId() {
		return mId;
	}
	
	@Override
	public String getIP() {
		return null;
	}

	@Override
	public String getName() {
		return mName;
	}

	public Activity getParentActivity() {
		return mParentActivity;
	}
	
	@Override
	public String getPassword() {
		return null;
	}

	@Override
	public int getPort() {
		return 0;
	}

	@Override
	public String getUser() {
		return null;
	}
	
	public void setParentActivity(Activity activity) {
		mParentActivity = activity;
	}

	@Override
	public void setId(long id) {
		mId = id;
	}
	
	@Override
	public void setIP(String ip) {
	}

	@Override
	public void setName(String name) {
		mName = name;
	}

	@Override
	public void setPassword(String password) {
	}

	@Override
	public void setPort(int port) {
	}

	@Override
	public void setUser(String user) {
	}
}
