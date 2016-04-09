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

package de.androvdr.controllers;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.hampelratte.svdrp.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import de.androvdr.AbstractViewHolder;
import de.androvdr.ActionModeHelper;
import de.androvdr.Channel;
import de.androvdr.Channels;
import de.androvdr.Epg;
import de.androvdr.Messages;
import de.androvdr.Preferences;
import de.androvdr.R;
import de.androvdr.VdrCommands;
import de.androvdr.activities.AbstractFragmentActivity;
import de.androvdr.activities.EpgdataActivity;

public class EpgsdataController extends AbstractController implements Runnable {
	private static transient Logger logger = LoggerFactory.getLogger(EpgsdataController.class);
	
	public static final int EPG_ALL = -1;
	public static final int EPG_NOW = -1;
	
	public static final int EPGSDATA_ACTION_RECORD = 1;
	public static final int EPGSDATA_ACTION_PROGRAMINFO = 2;
	
	public interface OnEpgdataSelectedListener {
		public boolean OnItemSelected(int position, Channel channel);
	}

	private ActionMode mActionMode;
	private int mChannelNumber;
	private EpgdataAdapter mEpgdataAdapter;
	private ArrayList<Epg> mEpgdata;
	private final boolean mIsMultiChannelView;
	private final ListView mListView;
	private final LinearLayout mMainView;
	private final int mMaxEpgdata;
	private OnEpgdataSelectedListener mSelectedListener;
	
	// --- needed by each row ---
	private final SimpleDateFormat dateformatter;
	private final SimpleDateFormat timeformatter;
	private final String[] weekdays;
	private final GregorianCalendar calendar;
	
