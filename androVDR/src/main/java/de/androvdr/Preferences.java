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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Environment;
import android.preference.PreferenceManager;
import de.androvdr.devices.Devices;
import de.androvdr.devices.VdrDevice;

public class Preferences {
	private static transient Logger logger = LoggerFactory.getLogger(Preferences.class);
	
	private static final String CONFIG_ROOTDIR = "AndroVDR";
	private static final String CONFIG_LOGODIR = "logos";
	private static final String CONFIG_PLUGINDIR = "plugins";
	private static final String CONFIG_MACROFILE = "macros.xml";
	private static final String CONFIG_LOGFILE = "log.txt";
	private static final String CONFIG_GESTUREFILE = "gestures";
	private static final String CONFIG_SSH_KEY = "sshkey";
	private static final String CONFIG_SSH_KNOWN_HOSTS = "known_hosts";
	private static final String CONFIG_USERTABFILE = "mytab";
	
	public static final int SCREENSIZE_NOT_DEFINED = 0;
	public static final int SCREENSIZE_SMALL = 1;
	public static final int SCREENSIZE_NORMAL = 2;
	public static final int SCREENSIZE_LONG = 3;
	public static final int SCREENSIZE_LARGE = 4;
	public static final int SCREENSIZE_XLARGE = 5;
	
	private static VdrDevice sCurrentVdr = null;
	private static long sCurrentVdrId = -1;
	private static boolean sIsInitialized = false;
	
	public static String sFilesDir;
	
	public static boolean blackOnWhite;
	public static boolean useLogos;
	public static int textSizeOffset;
	public static boolean deleteRecordingIds = false;
	public static int logoBackgroundColor;
	public static boolean alternateLayout;
	public static boolean epgsearch_title;
	public static boolean epgsearch_subtitle;
	public static boolean epgsearch_description;
	public static int epgsearch_max;
	public static boolean showDiskStatus;
	public static boolean showCurrentChannel;
	public static boolean detailsLeft;
	public static int screenSize = SCREENSIZE_NOT_DEFINED;
	
	public static String dateformat = "dd.MM.";
	public static String dateformatLong = "dd.MM.yyyy HH:mm";
	public static String timeformat = "HH:mm";

	public static boolean useInternet = false; // legt fest, ob Portforwarding zum Einsatz kommt
	public static Boolean useInternetSync = false;
	public static boolean doRecordingIdCleanUp = true;
	
	public static String getExternalRootDirName() {
		return Environment.getExternalStorageDirectory() + "/" + CONFIG_ROOTDIR;
	}
	
	public static String getGestureFileName() {
		return getExternalRootDirName() + "/" + CONFIG_GESTUREFILE;
	}
	
	public static String getLogFileName() {
		return getExternalRootDirName() + "/" + CONFIG_LOGFILE;
	}
	
	public static String getLogoDirName() {
		return getExternalRootDirName() + "/" + CONFIG_LOGODIR;
	}
	
	public static String getMacroFileName() {
		return getPluginDirName() + "/" + CONFIG_MACROFILE;
	}
	
	public static String getPluginDirName() {
		return getExternalRootDirName() + "/" + CONFIG_PLUGINDIR;
	}
	
	public static String getSSHKeyFileName() {
		return sFilesDir + "/" + CONFIG_SSH_KEY;
	}
	
	public static String getSSHKnownHostsFileName() {
		return sFilesDir + "/" + CONFIG_SSH_KNOWN_HOSTS;
	}
	
	public static String getUsertabFileName() {
		return getExternalRootDirName() + "/" + CONFIG_USERTABFILE;
	}
	
	public static VdrDevice getVdr() {
		if (sCurrentVdr == null) {
			Context context = AndroApplication.getAppContext();
			Devices devices = Devices.getInstance();
			sCurrentVdr = devices.getVdr(sCurrentVdrId);
			if (sCurrentVdr == null)
				if (devices.getVdrs().size() >= 1)
					if ((sCurrentVdr = devices.getFirstVdr()) != null) {
						SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
						sCurrentVdrId = sCurrentVdr.getId();
						SharedPreferences.Editor editor = sp.edit();
						editor.putLong("currentVdrId", sCurrentVdrId);
						editor.commit();
					}
		}
		return sCurrentVdr;
	}
	
