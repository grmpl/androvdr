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

package de.androvdr.activities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
import de.androvdr.ActionBarHelper;
import de.androvdr.DevicesTable;
import de.androvdr.ListPreferenceValueHolder;
import de.androvdr.Preferences;
import de.androvdr.R;
import de.androvdr.activities.DevicePreferencesActivity.CursorPreferenceHack.Editor;
import de.androvdr.devices.Devices;
import de.androvdr.devices.IDevice;

public class DevicePreferencesActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	private static transient Logger logger = LoggerFactory.getLogger(DevicePreferencesActivity.class);
	
	private CursorPreferenceHack pref = null;
	private Devices mDevices;
	private long mId;
	private boolean mIsVDR = false;
	
	@Override
	public SharedPreferences getSharedPreferences(String name, int mode) {
		return this.pref;
	}

	private void initCharsetPref(final ListPreference charsetPref) {
		CharsetHolder charsetHolder = new CharsetHolder();
		charsetPref.setEntryValues(charsetHolder.getIds());
		charsetPref.setEntries(charsetHolder.getNames());
	}

	private void initClassPref(final ListPreference classPref) {
		ClassHolder classHolder = new ClassHolder();
		classPref.setEntryValues(classHolder.getIds());
		classPref.setEntries(classHolder.getNames());
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Preferences.init(false);
		if (Preferences.blackOnWhite && Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			setTheme(R.style.Theme_Light);
			getListView().setCacheColorHint(Color.TRANSPARENT);
			getWindow().setBackgroundDrawable(getResources().getDrawable(android.R.drawable.screen_background_light));
		}
		ActionBarHelper.setHomeButtonEnabled(this, true);
		
		mId = getIntent().getExtras().getInt("deviceid", -1);
		
		mDevices = Devices.getInstance();
		
		pref = new CursorPreferenceHack(mId);
		pref.registerOnSharedPreferenceChangeListener(this);

		IDevice device = mDevices.getDevice(mId);
		if (device instanceof OnSharedPreferenceChangeListener)
			pref.registerOnSharedPreferenceChangeListener((OnSharedPreferenceChangeListener) device);
		
		if (mId == -1)
			mIsVDR = true;
		else if (mId == -2)
			mIsVDR = false;
		else
			mIsVDR = pref.getString(DevicesTable.CLASS, "").equals(Devices.VDR_CLASSNAME);

		if (mIsVDR) {
			addPreferencesFromResource(R.xml.devicepreferences_vdr);

			// Populate the character set encoding list with all available
			final ListPreference charsetPref = (ListPreference) findPreference(DevicesTable.CHARACTERSET);

			CharsetHolder charsetHolder = new CharsetHolder();
			if (charsetHolder.isInitialized()) {
				initCharsetPref(charsetPref);
			} else {
				String[] currentCharsetPref = new String[1];
				currentCharsetPref[0] = charsetPref.getValue();
				charsetPref.setEntryValues(currentCharsetPref);
				charsetPref.setEntries(currentCharsetPref);

				new Thread(new Runnable() {
					public void run() {
						initCharsetPref(charsetPref);
					}
				}).start();
			}
			
			Preference sshkey = (Preference) findPreference("sshkey_pref");
			sshkey.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				
				@Override
				public boolean onPreferenceClick(Preference preference) {
					if (! Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
						Toast.makeText(DevicePreferencesActivity.this, "SD-Card not available", Toast.LENGTH_LONG).show();
						return true;
					}
					
					ArrayList<String> filenames = new ArrayList<String>();
					File filepath = Environment.getExternalStorageDirectory();
					if (filepath != null) {
						for (File file : filepath.listFiles()) {
							if (file.isFile()) {
								filenames.add(file.getName());
							}
						}
					}
					
					final CharSequence[] items = filenames.toArray(new CharSequence[filenames.size()]);
					if (filenames.size() > 0) {
						AlertDialog.Builder builder = new AlertDialog.Builder(DevicePreferencesActivity.this);
						builder.setTitle(getString(R.string.settings_sshkey_select));
						builder.setItems(items, new DialogInterface.OnClickListener() {
						    public void onClick(DialogInterface dialog, int item) {
						    	File file = new File(Environment.getExternalStorageDirectory() + "/" + items[item]);
						    	BufferedReader in = null;
								StringBuilder sb = new StringBuilder();
						    	try {
									in = new BufferedReader(new FileReader(file));
									String s;
									while ((s = in.readLine()) != null) {
										sb.append(s + "\n");
									}
								} catch (IOException e) {
									Toast.makeText(
											DevicePreferencesActivity.this,
											e.toString(),
											Toast.LENGTH_LONG).show();
								} catch (OutOfMemoryError e) {
									logger.error(
											"Out of Memory on import SSH-Key: {}",
											items[item]);
									Toast.makeText(
											DevicePreferencesActivity.this,
											"Out of Memory",
											Toast.LENGTH_LONG).show();
									return;
								} finally {
									if (in != null)
										try {
											in.close();
										} catch (IOException e) { }
								}
								
						    	if (sb.length() > 1024 * 10) {
						    		Toast.makeText(
						    				DevicePreferencesActivity.this,
						    				"SSH-Key too large",
						    				Toast.LENGTH_LONG).show();
						    		return;
						    	}

						    	Editor editor = pref.edit();
						    	editor.putString("sshkey", sb.toString());
						    	editor.commit();
						        
						    	Toast.makeText(getApplicationContext(), 
						    			String.format(getString(R.string.settings_sshkey_imported), items[item]), 
						    			Toast.LENGTH_SHORT).show();
						    }
						});
						AlertDialog dialog = builder.create();
						dialog.show();
					} else {
						Toast.makeText(DevicePreferencesActivity.this, 
								getString(R.string.settings_sshkey_no_files), Toast.LENGTH_LONG).show();
					}
					return true;
				}
			});
		} else {
			addPreferencesFromResource(R.xml.devicepreferences);

			final ListPreference classPref = (ListPreference) findPreference(DevicesTable.CLASS);
			initClassPref(classPref);
		}

		updateSummaries();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.devicepreferences_option_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.devpref_sshkey_delete:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.settings_sshkey_delete)
			       .setCancelable(false)
			       .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			        	   dialog.dismiss();
			        	   
			        	   Editor editor = pref.edit();
			        	   editor.putString("sshkey", null);
			        	   editor.commit();
			           }
			       })
			       .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			                dialog.cancel();
			           }
			       });
			AlertDialog alert = builder.create();
			alert.show();
			return true;
		case android.R.id.home:
			Intent intent = new Intent(this, AndroVDR.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		updateSummaries();
	}
		
	private void updateSummaries() {
		// for all text preferences, set hint as current database value
		for (String key : this.pref.values.keySet()) {
			Preference pref = this.findPreference(key);
			if (pref != null
					&& (!mIsVDR || ((pref.getKey().equals("name")
							|| pref.getKey().equals("timeout")
							|| pref.getKey().equals("margin_start")
							|| pref.getKey().equals("margin_stop")
							|| pref.getKey().equals("remote_user") 
							|| pref.getKey().equals("remote_timeout"))))) {

				CharSequence value = this.pref.getString(key, "");

				if (pref instanceof ListPreference) {
					ListPreference listPref = (ListPreference) pref;
					int entryIndex = listPref.findIndexOfValue((String) value);
					if (entryIndex >= 0)
						value = listPref.getEntries()[entryIndex];
				}

				pref.setSummary(value);
			}
		}
	}

	public class CursorPreferenceHack implements SharedPreferences {
		protected long id;

		protected Map<String, String> values = new HashMap<String, String>();

		public CursorPreferenceHack(long id) {
			this.id = id;

			cacheValues();
		}

		protected final void cacheValues() {
			// fill a cursor and cache the values locally
			// this makes sure we dont have any floating cursor to dispose later

			Cursor cursor = mDevices.getCursorForDevice(id);
			if (cursor.moveToFirst()) {
				for (int i = 0; i < cursor.getColumnCount(); i++) {
					String key = cursor.getColumnName(i);
					if (key.equals(DevicesTable.ID))
						continue;
					String value = cursor.getString(i);
					values.put(key, value);
				}
			}
			cursor.close();
			mDevices.dbClose();
		}

		public boolean contains(String key) {
			return values.containsKey(key);
		}

		public class Editor implements SharedPreferences.Editor {

			private ContentValues update = new ContentValues();

			public SharedPreferences.Editor clear() {
				logger.trace("clear");
				update = new ContentValues();
				return this;
			}

			public boolean commit() {
				if (id < 0) {
					id = mDevices.dbStore(update);
					setResult((int) id);
				} else {
					mDevices.dbUpdate(id, update);
				}
				
				// make sure we refresh the parent cached values
				cacheValues();

				// and update any listeners
				for (OnSharedPreferenceChangeListener listener : listeners) {
					listener.onSharedPreferenceChanged(
							CursorPreferenceHack.this, null);
				}

				return true;
			}

			public android.content.SharedPreferences.Editor putBoolean(
					String key, boolean value) {
				return this.putString(key, Boolean.toString(value));
			}

			public android.content.SharedPreferences.Editor putFloat(
					String key, float value) {
				return this.putString(key, Float.toString(value));
			}

			public android.content.SharedPreferences.Editor putInt(String key,
					int value) {
				return this.putString(key, Integer.toString(value));
			}

			public android.content.SharedPreferences.Editor putLong(String key,
					long value) {
				return this.putString(key, Long.toString(value));
			}

			public android.content.SharedPreferences.Editor putString(
					String key, String value) {
				update.put(key, value);
				return this;
			}

			public android.content.SharedPreferences.Editor remove(String key) {
				update.remove(key);
				return this;
			}

			@Override
			public void apply() {
				commit();
			}

			@Override
			public android.content.SharedPreferences.Editor putStringSet(
					String arg0, Set<String> arg1) {
				// TODO Auto-generated method stub
				return null;
			}

		}

		public Editor edit() {
			return new Editor();
		}

		public Map<String, ?> getAll() {
			return values;
		}

		public boolean getBoolean(String key, boolean defValue) {
			return Boolean.valueOf(this.getString(key, Boolean
					.toString(defValue)));
		}

		public float getFloat(String key, float defValue) {
			try {
				return Float.valueOf(this.getString(key, Float.toString(defValue)));
			} catch (NumberFormatException e) {
				logger.error("key: " + key , e);
				return defValue;
			}
		}

		public int getInt(String key, int defValue) {
			try {
				return Integer.valueOf(this.getString(key, Integer
						.toString(defValue)));
			} catch (NumberFormatException e) {
				logger.error("key: " + key, e);
				return defValue;
			}
		}

		public long getLong(String key, long defValue) {
			try {
				return Long.valueOf(this.getString(key, Long.toString(defValue)));
			} catch (NumberFormatException e) {
				logger.error("key: " + key, e);
				return defValue;
			}
		}

		public String getString(String key, String defValue) {
			if (!values.containsKey(key))
				return defValue;
			return values.get(key);
		}

		protected List<OnSharedPreferenceChangeListener> listeners = new LinkedList<OnSharedPreferenceChangeListener>();

		public void registerOnSharedPreferenceChangeListener(
				OnSharedPreferenceChangeListener listener) {
			listeners.add(listener);
		}

		public void unregisterOnSharedPreferenceChangeListener(
				OnSharedPreferenceChangeListener listener) {
			listeners.remove(listener);
		}

		@Override
		public Set<String> getStringSet(String arg0, Set<String> arg1) {
			// TODO Auto-generated method stub
			return null;
		}
	}

	public class CharsetHolder extends ListPreferenceValueHolder {

		@Override
		protected void setValues(List<CharSequence> ids,
				List<CharSequence> names) {
			for (Entry<String, Charset> entry : Charset.availableCharsets().entrySet()) {
				Charset c = entry.getValue();
				if (c.canEncode() && c.isRegistered()) {
					ids.add(c.displayName());
					names.add(c.displayName());
				}
			}
		}
	}

	public class ClassHolder extends ListPreferenceValueHolder {

		@Override
		protected void setValues(List<CharSequence> ids,
				List<CharSequence> names) {
			for (String name : mDevices.getPluginNames()) {
				ids.add(name);
				names.add(name);
			}
		}
	}
}
