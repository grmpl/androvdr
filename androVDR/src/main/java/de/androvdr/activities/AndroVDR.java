/*
 * Copyright (c) 2009-2011 by androvdr <androvdr@googlemail.com>
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

package de.androvdr.activities;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.viewpagerindicator.CirclePageIndicator;
import com.viewpagerindicator.LinePageIndicator;

import de.androvdr.Channel;
import de.androvdr.Channels;
import de.androvdr.ConfigurationManager;
import de.androvdr.Dialogs;
import de.androvdr.GesturesFind;
import de.androvdr.IFileLogger;
import de.androvdr.PortForwarding;
import de.androvdr.Preferences;
import de.androvdr.R;
import de.androvdr.devices.Devices;
import de.androvdr.devices.IActuator;
import de.androvdr.devices.OnChangeListener;
import de.androvdr.devices.OnSensorChangeListener;
import de.androvdr.devices.VdrDevice;
import de.androvdr.fragments.RemoteFragment;
import de.androvdr.fragments.RemoteTabletFragment;
import de.androvdr.svdrp.VDRConnection;
import de.androvdr.widget.TextResizeButton;

public class AndroVDR extends AbstractFragmentActivity implements OnChangeListener, 
		OnSharedPreferenceChangeListener {
    
	private static final int PREFERENCEACTIVITY_ID = 1;
	private static final int ACTIVITY_ID = 2;
	private static final int DEVICEPREFERENCEACTIVITY_ID = 3;
	
	private static transient Logger logger = LoggerFactory.getLogger(AndroVDR.class);
	
	private static final int CLOSE_CONNECTION = 0;
	private static final int CLOSE_CONNECTION_PORTFORWARDING = 1;
	private static final int CLOSE_CONNECTION_TERMINATE = 2;
	
	private static final int SENSOR_CHANNEL = 1;
	
	public static PortForwarding portForwarding = null;
	
	private Devices mDevices;
	private String mTitle;
	private String mTitleChannelName;
	private WatchPortForwadingThread mWatchPortForwardingThread;
	private boolean mLayoutChanged = false;
	
	private PagerAdapter mPagerAdapter;
	private ViewPager mPager;
	
	public static int VDR = 4;
	public static int VDR_NUMERICS = 5;

	public static final String MSG_RESULT = "result";
	
	private Handler mResultHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Bundle bundle = msg.getData();
			if (bundle != null) {
				String result = bundle.getString(MSG_RESULT);
				if (result != "")
					Toast.makeText(AndroVDR.this, result, Toast.LENGTH_LONG).show();
			}
		}
	};
	
	private Handler mSensorHandler = new Handler() {
		public void handleMessage(Message msg) {
			int type = msg.what;
			String result = msg.getData().getString(MSG_RESULT);
			
			switch (type) {
			case SENSOR_CHANNEL:
				new ChannelViewUpdater().execute(result);
				break;
			}
		};
	};
	
	private Handler mUpdateTitleHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			updateTitle();
		};
	};
	
	private void initLogging(SharedPreferences sp) {
		if (logger instanceof IFileLogger) {
			IFileLogger filelogger = (IFileLogger) logger;
	    	int loglevel = Integer.parseInt(sp.getString("logLevel", "0"));
	    	if (loglevel < 2)
	    		filelogger.initLogFile(null, false);
	    	else
	    		filelogger.initLogFile(Preferences.getLogFileName(), (loglevel == 3));
	    }
	}

	public void initWorkspaceView(Bundle savedInstanceState) {
		if (! Preferences.alternateLayout)
			setTheme(R.style.Theme_Original);
		
		logger.debug("Model: {}", Build.MODEL);
		logger.debug("SDK Version: {}", Build.VERSION.SDK_INT);

		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		logger.debug("Width: {}", metrics.widthPixels);
		logger.debug("Height: {}", metrics.heightPixels);
		logger.debug("Density: {}", metrics.densityDpi);
		
	    Configuration conf = getResources().getConfiguration();
	    boolean screenSmall = ((conf.screenLayout & Configuration.SCREENLAYOUT_SIZE_SMALL) == Configuration.SCREENLAYOUT_SIZE_SMALL);
	    boolean screenNormal = ((conf.screenLayout & Configuration.SCREENLAYOUT_SIZE_NORMAL) == Configuration.SCREENLAYOUT_SIZE_NORMAL);
	    boolean screenLong = ((conf.screenLayout & Configuration.SCREENLAYOUT_LONG_YES) == Configuration.SCREENLAYOUT_LONG_YES);
	    boolean screenLarge = ((conf.screenLayout & Configuration.SCREENLAYOUT_SIZE_LARGE) == Configuration.SCREENLAYOUT_SIZE_LARGE);
	    boolean screenXLarge = ((conf.screenLayout & Configuration.SCREENLAYOUT_SIZE_XLARGE) == Configuration.SCREENLAYOUT_SIZE_XLARGE);

	    logger.debug("Screen Small: {}", screenSmall);
	    logger.debug("Screen Normal: {}", screenNormal);
	    logger.debug("Screen Long: {}", screenLong);
	    logger.debug("Screen Large: {}", screenLarge);
	    logger.debug("Screen XLarge: {}", screenXLarge);

	    if (screenSmall)
	    	Preferences.screenSize = Preferences.SCREENSIZE_SMALL;
	    if (screenNormal)
	    	Preferences.screenSize = Preferences.SCREENSIZE_NORMAL;
	    if (screenLong)
	    	Preferences.screenSize = Preferences.SCREENSIZE_LONG;
	    if (screenLarge)
	    	Preferences.screenSize = Preferences.SCREENSIZE_LARGE;
	    if (screenXLarge)
	    	Preferences.screenSize = Preferences.SCREENSIZE_XLARGE;
	    logger.trace("Screen size: {}", Preferences.screenSize);
	    
	    // --- init default text size for buttons ---
	    TextResizeButton.resetDefaultTextSize();
	    TextResizeButton rb = (TextResizeButton) LayoutInflater.from(this).inflate(R.layout.reference_button, null);
	    if ((Preferences.screenSize >= Preferences.SCREENSIZE_LARGE) 
	    		&& (metrics.widthPixels > metrics.heightPixels))
	    	rb.setTextSizeAsDefault(metrics.widthPixels / 2 / 5, 100);
	    else
	    	rb.setTextSizeAsDefault(Math.min(metrics.widthPixels, metrics.heightPixels) / 4, 100);
	    logger.debug("Default TextSize (px): {}", rb.getTextSize());

	    // --- landscape mode only on large displays ---
	    if (Preferences.screenSize < Preferences.SCREENSIZE_LARGE) {
	    	logger.trace("setting SCREEN_ORIENTATION_PORTRAIT");
	    	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	    }
	    
	    setContentView(R.layout.remote_pager);
	    mPagerAdapter = new PagerAdapter(getSupportFragmentManager());
	    mPager = (ViewPager) findViewById(R.id.pager);
	    mPager.setAdapter(mPagerAdapter);
	    
	    LinePageIndicator indicator = (LinePageIndicator) findViewById(R.id.titles);
	    if (mPagerAdapter.getCount() > 1) {
		    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		    int last = sp.getInt("remote_last_page", 0);
		    if (last < mPagerAdapter.getCount())
		    	indicator.setViewPager(mPager, last);
		    else
		    	indicator.setViewPager(mPager);
	    } else { 
	    	indicator.setVisibility(View.GONE);
	    }
	        
	    // --- show current channel in status bar ---
	    if (Preferences.screenSize < Preferences.SCREENSIZE_XLARGE)
			mDevices.addOnSensorChangeListener("VDR.channel", 1,
				new OnSensorChangeListener() {
					@Override
					public void onChange(String result) {
						logger.trace("Channel: {}", result);
						Message msg = Message.obtain(mSensorHandler,
								SENSOR_CHANNEL);
						Bundle bundle = new Bundle();
						bundle.putString(MSG_RESULT, result);
						msg.setData(bundle);
						msg.sendToTarget();
					}
			});

	    mDevices.startSensorUpdater(0);
	}

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	switch (requestCode) {
    	case PREFERENCEACTIVITY_ID:
    		if (mLayoutChanged) {
    			finish();
    			startActivity(getIntent());
    		}
    		break;
    	case DEVICEPREFERENCEACTIVITY_ID:
    		mDevices.initDevices();
    		break;
    	}
    }
    
	@Override
	public void onChange() {
		removeDialog(Dialogs.SWITCH_VDR);
	}

	/** Called when the activity is first created. **/
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Preferences.init(false);
		mDevices = Devices.getInstance();

		mConfigurationManager = ConfigurationManager.getInstance(this);
		mConfigurationManager.disableKeyguard();

		if (Build.VERSION.SDK_INT < 14)
			mTitle = getTitle().toString();
		else
			mTitle = "";
        mTitleChannelName = "";
        
		mDevices.setParentActivity(this);
		mDevices.setResultHandler(mResultHandler);
		mDevices.setOnDeviceConfigurationChangedListener(this);
		
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
	    sp.registerOnSharedPreferenceChangeListener(this);
	    sp.registerOnSharedPreferenceChangeListener(mDevices);

	    initLogging(sp);
		initWorkspaceView(savedInstanceState);
		
		mWatchPortForwardingThread = new WatchPortForwadingThread();
		mWatchPortForwardingThread.start();
    }

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = null;
		switch (id) {
		case Dialogs.SWITCH_VDR:
			final ArrayList<VdrDevice> vdrs = mDevices.getVdrs();
			final ArrayList<String> items = new ArrayList<String>();
			
			Comparator<VdrDevice> comparator = new Comparator<VdrDevice>() {
				@Override
				public int compare(VdrDevice a, VdrDevice b) {
					return a.getName().compareTo(b.getName());
				}
			};
			Collections.sort(vdrs, comparator);
			
			long current = -1;
			for (int i = 0; i < vdrs.size(); i++) {
				VdrDevice vdr = vdrs.get(i);
				if ((Preferences.getVdr() != null)
						&& (vdr.getId() == Preferences.getVdr().getId()))
					current = i;
				items.add(vdr.getName());
			}
			
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.main_select_vdr)
			.setNegativeButton(R.string.cancel_action, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			})
			.setSingleChoiceItems(items.toArray(new CharSequence[items.size()]), (int) current, new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int item) {
			        IActuator ac = mDevices.getVdr(vdrs.get(item).getId());
		        	Preferences.setVdr(ac.getId());
			        dialog.dismiss();
			    }
			});
			dialog = builder.create();
			break;
		case Dialogs.CONFIG_VDR:
			builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.dialog_novdr_title)
			.setMessage(R.string.dialog_novdr_summary)
			.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			})
			.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Intent intent = new Intent(AndroVDR.this, DevicePreferencesActivity.class);
					intent.putExtra("deviceid", -1);
					startActivityForResult(intent, DEVICEPREFERENCEACTIVITY_ID);
				}
			});
			dialog = builder.create();
			break;
		default:
			dialog = null;
		}
		return dialog;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_option_menu, menu);
		return true;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();	
		mConfigurationManager.enableKeyguard();
		mDevices.clearOnSensorChangeListeners();
		new CloseConnectionTask().execute(CLOSE_CONNECTION_PORTFORWARDING);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		VdrDevice vdr;
		String url;

		switch (item.getItemId()) {
		case R.id.androvdr_exit:
			new CloseConnectionTask().execute(CLOSE_CONNECTION_TERMINATE);
			return true;
		case R.id.androvdr_about:
			startActivity(new Intent(this, AboutActivity.class));
			return true;
		case R.id.androvdr_switch_vdr:
			showDialog(Dialogs.SWITCH_VDR);
			return true;
		case R.id.androvdr_settings:
			startActivityForResult(new Intent(this, PreferencesActivity.class), PREFERENCEACTIVITY_ID);
			return true;
		case R.id.androvdr_gestures:
			Intent intent = new Intent(this, GesturesFind.class);
			startActivityForResult(intent, ACTIVITY_ID);
			return true;
		case R.id.androvdr_internet:
			togglePortforwarding();
			return true;
		case R.id.androvdr_vdradmin:
			vdr = Preferences.getVdr();
			if (Preferences.useInternet) {
				url = "http://localhost:" + vdr.remote_vdradmin_port;
			} else {
				url = "http://" + vdr.getIP() + ":" + vdr.vdradmin_port;
			}
			logger.debug("Streaming URL: {}", url);
			intent = new Intent(Intent.ACTION_VIEW);
			intent.setDataAndType(Uri.parse(url.toString()),"text/html");
			startActivityForResult(intent, 1);
			return true;
		case R.id.androvdr_generalstreaming:
			vdr = Preferences.getVdr();
			url = "http://" + vdr.getIP() + vdr.generalstreaming_url;
			logger.debug("Streaming URL: {}", url);
			intent = new Intent(Intent.ACTION_VIEW);
			intent.setDataAndType(Uri.parse(url.toString()),"video/*");
			startActivityForResult(intent, 1);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		mDevices.stopSensorUpdater();
		
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		Editor editor = sp.edit();
		editor.putInt("remote_last_page", mPager.getCurrentItem());
		editor.commit();
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		MenuItem mi = menu.findItem(R.id.androvdr_internet);
		if (Preferences.useInternet)
			mi.setTitle(R.string.main_forwarding_off);
		else
			mi.setTitle(R.string.main_forwarding_on);
		
		boolean vdrdefined = (Preferences.getVdr() != null);
		menu.findItem(R.id.androvdr_gestures).setEnabled(vdrdefined);
		menu.findItem(R.id.androvdr_internet).setEnabled(vdrdefined);
		menu.findItem(R.id.androvdr_switch_vdr).setEnabled(vdrdefined);
		menu.findItem(R.id.androvdr_vdradmin).setEnabled(vdrdefined);
		menu.findItem(R.id.androvdr_generalstreaming).setEnabled(vdrdefined);
		
		if (vdrdefined) {
			menu.findItem(R.id.androvdr_vdradmin).setVisible(Preferences.getVdr().vdradmin);
			menu.findItem(R.id.androvdr_generalstreaming).setVisible(Preferences.getVdr().generalstreaming);
		}
		return true;
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mDevices.startSensorUpdater(1);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (key.equals("alternateLayout") || key.equals("blackOnWhite"))
			mLayoutChanged = true;
		if (key.equals("currentVdrId")) {
			new CloseConnectionTask().execute(CLOSE_CONNECTION_PORTFORWARDING);
		}
		if (key.equals("logLevel"))
			initLogging(sharedPreferences);
		if (key.equals("useLogos"))
			Channels.clear();
	}

	@Override
	public void onSwipe(int direction) {
	}
	
    private Handler sshDialogHandler = new Handler() {
		@Override
        public void handleMessage(Message msg) {
			PortForwarding.sshDialogHandlerMessage(msg);
		}
	};
    
	private void togglePortforwarding() {
		new CloseConnectionTask().execute(CLOSE_CONNECTION);

		if (Preferences.useInternet == false) {
			String connectionState = "";
			final ConnectivityManager cm = (ConnectivityManager) this
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo[] info = cm.getAllNetworkInfo();
			for (int i = 0; i < info.length; i++) {
				if (info[i].isConnected() == true) { // dann wird wohl hoffentlich eine Verbindung klappen
					portForwarding = new PortForwarding(sshDialogHandler, this);
					return;
				} else { // sammel mer mal die Begruendung
					NetworkInfo.DetailedState state = info[i].getDetailedState();
					connectionState += info[i].getTypeName() + " State is "	+ state.name() + "\n";
				}
			}
			// kein Netzwerk vorhanden, also mach mer nix
			Toast.makeText(this, connectionState, Toast.LENGTH_LONG).show();
		} else {
			// Toast.makeText(Settings.this, "Nein", Toast.LENGTH_SHORT).show();
			if (portForwarding != null) {
				portForwarding.disconnect();
				portForwarding = null;
			}
		}
	}

	public void updateTitle() {
		String title;
		
		if (Build.VERSION.SDK_INT < 11) {
			if (mTitleChannelName.length() == 0 )
				title = mTitle;
			else 
				title = mTitle + " - " + mTitleChannelName;
		} else {
			title = mTitleChannelName;
		}
		
    	if (! isFinishing())
    		setTitle(title);
    }
    
	private class ChannelViewUpdater extends AsyncTask<String, Void, Channel> {

		@Override
		protected Channel doInBackground(String... params) {
			int channelNumber;
			StringBuilder channelName = new StringBuilder();
			
			if (params[0].equalsIgnoreCase("N/A"))
				return null;
			
			try {
				String[] sa = params[0].split(" ");
				channelNumber = Integer.parseInt(sa[0]);
				for (int i = 1; i < sa.length; i++)
					channelName.append(sa[i] + " ");
			} catch (Exception e) {
				logger.error("Couldn't parse channel: {}", e);
				return null;
			}

			Channel channel = null;
			try {
				channel = new Channel(channelNumber, channelName.toString().trim(), "");
			} catch (IOException e) {
			}
			return channel;
		}
		
		@Override
		protected void onPostExecute(Channel channel) {
			if (channel == null)
				mTitleChannelName = "";
			else {
				mTitleChannelName = channel.name;
			}
			mUpdateTitleHandler.sendEmptyMessage(0);
		}
	}
	
	private class CloseConnectionTask extends AsyncTask<Integer, Void, Void> {

		@Override
		protected Void doInBackground(Integer... params) {
			logger.trace("CloseConnection: {}", params[0]);
			logger.trace("  --> Closing");
			VDRConnection.close();

			if (params[0] >= CLOSE_CONNECTION_PORTFORWARDING) {
				logger.trace("  --> Disconnecting PortForward");
				if (portForwarding != null)
					portForwarding.disconnect();
			}
			
			if (params[0] >= CLOSE_CONNECTION_TERMINATE) {
				logger.trace("  --> Kill Process");
				android.os.Process.killProcess(android.os.Process.myPid());
			}
			return null;
		}
		
	}
	
	public static class PagerAdapter extends FragmentPagerAdapter {
		private ArrayList<Integer> mLayouts = new ArrayList<Integer>();
		
		public PagerAdapter(FragmentManager fm) {
			super(fm);
			
			if (Preferences.alternateLayout) {
				switch (Preferences.screenSize) {
				case Preferences.SCREENSIZE_SMALL:
					mLayouts.add(R.layout.remote_vdr_main);
					mLayouts.add(R.layout.remote_vdr_numerics);
					mLayouts.add(R.layout.remote_vdr_play);
					break;
				case Preferences.SCREENSIZE_NORMAL:
				case Preferences.SCREENSIZE_LONG:
					mLayouts.add(R.layout.remote_vdr_main);
					mLayouts.add(R.layout.remote_vdr_numerics);
					break;
				case Preferences.SCREENSIZE_LARGE:
				case Preferences.SCREENSIZE_XLARGE:
					mLayouts.add(R.layout.remote_vdr_main);
					break;
				default:
					mLayouts.add(R.layout.remote_vdr_main);
					mLayouts.add(R.layout.remote_vdr_numerics);
					break;
				}
			} else {
				mLayouts.add(R.layout.tab1);
				mLayouts.add(R.layout.tab2);
				mLayouts.add(R.layout.tab3);
			}

			File usertabFile = new File(Preferences.getUsertabFileName());
			if (usertabFile.exists())
				mLayouts.add(-99);
				
		}

		@Override
		public Fragment getItem(int position) {
			if (position == 0 && Preferences.screenSize == Preferences.SCREENSIZE_XLARGE)
				return new RemoteTabletFragment();
			else
				return RemoteFragment.newInstance(mLayouts.get(position));
		}

		@Override
		public int getCount() {
			return mLayouts.size();
		}

	}
	
    public class WatchPortForwadingThread extends Thread {
    	
    	public void run() {
			NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

			while (!isInterrupted()) {
    			synchronized (Preferences.useInternetSync) {
    				try {
						if (Preferences.useInternet) {
							int icon = R.drawable.stat_ic_menu_login;
							CharSequence tickerText = getString(R.string.notification_connected_ticker);
							long when = System.currentTimeMillis();
							Context context = getApplicationContext();
							CharSequence contentTitle = getString(R.string.app_name);
							CharSequence contentText = getString(R.string.notification_connected); 
							VdrDevice vdr = Preferences.getVdr();
							if (vdr != null)
								contentText = contentText + " " + vdr.remote_host; 
							Intent notificationIntent = new Intent(AndroVDR.this, AndroVDR.class);
							PendingIntent contentIntent = PendingIntent.getActivity(AndroVDR.this, 0, notificationIntent, 0);

							Notification notification = new Notification(icon, tickerText, when);
							notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
							notification.flags = Notification.FLAG_NO_CLEAR;
							
							notificationManager.notify(1, notification);
						} else {
							notificationManager.cancel(1);
						}
						Preferences.useInternetSync.wait();
					} catch (InterruptedException e) {
						logger.debug("WatchPortForwardingThread interrupted");
					}
				}
    		}
    	}
    }
}