	private Handler mThreadHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.arg1) {
			case Messages.MSG_DONE:
				setEpgAdapter(new EpgdataAdapter(mActivity, mEpgdata), mListView);
				sendMsg(mHandler, Messages.MSG_PROGRESS_DISMISS, null);
				sendMsg(mHandler, Messages.MSG_CONTROLLER_READY, null);
				break;
			default:
				Message newMsg = new Message();
				newMsg.copyFrom(msg);
				mHandler.sendMessage(newMsg);
			}
		}
	};
	
	public EpgsdataController(AbstractFragmentActivity activity, Handler handler,
			LinearLayout view, int channelNumber, int max) {
		super.onCreate(activity, handler);
		
		mMainView = view;
		mListView = (ListView) view.findViewById(android.R.id.list);
		
		mChannelNumber = channelNumber;
		mMaxEpgdata = max;
		mIsMultiChannelView = (channelNumber == EPG_NOW);
		
		dateformatter = new SimpleDateFormat(Preferences.dateformat);
		timeformatter = new SimpleDateFormat(Preferences.timeformat);
		weekdays = mActivity.getResources().getStringArray(R.array.weekday);
		calendar = new GregorianCalendar();
		
		sendMsg(mHandler, Messages.MSG_CONTROLLER_LOADING, R.string.loading);
		sendMsg(mHandler, Messages.MSG_PROGRESS_SHOW, R.string.loading);
		
		Thread thread = new Thread(this);
		thread.start();
	}
	
	public void action(int action, int position) {
		Epg epg = mEpgdataAdapter.getItem(position);
		
		switch (action) {
		case EPGSDATA_ACTION_RECORD:
			new SetTimerTask().execute(epg);
			break;
		case EPGSDATA_ACTION_PROGRAMINFO:
			try {
				Channel channel = new Channels(Preferences.getVdr().channellist).getChannel(epg.kanal);
				channel.viewEpg = epg;
				
				if (! mSelectedListener.OnItemSelected(position, channel)) {
					Intent intent = new Intent(mActivity, EpgdataActivity.class);
					intent.putExtra("channelnumber", channel.nr);
					mActivity.startActivityForResult(intent, 1);
				}
			} catch (IOException e) {
				logger.error("Couldn't load channels", e);
				sendMsg(mThreadHandler, Messages.MSG_ERROR, e.getMessage());
			}
			break;
		}
	}
	
	private OnItemClickListener getOnItemClickListener() {
		return new OnItemClickListener() {
			public void onItemClick(AdapterView<?> listView, View v, int position, long ID) {
				if (!mActivity.isDualPane())
					mListView.setItemChecked(position, false);
				if (mActionMode != null) {
					ActionModeHelper.finish(mActionMode);
					if (mActivity.isDualPane())
						action(EPGSDATA_ACTION_PROGRAMINFO, position);
				} else
					action(EPGSDATA_ACTION_PROGRAMINFO, position);
			}
		};
	}

	private OnItemLongClickListener getOnItemLongClickListener() {
		return new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> listView, View v,
					int position, long ID) {
				if (position >= mEpgdataAdapter.getCount())
					return false;
				
				mListView.setItemChecked(position, true);
				if (mActivity.isDualPane())
					action(EPGSDATA_ACTION_PROGRAMINFO, position);
				
				if (mActionMode == null)
					mActionMode = mActivity.startActionMode(new ModeCallback());
				return true;
			}
		};
	}

	public String getTitle(int position) {
		Epg epg = mEpgdataAdapter.getItem(position);
		return epg.titel;
	}
	
	public void run() {
		try {
			Channels channels = new Channels(Preferences.getVdr().channellist);
			if (mChannelNumber == EPG_NOW) {
				mEpgdata = new ArrayList<Epg>();
				for (int i = 0; i < channels.getItems().size(); i++) {
					Channel channel = channels.getItems().get(i);
					channel.updateEpg(true);
					mEpgdata.add(channel.getNow());
					mEpgdata.add(channel.getNext());
				}
			} else {
				if (mChannelNumber == 0) {
					Channel c = channels.addChannel(-1);
					if (c == null)
						throw new IOException("Couldn't get channel");
					
					mChannelNumber = c.nr;
				}

				if (channels.getChannel(mChannelNumber) == null) {
					Channel c = channels.addChannel(mChannelNumber);
					c.isTemp = true;
				}
				
				if (mMaxEpgdata == EPG_ALL)
					mEpgdata = channels.getChannel(mChannelNumber).getAll();
				else
					mEpgdata = channels.getChannel(mChannelNumber).get(mMaxEpgdata);
			}
			sendMsg(mThreadHandler, Messages.MSG_DONE, null);
		} catch (IOException e) {
			logger.error("Couldn't load epg data", e);
			sendMsg(mThreadHandler, Messages.MSG_ERROR, e.getMessage());
		}
	}

	private void setEpgAdapter(EpgdataAdapter adapter, ListView listView) {
		mEpgdataAdapter = adapter;

		TextView tv = (TextView) mMainView.findViewById(R.id.header_text);
		if (! mIsMultiChannelView) {
			try {
				tv.setText(new Channels(Preferences.getVdr().channellist).getName(mChannelNumber));			
			} catch (Exception e) {
				tv.setText("");
				logger.error("Couldn't load channels", e);
			}
		} else {
			tv.setVisibility(View.GONE);
		}

		listView.setAdapter(adapter);
		listView.setOnItemClickListener(getOnItemClickListener());
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
			listView.setOnItemLongClickListener(getOnItemLongClickListener());
		listView.setSelected(true);
		listView.setSelection(0);
	}

	public void setOnEpgdataSelectedListener(OnEpgdataSelectedListener listener) {
		mSelectedListener = listener;
	}
	
	private class EpgdataAdapter extends ArrayAdapter<Epg> {
		private final Activity mActivity;
		
		private class ViewHolder extends AbstractViewHolder {
			public TextView date;
			public TextView channel;
			public ProgressBar progress;
			public TextView title;
			public TextView shorttext;
		}
		public EpgdataAdapter(Activity activity, ArrayList<Epg> epgdata) {
			super(activity, R.layout.epgsdata_item, epgdata);
			mActivity = activity;
		}
		
		public View getView(int position, View convertView, ViewGroup parent) {
			View row;
			if (convertView == null) {
				LayoutInflater inflater = mActivity.getLayoutInflater();
				row = inflater.inflate(R.layout.epgsdata_item, null);
				
				ViewHolder vh = new ViewHolder();
				vh.date = (TextView) row.findViewById(R.id.epgdate);
				vh.channel = (TextView) row.findViewById(R.id.epgchannel);
				vh.title = (TextView) row.findViewById(R.id.epgtitle);
				vh.shorttext = (TextView) row.findViewById(R.id.epgshorttext);
				vh.setTextSize(Preferences.textSizeOffset,
						mActivity.getResources().getDisplayMetrics().scaledDensity);
				row.setTag(vh);
			} else {
				row = convertView;
			}

			Epg item = this.getItem(position);
			ViewHolder vh = (ViewHolder) row.getTag();
			
			calendar.setTimeInMillis(item.startzeit * 1000);
			vh.date.setText(weekdays[calendar.get(Calendar.DAY_OF_WEEK) - 1]
					+ " " + dateformatter.format(calendar.getTime())
					+ " " + timeformatter.format(calendar.getTime()));

			if (mIsMultiChannelView) {
				String text;
				try {
					text = String.valueOf(new Channels(Preferences.getVdr().channellist).getName(item.kanal));
				} catch (IOException e) {
					text = "";
				}
				vh.channel.setText(text);
				vh.channel.setVisibility(View.VISIBLE);

				vh.progress.setProgress(item.getActualPercentDone());
				vh.progress.setVisibility(View.VISIBLE);
			}
			
        	String text = item.titel;
        	vh.title.setText(text);

        	if(item.kurztext != null) {
            	vh.shorttext.setText(item.kurztext);                            
        	}
       		else {
       			vh.shorttext.setText("");
       		}

			return row;
		}
	}

	private final class ModeCallback implements ActionMode.Callback {
		
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			boolean result = false;
			int position = mListView.getCheckedItemPosition();
			
			switch (item.getItemId()) {
			case R.id.epgs_record:
				action(EPGSDATA_ACTION_RECORD, position);
				result = true;
			}
			mode.finish();
			return result;
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			MenuInflater inflater = mActivity.getMenuInflater();
			if (Preferences.blackOnWhite)
				inflater.inflate(R.menu.epgs_menu_light, menu);
			else
				inflater.inflate(R.menu.epgs_menu, menu);
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			if (!mActivity.isDualPane()) {
				int position = mListView.getCheckedItemPosition();
				if (position != AdapterView.INVALID_POSITION)
					mListView.setItemChecked(position, false);
			}
			mActionMode = null;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}
	}
	
	private class SetTimerTask extends AsyncTask<Epg, Void, Response> {

		@Override
		protected Response doInBackground(Epg... params) {
			return VdrCommands.setTimer(params[0]);
		}
		
		@Override
		protected void onPostExecute(Response result) {
			if (result.getCode() != 250)
				logger.error("Couldn't set timer: {}", result.getCode() + " - " + result.getMessage());
			Toast.makeText(mActivity, result.getCode() + " - " + result.getMessage().replaceAll("\n$", ""), 
					Toast.LENGTH_SHORT).show();
		}
	}
}
