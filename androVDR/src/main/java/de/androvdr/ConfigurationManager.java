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

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.media.AudioManager;
import android.os.PowerManager;
import android.os.Vibrator;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import de.androvdr.devices.Devices;

public class ConfigurationManager implements OnSharedPreferenceChangeListener {
	private static transient Logger logger = LoggerFactory.getLogger(ConfigurationManager.class);
	private static final long VIBRATION_LENGTH = 45;

	private static ConfigurationManager sInstance;
	
	private Activity mActivity;
	
	private Devices mDevices;
	private boolean mDisableKeyguard;
	private boolean mUseSwipeToFinish;
	private boolean mUseVibrator;
	private boolean mDisableStandby;
	private boolean mUseVolumeVDR;
	
	private KeyguardLock mKeyguardLock = null;
	private Vibrator mVibrator = null;
	private WakeLock mWakeLock = null;
	
	private ConfigurationManager(Activity activity) {
		mActivity = activity;

		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(activity);
	    sp.registerOnSharedPreferenceChangeListener(this);
	    mDisableKeyguard = sp.getBoolean("keyguard", false);
	    mDisableStandby = sp.getBoolean("standbyNo", false);
	    mUseSwipeToFinish = sp.getBoolean("swipeToFinish", false);
	    
	    mDevices = Devices.getInstance();
	    mUseVolumeVDR = mDevices.volumeControl();
	    
	    mUseVibrator = sp.getBoolean("hapticFeedback", false);
	    
	    if (mDisableStandby)
	    	disableStandby();
	    if (mUseVibrator)
	    	enableVibrator();
	    if (mUseVolumeVDR)
			enableVolumeControl();
	}
	
	public static ConfigurationManager getInstance(Activity activity) {
		if (sInstance == null)
			sInstance = new ConfigurationManager(activity);
		else
			sInstance.mActivity = activity;
		return sInstance;
	}

	public void disableKeyguard() {
		if (! mDisableKeyguard)
			return;
		
		if (mKeyguardLock == null) {
	    	KeyguardManager keyguardManager = (KeyguardManager) mActivity.getSystemService(Context.KEYGUARD_SERVICE);
	    	mKeyguardLock = keyguardManager.newKeyguardLock(mActivity.getString(R.string.app_name));
		}
		mKeyguardLock.disableKeyguard();
		logger.trace("keyguard disabled");
	}

	public void disableStandby() {
		if (mWakeLock == null) {
			PowerManager pm = (PowerManager) mActivity.getSystemService(Context.POWER_SERVICE);  
	    	mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "DoNotGoStandby");
		}
    	logger.trace("standby disabled");
	}
	
	public void disableVibrator() {
		mVibrator = null;
		logger.trace("vibrator disabled");
	}

	public void disableVolumeControl() {
		mActivity.setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);
	}
	
	public boolean doSwipe(int direction) {
		return (mUseSwipeToFinish && (direction == SimpleGestureFilter.SWIPE_RIGHT));
	}
	
	public void enableKeyguard() {
		if (mKeyguardLock != null) {
			mKeyguardLock.reenableKeyguard();
			mKeyguardLock = null;
		}
		logger.trace("keyguard enabled");
	}

	public void enableStandby() {
		if (mWakeLock != null) {
			if (mWakeLock.isHeld())
				mWakeLock.release();
			mWakeLock = null;
		}
    	logger.trace("standby enabled");
	}

	public void enableVibrator() {
		if (mVibrator == null) {
	    	mVibrator = (Vibrator) mActivity.getSystemService(Context.VIBRATOR_SERVICE);
		}
    	logger.trace("vibrator enabled");
	}
	
	public void enableVolumeControl() {
		mActivity.setVolumeControlStream(AudioManager.STREAM_MUSIC);
	}
	
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	if (!mUseVolumeVDR) {
    		return false;
    	}
    	switch (keyCode) {
    	case KeyEvent.KEYCODE_VOLUME_DOWN:
    		mDevices.volumeDown();
			return true;
    	case KeyEvent.KEYCODE_VOLUME_UP:
    		mDevices.volumeUp();
			return true;
    	default:
    		return false;
    	}
    }
    
	public void onPause() {
		if (mWakeLock != null)
			mWakeLock.release();
		
		mDevices.onPause();
	}
	
	public void onResume() {
		if (mWakeLock != null)
			mWakeLock.acquire();
		
		if (mUseVolumeVDR)
			enableVolumeControl();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sp,	String key) {
		if (key.equals("standbyNo")) {
			mDisableStandby = sp.getBoolean(key, false);
			if (mDisableStandby)
				disableStandby();
			else
				enableStandby();
		} else if (key.equals("hapticFeedback")) {
			mUseVibrator = sp.getBoolean(key, false);
			if (mUseVibrator)
				enableVibrator();
			else
				disableVibrator();
		} else if (key.equals("keyguard")) {
			mDisableKeyguard = sp.getBoolean(key, false);
			if (mDisableKeyguard)
				disableKeyguard();
			else
				enableKeyguard();
		} else if (key.equals("swipeToFinish")) {
			mUseSwipeToFinish = sp.getBoolean(key, false);
		} else {
			mUseVolumeVDR = mDevices.volumeControl();
			if (mUseVolumeVDR)
				enableVolumeControl();
			else
				disableVolumeControl();
		}
	}

	public void vibrate() {
		if (mVibrator != null) {
			mVibrator.vibrate(VIBRATION_LENGTH);
		}
	}
}
