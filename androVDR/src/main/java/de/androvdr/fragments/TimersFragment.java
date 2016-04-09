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
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.TextView;
import de.androvdr.Channel;
import de.androvdr.EpgSearch;
import de.androvdr.Preferences;
import de.androvdr.R;
import de.androvdr.controllers.TimerController;

public class TimersFragment extends AbstractListFragment 
		implements TimerController.OnTimerSelectedListener {
	private static transient Logger logger = LoggerFactory.getLogger(TimersFragment.class);
	
	private TimerController mController;
	private boolean mIsSearch = false;
	private ListView mListView;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
	    mListView = (ListView) mActivity.findViewById(android.R.id.list);

		/*
		 * setTheme doesn't change background color :(
		 */
		if (Preferences.blackOnWhite)
			mListView.setBackgroundColor(Color.WHITE);
		
		/*
		 * perform epgsearch ?
		 */
		EpgSearch epgSearch = null;
	    Intent intent = mActivity.getIntent();
	    if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
	      String query = intent.getStringExtra(SearchManager.QUERY);
	      epgSearch = new EpgSearch();
	      epgSearch.search = query.trim();
	      epgSearch.inTitle = Preferences.epgsearch_title;
	      epgSearch.inSubtitle = Preferences.epgsearch_subtitle;
	      epgSearch.inDescription = Preferences.epgsearch_description;
	      TextView tv = (TextView) mActivity.findViewById(R.id.header_text);
	      tv.setText(epgSearch.search);
	      mIsSearch = true;
	    }

		mController = new TimerController(mActivity, mHandler, mListView, epgSearch);
		mController.setOnTimerSelectedListener(this);
		mController.isSearch = mIsSearch;
		
		getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		if (mActivity.isDualPane()) {
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
	public boolean onContextItemSelected(MenuItem item) {
		final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()) {
		case R.id.timer_overview:
			mController.action(TimerController.TIMER_ACTION_PROGRAMINFOS, info.position);
			return true;
		case R.id.timer_overviewfull:
			mController.action(TimerController.TIMER_ACTION_PROGRAMINFOS_ALL, info.position);
			return true;
		case R.id.timer_delete:
			AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
			builder.setMessage(R.string.timer_delete_timer)
			       .setCancelable(false)
			       .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			        	   dialog.dismiss();
			        	   mController.action(TimerController.TIMER_ACTION_DELETE, info.position);
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
		case R.id.timer_toggle:
			mController.action(TimerController.TIMER_ACTION_TOGGLE, info.position);
			return true;
		case R.id.timer_record:
			mController.action(TimerController.TIMER_ACTION_RECORD, info.position);
			return true;
		case R.id.timer_switch:
			mController.action(TimerController.TIMER_ACTION_SWITCH_CAHNNEL, info.position);
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		AdapterContextMenuInfo mi = (AdapterContextMenuInfo) menuInfo;
		MenuInflater inflater = mActivity.getMenuInflater();
		if (mIsSearch)
			inflater.inflate(R.menu.timers_menu_search, menu);
		else
			inflater.inflate(R.menu.timers_menu, menu);
		menu.setHeaderTitle(mController.getTitle(mi.position));
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.timers_fragment, container, false);
		
		TextView tv = (TextView) root.findViewById(R.id.header_text);
		tv.setText(R.string.timers);
		tv.setTextSize(
				TypedValue.COMPLEX_UNIT_DIP,
				(tv.getTextSize() / getResources().getDisplayMetrics().scaledDensity)
						+ Preferences.textSizeOffset);

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH)
			registerForContextMenu(root.findViewById(android.R.id.list));
		return root;
	}
	
	@Override
	public boolean OnTimerSelected(int position, Channel channel) {
		mCurrentItemIndex = position;
		if (mActivity.isDualPane()) {
			showDetail(channel.nr);
			return true;
		} else {
			return false;
		}
	}

	@Override
	protected void setSelectedItem(int position) {
		logger.trace("setSelectedItem position = {}", position);
		mController.action(TimerController.TIMER_ACTION_SHOW_EPG, position);
		getListView().setItemChecked(position, true);
	}
	
    private void showDetail(int channelnumber) {
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
			logger.error("showDetail", e);
		}
	}
}
