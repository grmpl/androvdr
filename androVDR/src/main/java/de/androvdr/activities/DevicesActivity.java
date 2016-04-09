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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.AdapterView.AdapterContextMenuInfo;
import de.androvdr.DevicesTable;
import de.androvdr.Preferences;
import de.androvdr.R;
import de.androvdr.devices.Devices;

public class DevicesActivity extends AbstractListActivity {
	private static final int REQUEST_UPDATE = 0;
	private static final int REQUEST_ADD = 1;
	
	private static final int MENU_ID_DELETE = 0;
	private static final int MENU_ID_SWITCH = 1;
	
	private static final String[] VIEW_COLUMNS = new String[] { DevicesTable.NAME,
			DevicesTable.HOST };
	private static final int[] VIEW_IDS = new int[] { R.id.devicename,
			R.id.devicehost };
	private Devices mDevices;
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		mDevices.initDevices();
	}
	
	public void onButtonClick(View view) {
		Intent intent = new Intent(this, DevicePreferencesActivity.class);
		if (view.getId() == R.id.devices_add_vdr) {
			intent.putExtra("deviceid", -1);
			startActivityForResult(intent, REQUEST_ADD);
		} else if (view.getId() == R.id.devices_add_device) {
			intent.putExtra("deviceid", -2);
			startActivityForResult(intent, REQUEST_ADD);
		}
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_ID_DELETE:
			final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.devices_delete_device)
			       .setCancelable(false)
			       .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			        	   dialog.dismiss();
			        	   Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
			        	   mDevices.dbDelete(cursor.getInt(0));
			        	   final ListView view = getListView();
			        	   ((SimpleCursorAdapter) view.getAdapter()).getCursor().requery();
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
		case MENU_ID_SWITCH:
			
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
			
		setContentView(R.layout.devices);
		
		if (Preferences.blackOnWhite && Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
			getListView().setBackgroundColor(Color.WHITE);

		mDevices = Devices.getInstance();
		if (! mDevices.hasPlugins()) {
			Button b = (Button) findViewById(R.id.devices_add_device);
			b.setVisibility(View.GONE);
		}
		
		show();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		
		AdapterContextMenuInfo mi = (AdapterContextMenuInfo) menuInfo;
		Cursor cursor = (Cursor) getListAdapter().getItem(mi.position);
		menu.setHeaderTitle(cursor.getString(2));
		menu.add(0, MENU_ID_DELETE, 0, R.string.devices_delete);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mDevices.dbClose();
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Cursor cursor = (Cursor) getListView().getItemAtPosition(position);
		int deviceId = cursor.getInt(0);
		Intent intent = new Intent(this, DevicePreferencesActivity.class);
		intent.putExtra("deviceid", deviceId);
		startActivityForResult(intent, REQUEST_UPDATE);
	}

	private void show() {
		final Cursor cursor = mDevices.getCursorForAllDevices();
		startManagingCursor(cursor);
		final SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
				R.layout.devices_item, cursor, VIEW_COLUMNS, VIEW_IDS);
		setListAdapter(adapter);
		registerForContextMenu((ListView) findViewById(android.R.id.list));
	}
}