	public static void setVdr(long id) {
		if (id == sCurrentVdrId)
			return;
		sCurrentVdrId = id;
		sCurrentVdr = null;
		Context context = AndroApplication.getAppContext();
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = sp.edit();
		editor.putLong("currentVdrId", id);
		editor.commit();
	}
	
	public static void init(boolean force) {
		if (sIsInitialized && ! force)
			return;
		
		Context context = AndroApplication.getAppContext();
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		sFilesDir = context.getFilesDir().getAbsolutePath();
		
		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			File appDir = new File(Environment.getExternalStorageDirectory(), CONFIG_ROOTDIR);
			if (!appDir.exists())
				appDir.mkdirs();
		}
		
		if (logger instanceof IFileLogger) {
			IFileLogger filelogger = (IFileLogger) logger;
			int logging = Integer.parseInt(sp.getString("logLevel", "0"));
			if (logging == 0)
				filelogger.setLogLevel(0);
			else
				filelogger.setLogLevel(Integer.parseInt(sp.getString("slf4jLevel", "5")));
		}

		logger.trace("initializing");

		blackOnWhite = sp.getBoolean("blackOnWhite", false);
	    textSizeOffset = Integer.parseInt(sp.getString("textSizeOffset", "0"));
	    useLogos = sp.getBoolean("useLogos", false);
	    deleteRecordingIds = sp.getBoolean("deleteRecordingIds", false);
	    sCurrentVdrId = sp.getLong("currentVdrId", -1);
	    alternateLayout = sp.getBoolean("alternateLayout", true);
	    epgsearch_title = sp.getBoolean("epgsearch_title", true);
	    epgsearch_subtitle = sp.getBoolean("epgsearch_subtitle", true);
	    epgsearch_description = sp.getBoolean("epgsearch_description", false);
	    
	    if (sp.getString("epgsearch_max", "30").equals("")) {
	    	Editor editor = sp.edit();
	    	editor.putString("epgsearch_max", "30");
	    	editor.commit();
	    }
	    epgsearch_max = Integer.parseInt(sp.getString("epgsearch_max", "30"));
	    detailsLeft = sp.getBoolean("detailsLeft", false);
	    
	    Configuration conf = context.getResources().getConfiguration();
	    if (((conf.screenLayout & Configuration.SCREENLAYOUT_SIZE_SMALL) == Configuration.SCREENLAYOUT_SIZE_SMALL) 
	    	&& ((conf.screenLayout & Configuration.SCREENLAYOUT_SIZE_NORMAL) != Configuration.SCREENLAYOUT_SIZE_NORMAL)) {
	    	showDiskStatus = sp.getBoolean("showDiskStatus", false);
	    	showCurrentChannel = sp.getBoolean("showCurrentChannel", false);
	    }  else {
	    	showDiskStatus = sp.getBoolean("showDiskStatus", true);
	    	showCurrentChannel = sp.getBoolean("showCurrentChannel", true);
	    }

	    if (! sp.contains("showDiskStatus")) {
	    	Editor editor = sp.edit();
	    	editor.putBoolean("showDiskStatus", showDiskStatus);
	    	editor.commit();
	    }
	    
	    if (! sp.contains("showCurrentChannel")) {
	    	Editor editor = sp.edit();
	    	editor.putBoolean("showCurrentChannel", showCurrentChannel);
	    	editor.commit();
	    }
	    
	    String colorname = sp.getString("logoBackgroundColor", "none");
	    if (!colorname.equals("none"))
	    	logoBackgroundColor = Color.parseColor(colorname);
	    else
	    	logoBackgroundColor = 0;
	    
	    sCurrentVdr = null;
	    sIsInitialized = true;
	}
	
	public static void store() {
		Context context = AndroApplication.getAppContext();
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean("deleteRecordingIds", deleteRecordingIds);
        editor.commit();		
	}
}
