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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;

import org.hampelratte.svdrp.Command;
import org.hampelratte.svdrp.Response;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import de.androvdr.Channels;
import de.androvdr.EventListenerList;
import de.androvdr.Preferences;
import de.androvdr.Wol;
import de.androvdr.svdrp.VDRConnection;

public class VdrDevice implements IActuator, ISensor, OnSharedPreferenceChangeListener {
	private static final String DISPLAYCLASSNAME = "VDR";

	private long mId;
	private String mName;
	private String mHost;
	private int mPort;
	private String mUser;
	private String mPassword;
	private HashSet<String> mChannelChangers = new HashSet<String>();
	private Hashtable<String, String> mCommands = new Hashtable<String, String>();
	private Hashtable<String, String> mCommandsCompat = new Hashtable<String, String>();
	private EventListenerList mListensers = new EventListenerList();
	private Hashtable<String, String> mSensors = new Hashtable<String, String>();
	private String mLastError;

	public int timeout;
	public String macaddress;
	public String broadcastaddress;
	public String remote_host;
	public String remote_user;
	public int remote_port;
	public int remote_local_port;
	public int remote_timeout;
	public int remote_streaming_port;
	public String channellist;
	public int epgmax;
	public String characterset;
	public boolean vps;
	public int margin_start;
	public int margin_stop;
	public String sshkey;
	public int streamingport;
	public boolean extremux;
	public String extremux_command;
	public String extremux_param;
	public boolean vdradmin;
	public int vdradmin_port;
	public int remote_vdradmin_port;
	public boolean generalstreaming;
	public String generalstreaming_url;
	
	public VdrDevice() {
		initCommands();
		initSensors();
	}

	@Override
	public void disconnect() {

	}

	public void addChannelChangedListener(OnChannelChangedListener listener) {
		mListensers.add(OnChannelChangedListener.class, listener);
	}
	
	@Override
	public ArrayList<String> getCommands() {
		ArrayList<String> result = new ArrayList<String>();
		Enumeration<String> e = mCommands.keys();
		while (e.hasMoreElements())
			result.add(e.nextElement());
		return result;
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
		return mHost;
	}

	@Override
	public String getLastError() {
		return mLastError;
	}

	@Override
	public String getName() {
		return mName;
	}

	@Override
	public ArrayList<String> getSensors() {
		ArrayList<String> result = new ArrayList<String>();
		Enumeration<String> e = mSensors.keys();
		while (e.hasMoreElements())
			result.add(e.nextElement());
		return result;
	}
	
	@Override
	public String getPassword() {
		return mPassword;
	}

	@Override
	public int getPort() {
		return mPort;
	}

	@Override
	public String getUser() {
		return mUser;
	}

