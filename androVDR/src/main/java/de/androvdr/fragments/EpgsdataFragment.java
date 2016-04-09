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
import de.androvdr.Channel;
import de.androvdr.Preferences;
import de.androvdr.R;
import de.androvdr.controllers.EpgsdataController;

public class EpgsdataFragment extends AbstractListFragment implements
		EpgsdataController.OnEpgdataSelectedListener {
	private static transient Logger logger = LoggerFactory.getLogger(EpgsdataFragment.class);
	private int mChannelNumber;
	private EpgsdataController mController;
	private LinearLayout mView;
	private int mMaxItems;
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		Bundle bundle = mActivity.getIntent().getExtras();
		if(bundle != null){
			mChannelNumber = bundle.getInt("channelnumber");
			mMaxItems = bundle.getInt("maxitems");
		}
		else {
			mChannelNumber = 0;
			mMaxItems = Preferences.getVdr().epgmax;
		}
	    mView = (LinearLayout) mActivity.findViewById(R.id.epgsdata_main);

		/*
		 * setTheme doesn't change background color :(
		 */
		if (Preferences.blackOnWhite)
			mView.setBackgroundColor(Color.WHITE);
		
		mController = new EpgsdataController(mActivity, mHandler, 
				mView, mChannelNumber, mMaxItems);
		mController.setOnEpgdataSelectedListener(this);
		
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
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = mActivity.getMenuInflater();
		inflater.inflate(R.menu.epgs_menu, menu);
		AdapterContextMenuInfo mi = (AdapterContextMenuInfo) menuInfo;
		menu.setHeaderTitle(mController.getTitle(mi.position));
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.epgs_option_menu, menu);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.epgsdata_fragment, container, false);

		TextView tv = (TextView) root.findViewById(R.id.header_text);
		tv.setTextSize(
				TypedValue.COMPLEX_UNIT_DIP,
				(tv.getTextSize() / getResources().getDisplayMetrics().scaledDensity)
						+ Preferences.textSizeOffset);
		
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH)
			registerForContextMenu(root.findViewById(android.R.id.list));
		return root;
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()) {
		case R.id.epgs_record:
			mController.action(EpgsdataController.EPGSDATA_ACTION_RECORD, info.position);
			return true;
		}
		return false;
	}

	@Override
	public boolean OnItemSelected(int position, Channel channel) {
		mCurrentItemIndex = position;
		if (mActivity.isDualPane()) {
			showDetail(channel.nr);
			return true;
		} else {
			return false;
		}
	}

    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.epgs_search:
			mActivity.onSearchRequested();
			break;
		default:
			super.onOptionsItemSelected(item);
		}
		return true;
	}

    @Override
    protected void setSelectedItem(int position) {
		logger.trace("setSelectedItem position = {}", position);
		mController.action(EpgsdataController.EPGSDATA_ACTION_PROGRAMINFO, position);
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
			logger.error("showDetails", e);
		}
	}
}
