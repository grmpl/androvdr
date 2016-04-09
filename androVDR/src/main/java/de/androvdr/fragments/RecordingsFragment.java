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

package de.androvdr.fragments;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import de.androvdr.Preferences;
import de.androvdr.R;
import de.androvdr.Recording;
import de.androvdr.RecordingViewItem;
import de.androvdr.controllers.RecordingController;

public class RecordingsFragment extends AbstractListFragment implements
		RecordingController.OnRecordingSelectedListener {
	private static transient Logger logger = LoggerFactory.getLogger(RecordingsFragment.class);
	
	private RecordingController mController;
	private ListView mListView;

	public RecordingController getController() {
		return mController;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (! Preferences.showDiskStatus) {
			LinearLayout lay = (LinearLayout) mActivity.findViewById(R.id.recdiskstatus);
			lay.setVisibility(View.GONE);
		}

		mListView = (ListView) mActivity.findViewById(android.R.id.list);

		/*
		 * setTheme doesn't change background color :(
		 */
		if (Preferences.blackOnWhite)
			mListView.setBackgroundColor(Color.WHITE);

		mController = new RecordingController(mActivity, mHandler, mListView,
				savedInstanceState);	
		mController.setOnRecordingSelectedListener(this);
		
		getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		
		if (mActivity.isDualPane()) {
			mUpdateSelectedItemThread = new UpdateSelectedItemThread() {
				@Override
				public int getPosition() {
					int position = mCurrentItemIndex;
					if (position == -1) {
						for (int i = 0; i < getListView().getCount(); i++) {
							RecordingViewItem item = (RecordingViewItem) getListView()
									.getItemAtPosition(i);
							if (! item.isFolder) {
								position = i;
								break;
							}
						}
					}
					logger.trace("position = {}", position);
					return position;
				}
			};
		}
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()) {
		case R.id.rec_play:
			mController.action(RecordingController.RECORDING_ACTION_PLAY, info.position);
			return true;
		case R.id.rec_play_start:
			mController.action(RecordingController.RECORDING_ACTION_PLAY_START, info.position);
			return true;
		case R.id.rec_remote:
			mController.action(RecordingController.RECORDING_ACTION_REMOTE);
			return true;
		case R.id.rec_delete:
			AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
			builder.setMessage(R.string.rec_delete_recording)
			       .setCancelable(false)
			       .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			        	   dialog.dismiss();
			        	   mController.action(RecordingController.RECORDING_ACTION_DELETE, info.position);
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
		default:
			return super.onContextItemSelected(item);
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		AdapterContextMenuInfo mi = (AdapterContextMenuInfo) menuInfo;
		if (! mController.isFolder(mi.position)) {
			MenuInflater inflater = mActivity.getMenuInflater();
			inflater.inflate(R.menu.recordings_menu, menu);
			menu.setHeaderTitle(mController.getTitle(mi.position));
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.recordings_option_menu, menu);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.recordings_fragment, container, false);

		float sd = getResources().getDisplayMetrics().scaledDensity;
		TextView tv = (TextView) root.findViewById(R.id.header_text);
		tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP,	(tv.getTextSize() / sd)
				+ Preferences.textSizeOffset);
		tv.setText(R.string.rec_recordings);
		
		tv = (TextView) root.findViewById(R.id.recdiskstatus_values);
		if (tv != null)
			tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP,	(tv.getTextSize() / sd)
					+ Preferences.textSizeOffset);

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH)
			registerForContextMenu(root.findViewById(android.R.id.list));
		return root;
	}
	
	@Override
	public boolean OnItemSelected(int position, Recording recording) {
		mCurrentItemIndex = position;
		if (mActivity.isDualPane()) {
			if (recording == null)
				showDetail(-1);
			else
				showDetail(recording.number);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.recm_sort_date:
			mController.action(RecordingController.RECORDING_ACTION_SORT_DATE);
			break;
		case R.id.recm_sort_name:
			mController.action(RecordingController.RECORDING_ACTION_SORT_NAME);
			break;
		default:
			super.onOptionsItemSelected(item);
		}
		return true;
	}
    
    @Override
    public void onPause() {
    	mController.onPause();
    	super.onPause();
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	mController.onResume();
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	mController.onSaveInstanceState(outState);
    }
    
    @Override
    protected void setSelectedItem(int position) {
		logger.trace("setSelectedItem position = {}", position);
		mController.action(RecordingController.RECORDING_ACTION_INFO, position);
		getListView().setItemChecked(position, true);
    }

    private void showDetail(int recordingnumber) {
		if (! mActivity.isDualPane())
			return;
		
		int fragmentId;
		if (Preferences.detailsLeft)
			fragmentId = R.id.detail_fragment_left;
		else
			fragmentId = R.id.detail_fragment_right;
		
		try {
			RecordingInfoFragment details = (RecordingInfoFragment)
			        getFragmentManager().findFragmentById(fragmentId);
			if (details == null || details.getRecordingNumber() != recordingnumber) {
			    details = RecordingInfoFragment.newInstance(recordingnumber);
			    FragmentTransaction ft = getFragmentManager().beginTransaction();
			    ft.replace(fragmentId, details);
			    ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
			    ft.commit();
			}
		} catch (Exception e) {
			logger.error("showDetails", e);
		}
	}
}