	private void initCommands() {
    	mCommands.put("up", "HITK Up");
    	mCommands.put("down", "HITK Down");
    	mCommands.put("menu", "HITK Menu");
    	mCommands.put("ok", "HITK Ok");
    	mCommands.put("back", "HITK Back");
    	mCommands.put("left", "HITK Left");
    	mCommands.put("right", "HITK Right");
    	mCommands.put("red", "HITK Red");
    	mCommands.put("green", "HITK Green");
    	mCommands.put("yellow", "HITK Yellow");
    	mCommands.put("blue", "HITK Blue");
    	mCommands.put("0", "HITK 0");
    	mCommands.put("1", "HITK 1");
    	mCommands.put("2", "HITK 2");
    	mCommands.put("3", "HITK 3");
    	mCommands.put("4", "HITK 4");
    	mCommands.put("5", "HITK 5");
    	mCommands.put("6", "HITK 6");
    	mCommands.put("7", "HITK 7");
    	mCommands.put("8", "HITK 8");
    	mCommands.put("9", "HITK 9");
    	mCommands.put("info", "HITK Info");
    	mCommands.put("play", "HITK Play");
    	mCommands.put("pause", "HITK Pause");
    	mCommands.put("stop", "HITK Stop");
    	mCommands.put("record", "HITK Record");
    	mCommands.put("fastfwd", "HITK FastFwd");
    	mCommands.put("fastrew", "HITK FastRew");
    	mCommands.put("next", "HITK Next");
    	mCommands.put("prev", "HITK Prev");
    	mCommands.put("power", "HITK Power");   	
    	mCommands.put("chan_up", "CHAN +");
    	mCommands.put("chan_down", "CHAN -");
    	mCommands.put("prev_chan", "HITK PrevChannel");
    	mCommands.put("vol_up", "HITK Volume+");
    	mCommands.put("vol_down", "HITK Volume-");
    	mCommands.put("mute", "HITK Mute");
    	mCommands.put("audio", "HITK Audio");
    	mCommands.put("subtitles", "HITK Subtitles");
    	mCommands.put("schedule", "HITK Schedule");
    	mCommands.put("channels", "HITK Channels");
    	mCommands.put("timers", "HITK Timers");
    	mCommands.put("recordings", "HITK Recordings");
    	mCommands.put("setup", "HITK Setup");
    	mCommands.put("commands", "HITK Commands");
    	mCommands.put("user0", "HITK User0");
    	mCommands.put("user1", "HITK User1");
    	mCommands.put("user2", "HITK User2");
    	mCommands.put("user3", "HITK User3");
    	mCommands.put("user4", "HITK User4");
    	mCommands.put("user5", "HITK User5");
    	mCommands.put("user6", "HITK User6");
    	mCommands.put("user7", "HITK User7");
    	mCommands.put("user8", "HITK User8");
    	mCommands.put("user9", "HITK User9");
    	
    	mCommandsCompat.put("kanal", "HITK Channels");
    	mCommandsCompat.put("timer", "HITK Timers");
    	mCommandsCompat.put("programm", "HITK Schedule");
    	mCommandsCompat.put("off", "HITK Power");
    	mCommandsCompat.put("rec", "HITK Record");

    	mChannelChangers.add("up");
    	mChannelChangers.add("down");
    	mChannelChangers.add("0");
    	mChannelChangers.add("1");
    	mChannelChangers.add("2");
    	mChannelChangers.add("3");
    	mChannelChangers.add("4");
    	mChannelChangers.add("5");
    	mChannelChangers.add("6");
    	mChannelChangers.add("7");
    	mChannelChangers.add("8");
    	mChannelChangers.add("9");
    	mChannelChangers.add("chan_up");
    	mChannelChangers.add("chan_down");
    	mChannelChangers.add("prev_chan");
	}

	private void initSensors() {
		mSensors.put("disk", "STAT disk");
		mSensors.put("channel", "CHAN");
	}
	
	public static boolean isChannelSensor(String command) {
		return command.equalsIgnoreCase("vdr.channel");
	}

