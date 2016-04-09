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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;

import org.hampelratte.svdrp.Response;
import org.hampelratte.svdrp.commands.CHAN;
import org.hampelratte.svdrp.commands.DELT;
import org.hampelratte.svdrp.commands.MODT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import de.androvdr.AbstractViewHolder;
import de.androvdr.ActionModeHelper;
import de.androvdr.Channel;
import de.androvdr.Channels;
import de.androvdr.Epg;
import de.androvdr.EpgSearch;
import de.androvdr.Messages;
import de.androvdr.Preferences;
import de.androvdr.R;
import de.androvdr.Timer;
import de.androvdr.Timers;
import de.androvdr.VdrCommands;
import de.androvdr.activities.AbstractFragmentActivity;
import de.androvdr.activities.EpgdataActivity;
import de.androvdr.activities.EpgsdataActivity;
import de.androvdr.devices.Devices;
import de.androvdr.devices.VdrDevice;
import de.androvdr.svdrp.ConnectionProblem;
import de.androvdr.svdrp.VDRConnection;

public class TimerController extends AbstractController implements Runnable {
	public final static int TIMER_ACTION_DELETE = 1;
	public final static int TIMER_ACTION_TOGGLE = 2;
	public final static int TIMER_ACTION_SHOW_EPG = 3;
	public final static int TIMER_ACTION_RECORD = 4;
	public final static int TIMER_ACTION_PROGRAMINFOS = 5;
	public final static int TIMER_ACTION_PROGRAMINFOS_ALL = 6;
	public final static int TIMER_ACTION_SWITCH_CAHNNEL = 7;

	public interface OnTimerSelectedListener {
		public boolean OnTimerSelected(int position, Channel channel);
	}
	
	private static transient Logger logger = LoggerFactory.getLogger(TimerController.class);

	private ActionMode mActionMode;
	private TimerAdapter mAdapter;
	private Channels mChannels;
	private final ListView mListView;
	private final EpgSearch mSearchFor;
	private OnTimerSelectedListener mSelectedListener;
	private ArrayList<Timer> mTimer;
	
	// --- needed by each row ---
	private final SimpleDateFormat dateformatter;
	private final SimpleDateFormat timeformatter;
	private final String[] weekdays;
	private final GregorianCalendar calendar;
	
	public boolean isSearch = false;
	
