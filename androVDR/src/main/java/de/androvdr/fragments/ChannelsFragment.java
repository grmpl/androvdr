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

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import de.androvdr.Channel;
import de.androvdr.Preferences;
import de.androvdr.R;
import de.androvdr.activities.ChannelsActivity;
import de.androvdr.controllers.ChannelController;

public class ChannelsFragment extends AbstractListFragment implements 
		ChannelController.OnChannelSelectedListener, Channel.OnCurrentEpgChangedListener {
	private static transient Logger logger = LoggerFactory.getLogger(ChannelsFragment.class);
	
	public static final String SEARCHTIME = "searchtime";
	
	private ChannelController mController;
	private Channel mCurrentChannel;
	private ListView mListView;
	private long mSearchTime;
	
	public ChannelController getController() {
		return mController;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (! Preferences.showCurrentChannel) {
			LinearLayout lay = (LinearLayout) mActivity.findViewById(R.id.channels_currentchannel);
			lay.setVisibility(View.GONE);
		}

		mListView = (ListView) mActivity.findViewById(android.R.id.list);

		/*
		 * setTheme doesn't change background color :(
		 */
		if (Preferences.blackOnWhite)
			mListView.setBackgroundColor(Color.WHITE);
		
		mSearchTime = mActivity.getIntent().getLongExtra(SEARCHTIME, 0);
	    mController = new ChannelController(mActivity, mHandler, mListView, mSearchTime);
	    mController.setOnChannelSelectedListener(this);
		
		getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
	    if (mActivity.isDualPane()) {
			getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
			mUpdateSelectedItemThread = new UpdateSelectedItemThread() {
				@Override
				public int getPosition() {
					int position = mCurrentItemIndex;
					if (position == -1 && getListView().getCount() > 0)
						position = 0;
					return position;
				}
			};
	    }
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = mActivity.getMenuInflater();
		inflater.inflate(R.menu.channels_menu, menu);
		AdapterContextMenuInfo mi = (AdapterContextMenuInfo) menuInfo;
		menu.setHeaderTitle(mController.getChannelName(mi.position));
		
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mActivity);
		if (mSearchTime != 0) {
			menu.removeItem(R.id.cm_remote);
			menu.removeItem(R.id.cm_livetv);
		}
		else if (!sp.getBoolean("livetv_enabled", false)) {
			menu.removeItem(R.id.cm_livetv);
		}
		
		if (Preferences.useInternet && ! Preferences.getVdr().extremux) {
			MenuItem menuitem = menu.findItem(R.id.cm_livetv);
			if (menuitem != null)
				menuitem.setEnabled(false);
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.channels_option_menu, menu);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.extendedchannels_fragment, container, false);

		float sd = getResources().getDisplayMetrics().scaledDensity;
		TextView tv = (TextView) root.findViewById(R.id.header_text);
		tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, (tv.getTextSize() / sd)
				+ Preferences.textSizeOffset);
		tv.setText(R.string.channels);
	    
		tv = (TextView) root.findViewById(R.id.footer_text);
	    if (tv != null)
			tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP,	(tv.getTextSize() / sd) 
					+ Preferences.textSizeOffset);
	    
	    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	    	registerForContextMenu(root.findViewById(android.R.id.list));
		return root;
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()) {
		case R.id.cm_switch:
			mController.action(ChannelController.CHANNEL_ACTION_SWITCH, info.position);
			return true;
		case R.id.cm_overview:
			mController.action(ChannelController.CHANNEL_ACTION_PROGRAMINFOS, info.position);
			return true;
		case R.id.cm_overviewfull:
			mController.action(ChannelController.CHANNEL_ACTION_PROGRAMINFOS_ALL, info.position);
			return true;
		case R.id.cm_remote:
			mController.action(ChannelController.CHANNEL_ACTION_REMOTECONTROL, info.position);
			return true;
		case R.id.cm_record:
			mController.action(ChannelController.CHANNEL_ACTION_RECORD, info.position);
			return true;
		case R.id.cm_livetv:
			mController.action(ChannelController.CHANNEL_ACTION_LIVETV, info.position);
			return true;
		}
		return false;
	}

	@Override
	public void OnCurrentEpgChanged(Channel channel) {
		final int position = mController.getItemPosition(channel);
		mUpdateSelectedItemThread = new UpdateSelectedItemThread() {
				@Override
				public int getPosition() {
					return position;
				}
		};
	}

	@Override
	public void onDestroy() {
		if (mCurrentChannel != null) {
			mCurrentChannel.setOnCurrentEpgChangedListener(null);
			logger.trace("OnDestroy EpgChangedListener <- {}", mCurrentChannel.nr);
		}
		super.onDestroy();
	}
	
	@Override
	public boolean OnItemSelected(int position, Channel channel) {
		logger.debug("OnItemSelected: position={} channel={}", position, channel.nr);
		
		if (mActivity.isDualPane()) {
			if (mCurrentChannel != null) {
				mCurrentChannel.setOnCurrentEpgChangedListener(null);
				logger.trace("OnItemSelected EpgChangedListener <- {}", mCurrentChannel.nr);
			}
			mCurrentChannel = channel;
			mCurrentChannel.setOnCurrentEpgChangedListener(this);
			logger.trace("OnItemSelected EpgChangedListener -> {}", mCurrentChannel.nr);
			
			mCurrentItemIndex = position;
			showDetail(channel.nr);
			return true;
		} else {
			mCurrentItemIndex = position;
			return false;
		}
	}

    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.cm_search:
			mActivity.onSearchRequested();
			break;
		case R.id.cm_whats_on:
			mActivity.showDialog(ChannelsActivity.DIALOG_WHATS_ON);
			break;
		default:
			super.onOptionsItemSelected(item);
		}
		return true;
	}
    
	@Override
	public void onPause() {
		super.onPause();
		mController.onPause();
	}

	@Override
	public void onResume() {
		super.onResume();
		mController.onResume();
	}
	
	@Override
	protected void setSelectedItem(int position) {
		logger.trace("setSelectedItem position = {}", position);
		mController.action(ChannelController.CHANNEL_ACTION_PROGRAMINFO, position);
		getListView().setItemChecked(position, true);
		
		Channel ch = (Channel) getListView().getItemAtPosition(position);
		ch.setOnCurrentEpgChangedListener(this);
	}
	
	private void showDetail(int channelnumber) {
		logger.trace("showDetail channelnumber = {} isDualPane = {}", channelnumber, mActivity.isDualPane());
		if (! mActivity.isDualPane())
			return;
		
		int fragmentId;
		if (Preferences.detailsLeft)
			fragmentId = R.id.detail_fragment_left;
		else
			fragmentId = R.id.detail_fragment_right;

		try {
			EpgdataFragment details = EpgdataFragment.newInstance(channelnumber);
			FragmentTransaction ft = getFragmentManager().beginTransaction();
			ft.replace(fragmentId, details);
			ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
			ft.commit();
		} catch (Exception e) {
			logger.error("showDetails", e);
		}
	}
}