	private synchronized void notifyChannelChanged() {
		for (OnChannelChangedListener l : mListensers.getListeners(OnChannelChangedListener.class)) {
			l.onChannelChanged();
		}
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sp,
			String key) {
		boolean clearChannels = false;
		
		mName = sp.getString("name", "");
		mHost = sp.getString("host", "");
		mPort = sp.getInt("port", 6419);
		timeout = sp.getInt("timeout", 7500);
		macaddress = sp.getString("macaddress", "");
		broadcastaddress = sp.getString("broadcastaddress", "");
		remote_host = sp.getString("remote_host", "");
		remote_user = sp.getString("remote_user", "");
		remote_port = sp.getInt("remote_port", 22);
		remote_local_port = sp.getInt("remote_local_port", 35550);
		remote_timeout = sp.getInt("remote_timeout", 25000);
		remote_streaming_port = sp.getInt("remote_streaming_port", 35551);
		epgmax = sp.getInt("epgmax", 30);
		
		String newchannellist = sp.getString("channellist", "1-20,24");
		if (! newchannellist.equals(channellist))
			clearChannels = true;
		channellist = newchannellist;
		
		String newcharacterset = sp.getString("characterset", "ISO-8859-1");
		if (! newcharacterset.equals(characterset))
			clearChannels = true;
		characterset = newcharacterset;

		vps = sp.getString("vps", "false").equals("true");
		margin_start = sp.getInt("margin_start", 5);
		margin_stop	= sp.getInt("margin_stop", 10);
		sshkey = sp.getString("sshkey", null);
		streamingport = sp.getInt("streamingport", 3000);
		extremux = sp.getString("extremux", "false").equals("true");
		extremux_command = sp.getString("extremux_command", "EXT");
		extremux_param = sp.getString("extremux_param", null);
		vdradmin = sp.getString("vdradmin", "false").equals("true");
		vdradmin_port = sp.getInt("vdradmin_port", 8001);
		remote_vdradmin_port = sp.getInt("remote_vdradmin_port", 35552);
		generalstreaming = sp.getString("generalstreaming", "false").equals("true");
		generalstreaming_url = sp.getString("generalstreaming_url", "");
		
		VdrDevice currentVdr = Preferences.getVdr();
		if ((currentVdr != null) && (currentVdr.getId() == mId)) {
			if (clearChannels)
				Channels.clear();

			// --- network operations in main thread disallowed on HonyComb ---
			Thread thread = new Thread() {
				@Override
				public void run() {
					VDRConnection.close();
				};
			};
			thread.start();
		}
	}

	@Override
	public String read(String command) {

		final String vdrsensor = (String) mSensors.get(command);
		if (vdrsensor == null) {
			mLastError = "Unknown command: " + command;
			return null;
		}
		
		Response response = VDRConnection.send(new Command() {
			private static final long serialVersionUID = 1L;

			@Override
            public String toString() {
                return vdrsensor;
            }
            
            @Override
            public String getCommand() {
                return vdrsensor;
            }
        });
		
		if (response != null && response.getCode() != 250) {
			mLastError = response.getCode() + " - " + response.getMessage().replaceAll("\n$", "");
			return null;
		}
		
		return response.getMessage().replaceAll("\n$", "");
	}
	
	public void removeChannelChangedListener(OnChannelChangedListener listener) {
		mListensers.remove(OnChannelChangedListener.class, listener);
	}
	
	@Override
	public void setId(long id) {
		mId = id;
	}
	
	@Override
	public void setIP(String ip) {
		mHost = ip;
	}

	@Override
	public void setName(String name) {
		mName = name;
	}

	@Override
	public void setPassword(String password) {
		mPassword = password;
	}

	@Override
	public void setPort(int port) {
		mPort = port;
	}

	@Override
	public void setUser(String user) {
		mUser = user;
	}

	private String vdrcommand;
	@SuppressWarnings("serial")
	@Override
	public boolean write(String command) {
		if (command.equalsIgnoreCase("WOL")) {
			Wol wol = new Wol();
			if (! wol.sendMagicPaket(broadcastaddress, macaddress)) {
				mLastError = wol.lastError;
				return false;
			}
			return true;
		} else if(command.equalsIgnoreCase("WOLINTERNET")) {
			Wol wol = new Wol();
			if (! wol.sendMagicPaket(remote_host, macaddress)) {
				mLastError = wol.lastError;
				return false;
			}
			return true;
		}
		
		vdrcommand = (String) mCommands.get(command);
		if (vdrcommand == null)
			vdrcommand = (String) mCommandsCompat.get(command);
		
		if (vdrcommand == null) {
			mLastError = "Unknown command: " + command;
			return false;
		}
		
		Response response = VDRConnection.send(new Command() {
            @Override
            public String toString() {
                return vdrcommand;
            }
            
            @Override
            public String getCommand() {
                return vdrcommand;
            }
        });
		
		if (response != null && response.getCode() != 250) {
			mLastError = response.getCode() + " - " + response.getMessage().replaceAll("\n$", "");
			return false;
		}
		
		if (mChannelChangers.contains(command))
			notifyChannelChanged();
		
		return true;
	}
}