	private Handler mThreadHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.arg1) {
			case Messages.MSG_DONE:
				mAdapter = new TimerAdapter(mActivity, mTimer);
				setTimerAdapter(mAdapter, mListView);
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

	public TimerController(AbstractFragmentActivity activity, Handler handler, ListView listView, EpgSearch epgSearch) {
		super.onCreate(activity, handler);
		mListView = listView;
		mSearchFor = epgSearch;
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
		if (position >= mAdapter.getCount())
			return;
		
		final Timer item = mAdapter.getItem(position);

		switch (action) {
		case TIMER_ACTION_DELETE:
			new TimerDeleteTask().execute(item);
			break;
		case TIMER_ACTION_PROGRAMINFOS:
		case TIMER_ACTION_PROGRAMINFOS_ALL:
			Intent intent = new Intent(mActivity, EpgsdataActivity.class);
			intent.putExtra("channelnumber", item.channel);
			if (action == TIMER_ACTION_PROGRAMINFOS)
				intent.putExtra("maxitems", Preferences.getVdr().epgmax);
			else
				intent.putExtra("maxitems", EpgsdataController.EPG_ALL);
			mActivity.startActivityForResult(intent, 1);
			break;
		case TIMER_ACTION_RECORD:
			new TimerRecordTask().execute(item);
			break;
		case TIMER_ACTION_SHOW_EPG:
			new GetEpgTask().execute(item);
			break;
		case TIMER_ACTION_SWITCH_CAHNNEL:
			new SwitchChannelTask().execute(item);
			break;
		case TIMER_ACTION_TOGGLE:
			new TimerToggleTask().execute(item);
			break;
		}
	}
	
	private OnItemClickListener getOnItemClickListener() {
		return new OnItemClickListener() {
			public void onItemClick(AdapterView<?> listView, View v,
					int position, long ID) {
				if (!mActivity.isDualPane())
					mListView.setItemChecked(position, false);
				if (mActionMode != null) {
					if (mActivity.isDualPane())
						action(TIMER_ACTION_SHOW_EPG, position);
					ActionModeHelper.finish(mActionMode);
			} else
					action(TIMER_ACTION_SHOW_EPG, position);
			}
		};
	}

	private OnItemLongClickListener getOnItemLongClickListener() {
		return new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> listView, View v,
					int position, long ID) {
				if (position >= mAdapter.getCount())
					return false;
				
				mListView.setItemChecked(position, true);
				if (mActivity.isDualPane())
					action(TIMER_ACTION_SHOW_EPG, position);
				
				if (mActionMode == null)
					mActionMode = mActivity.startActionMode(new ModeCallback());
				return true;
			}
		};
	}

	public CharSequence getTitle(int position) {
		return mAdapter.getItem(position).title;
	}
	
	@Override
	public void run() {
		try {
			if (mSearchFor == null)
				mTimer = new Timers().getItems();
			else {
			    mTimer = new Timers(mSearchFor).getItems();
			}
			mChannels = new Channels(Preferences.getVdr().channellist);
			sendMsg(mThreadHandler, Messages.MSG_DONE, null);
		} catch (IOException e) {
			logger.error("Couldn't load timers or execute epgsearch", e);
			if (e.toString().contains("550"))
				sendMsg(mThreadHandler, Messages.MSG_EPGSEARCH_NOT_FOUND, null);
			else
				sendMsg(mThreadHandler, Messages.MSG_ERROR, e.getMessage());
		}
	}

	public void setOnTimerSelectedListener(OnTimerSelectedListener listener) {
		mSelectedListener = listener;
	}
	
	private void setTimerAdapter(TimerAdapter adapter, ListView listView) {
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(getOnItemClickListener());
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
			listView.setOnItemLongClickListener(getOnItemLongClickListener());
		listView.setSelected(true);
		listView.setSelection(0);
	}
	
	private class GetEpgTask extends AsyncTask<Timer, Void, String> {

		@Override
		protected String doInBackground(Timer... params) {
			Timer timer = params[0];
			try {
				VdrDevice vdr = Preferences.getVdr();
				Channels channels = new Channels(vdr.channellist);
				Channel channel = channels.getChannel(timer.channel);
				if (channel == null) {
					channel = channels.addChannel(timer.channel);
					channel.isTemp = true;
				}
				channel.viewEpg = channel.getAt(timer.start + ((vdr.margin_start + 1) * 60));
				int position = mAdapter.getPosition(timer);
				if (! mSelectedListener.OnTimerSelected(position, channel)) {
					Intent intent = new Intent(mActivity, EpgdataActivity.class);
					intent.putExtra("channelnumber", channel.nr);
					mActivity.startActivityForResult(intent, 1);
				}
				return "";
			} catch (IOException e) {
				logger.error("Couldn't get epg data", e);
				return (e.getMessage());
			}
		}

		@Override
		protected void onPostExecute(String result) {
			sendMsg(mHandler, Messages.MSG_PROGRESS_DISMISS, null);
			if (result != "")
				Toast.makeText(mActivity, result, Toast.LENGTH_LONG).show();
		}

		@Override
		protected void onPreExecute() {
			sendMsg(mHandler, Messages.MSG_PROGRESS_SHOW, R.string.searching);
		}
		
	}
	
	private final class ModeCallback implements ActionMode.Callback {
		
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			boolean result = false;
			final int position = mListView.getCheckedItemPosition();
			
			switch (item.getItemId()) {
			case R.id.timer_overview:
				action(TIMER_ACTION_PROGRAMINFOS, position);
				result = true;
				break;
			case R.id.timer_overviewfull:
				action(TIMER_ACTION_PROGRAMINFOS_ALL, position);
				result = true;
				break;
			case R.id.timer_delete:
				AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
				builder.setMessage(R.string.timer_delete_timer)
				       .setCancelable(false)
				       .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
				           public void onClick(DialogInterface dialog, int id) {
				        	   dialog.dismiss();
				        	   action(TIMER_ACTION_DELETE, position);
				           }
				       })
				       .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
				           public void onClick(DialogInterface dialog, int id) {
				                dialog.cancel();
				           }
				       });
				AlertDialog alert = builder.create();
				alert.show();
				result = true;
				break;
			case R.id.timer_toggle:
				action(TIMER_ACTION_TOGGLE, position);
				result = true;
				break;
			case R.id.timer_record:
				action(TIMER_ACTION_RECORD, position);
				result = true;
				break;
			case R.id.timer_switch:
				action(TIMER_ACTION_SWITCH_CAHNNEL, position);
				result = true;
				break;
			}
			mode.finish();
			return result;
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			MenuInflater inflater = mActivity.getMenuInflater();
			if (Preferences.blackOnWhite) {
				if (isSearch)
					inflater.inflate(R.menu.timers_menu_search_light, menu);
				else
					inflater.inflate(R.menu.timers_menu_light, menu);
			} else {
				if (isSearch)
					inflater.inflate(R.menu.timers_menu_search, menu);
				else
					inflater.inflate(R.menu.timers_menu, menu);
			}
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
	
	private class SwitchChannelTask extends AsyncTask<Timer, Void, Response> {

		@Override
		protected Response doInBackground(Timer... params) {
			return VDRConnection.send(new CHAN(Integer.toString(params[0].channel)));
		}
		
		@Override
		protected void onPostExecute(Response result) {
		    if(result.getCode() == 250) {
		    	Devices devices = Devices.getInstance();
		    	devices.updateChannelSensor();
		    	mActivity.finish();
		    } else {
		        logger.error("Couldn't switch channel: {}", result.getCode() + " - " + result.getMessage());
		        
		        if (! mActivity.isFinishing())
			        Toast.makeText(mActivity, result.getCode() + " - " + result.getMessage().replaceAll("\n$", ""), 
			        		Toast.LENGTH_LONG).show();
		    }
		}
	}

	private class TimerAdapter extends ArrayAdapter<Timer> {
		private final Activity mActivity;
		
		private class ViewHolder extends AbstractViewHolder {
			public TextView date;
			public TextView channel;
			public TextView time;
			public TextView status;
			public ImageView folderimage;
			public TextView foldername;
			public TextView title;
		}
		
		public TimerAdapter(Activity activity, ArrayList<Timer> timer) {
			super(activity, R.layout.timers_item, timer);
			mActivity = activity;
		}
		
		public View getView(int position, View convertView, ViewGroup parent) {
			View row;
			if (convertView == null) {
				LayoutInflater inflater = mActivity.getLayoutInflater();
				row = inflater.inflate(R.layout.timers_item, null);
				
				ViewHolder vh = new ViewHolder();
				vh.date = (TextView) row.findViewById(R.id.timer_date);
				vh.channel = (TextView) row.findViewById(R.id.timer_channel);
				vh.time = (TextView) row.findViewById(R.id.timer_time);
				vh.status = (TextView) row.findViewById(R.id.timer_status);
				vh.folderimage = (ImageView) row.findViewById(R.id.timer_folderimage);
				vh.foldername = (TextView) row.findViewById(R.id.timer_folder);
				vh.title = (TextView) row.findViewById(R.id.timer_title);
				vh.setTextSize(Preferences.textSizeOffset,
						mActivity.getResources().getDisplayMetrics().scaledDensity);
				row.setTag(vh);
			} else {
				row = convertView;
			}
			
			Timer item = this.getItem(position);
			ViewHolder vh = (ViewHolder) row.getTag();
			
			calendar.setTimeInMillis(item.start * 1000);
			if (item.noDate != "")
				vh.date.setText(item.noDate);
			else
				vh.date.setText(weekdays[calendar.get(Calendar.DAY_OF_WEEK) - 1]
							+ " " + dateformatter.format(calendar.getTime()));
			
			if (mChannels.getName(item.channel).equals(""))
				vh.channel.setText("(" + item.channel + ")");
			else
				vh.channel.setText(mChannels.getName(item.channel));

			StringBuilder sb = new StringBuilder();
			sb.append(timeformatter.format(calendar.getTime()));
			sb.append(" - ");
			calendar.setTimeInMillis(item.end * 1000);
			sb.append(timeformatter.format(calendar.getTime()));
			vh.time.setText(sb.toString());
			
			if (mSearchFor == null) {
				switch (item.getStatus()) {
				case Timer.TIMER_INACTIVE: 
					vh.status.setText("Inactive");
					break;
				case Timer.TIMER_ACTIVE:
					vh.status.setText("Active");
					break;
				case Timer.TIMER_VPS:
					vh.status.setText("VPS");
					break;
				case Timer.TIMER_RECORDING:
					vh.status.setText("Rec");
					break;
				default:
					vh.status.setText("");
					break;
				}
			} else {
				vh.status.setVisibility(View.GONE);
			}

			if (item.inFolder()) {
				vh.foldername.setText(item.folder());
				vh.foldername.setVisibility(View.VISIBLE);
				vh.folderimage.setVisibility(View.VISIBLE);
			}
			else {
				vh.foldername.setText("");
				vh.foldername.setVisibility(View.GONE);
				vh.folderimage.setVisibility(View.GONE);
			}

			vh.title.setText(item.title);

			return row;
		}
	}
	
	private class TimerBaseTask extends AsyncTask<Timer, Void, String> {

		@Override
		protected String doInBackground(Timer... params) {
			Timer timer = params[0];
			String result = null;
			
			int lastUpdate = (int) (new Date().getTime() / 60000);
			if (timer.lastUpdate < lastUpdate) {
				result = update();
				if (result != null)
					return result;
			}
			
			Response response =  doIt(timer);
			
			if (response.getCode() == 250) {
				VDRConnection.close();
				result = update();
			}
			else
				result = response.getCode() + " - " + response.getMessage().replaceAll("\n$", "");
			
			return result;
		}
		
		protected Response doIt(Timer timer) {
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			sendMsg(mHandler, Messages.MSG_PROGRESS_DISMISS, null);
			
			for (int i = mAdapter.getCount() -1; i >= 0; i--)
				if (mAdapter.getItem(i).lastUpdate < 0)
					mAdapter.remove(mAdapter.getItem(i));
			mAdapter.notifyDataSetChanged();
			
			if(result != null && ! mActivity.isFinishing())
				Toast.makeText(mActivity, result, Toast.LENGTH_SHORT).show();
		}
		
		@Override
		protected void onPreExecute() {
			sendMsg(mHandler, Messages.MSG_PROGRESS_SHOW, R.string.updating);
		}
		
		private String update() {
			logger.trace("update start");
			String result = null;
			
			int lastUpdate = (int) (new Date().getTime() / 60000);
			ArrayList<Timer> timers = null;
			
			// --- get timers from vdr ---
			try {
				timers = new Timers().getItems();
				TimerComparer comparator = new TimerComparer();
				Collections.sort(timers, comparator);
				// --- update timers ---
				for (int i = 0; i < mTimer.size(); i++) {
					Timer dst = mTimer.get(i);
					int index = Collections.binarySearch(timers, dst, comparator);
					if (index >= 0) {
						Timer src = timers.get(index);
						if (dst.number != src.number) {
							logger.trace(dst.title + " " + dst.number + " -> " + src.number);
						}
						dst.number = src.number;
						dst.status = src.status;
						dst.lastUpdate = lastUpdate;
					}
					else
						dst.lastUpdate = -1;
				}
				logger.trace("update done");
			} catch (IOException e) {
				logger.error("Couldn't update timers", e);
				result = e.getMessage();
			}
			return result;
		}
	}
	
	private class TimerComparer implements Comparator<Timer> {

		@Override
		public int compare(Timer a, Timer b) {
			Long l;
			Integer i;
			int result = a.title.compareTo(b.title);
			if (result == 0) {
				l = a.start;
				result = l.compareTo(b.start);
			}
			if (result == 0) {
				l = a.end;
				result = l.compareTo(b.end);
			}
			if (result == 0) {
				i = a.channel;
				result = i.compareTo(b.channel);
			}
			if (result == 0) {
				i = a.lifetime;
				result = i.compareTo(b.lifetime);
			}
			if (result == 0) {
				i = a.priority;
				result = i.compareTo(b.priority);
			}
			if (result == 0) {
				result = a.noDate.compareTo(b.noDate);
			}
			return result;
		}
		
	}
	
	private class TimerDeleteTask extends TimerBaseTask {
		
		@Override
		protected Response doIt(Timer timer) {
			if (timer.lastUpdate > 0) {
				Response response = VDRConnection.send(new DELT(timer.number));
				return response;
			}
			else
				return new ConnectionProblem(mActivity.getString(R.string.timer_not_found));
		}
	}
	
	private class TimerRecordTask extends AsyncTask<Timer, Void, Response> {
		
		@Override
		protected Response doInBackground(Timer... params) {
			Timer timer = params[0];
			Epg epg = new Epg();
			epg.kanal = timer.channel;
			epg.startzeit = timer.start;
			Long l = (timer.end - timer.start);
			epg.dauer = l.intValue();
			epg.titel = timer.title;
			return VdrCommands.setTimer(epg);
		}
		
		@Override
		protected void onPostExecute(Response result) {
			if (result.getCode() != 250)
				logger.error("Couldn't set timer: {}", result.getCode() + " - " + result.getMessage());
			Toast.makeText(mActivity, result.getCode() + " - " + result.getMessage().replaceAll("\n$", ""), 
					Toast.LENGTH_SHORT).show();
		}
	}
	
	private class TimerToggleTask extends TimerBaseTask {

		@Override
		protected Response doIt(Timer timer) {
			if (timer.lastUpdate > 0) {
				String s;
				if (timer.isActive())
					s = " OFF";
				else
					s = " ON";
				MODT modt = new MODT(timer.number, s);
				return VDRConnection.send(modt);
			} else
				return new ConnectionProblem(mActivity.getString(R.string.timer_not_found));
		}
	}
}
