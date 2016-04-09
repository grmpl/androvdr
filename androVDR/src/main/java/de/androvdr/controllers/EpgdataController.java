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
import java.util.Formatter;
import java.util.GregorianCalendar;

import org.hampelratte.svdrp.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import de.androvdr.ActionModeHelper;
import de.androvdr.Channel;
import de.androvdr.Channels;
import de.androvdr.Epg;
import de.androvdr.Messages;
import de.androvdr.Preferences;
import de.androvdr.R;
import de.androvdr.StreamInfo;
import de.androvdr.VdrCommands;
import de.androvdr.activities.AbstractFragmentActivity;

public class EpgdataController extends AbstractController {
	private static transient Logger logger = LoggerFactory.getLogger(EpgdataController.class);
	
	public static final int EPGDATA_ACTION_RECORD = 1;
	
	public String lastError;
	
	private ActionMode mActionMode;
	private final ScrollView mView;
	private final int mChannelNumber;
	private Channel mChannel;
	
	public EpgdataController(AbstractFragmentActivity activity, Handler handler,
			ScrollView view, int channelNumber) {
		super.onCreate(activity, handler);
		mView = view;
		
		mChannelNumber = channelNumber;
		try {
			mChannel = new Channels(Preferences.getVdr().channellist).getChannel(mChannelNumber);
			if (mChannel == null)
				throw new IOException("Couldn't get channel");
			
			showData();
		} catch (IOException e) {
			logger.error("Couldn't load channels", e);
			sendMsg(mHandler, Messages.MSG_ERROR, e.getMessage());
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			RelativeLayout rl = (RelativeLayout) mView.findViewById(R.id.pgi_layout_content);
			rl.setOnClickListener(getOnClickListener());
			rl.setOnLongClickListener(getOnLongClickListener());
		}
	}

	public void action(int action) {
		if (mChannel.viewEpg == null)
			return;

		switch (action) {
		case EPGDATA_ACTION_RECORD:
			new SetTimerTask().execute(mChannel.viewEpg);
			break;
		}
	}
	
	private OnClickListener getOnClickListener() {
		return new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (mActionMode != null) 
					ActionModeHelper.finish(mActionMode);
			}
		};
	}
	private OnLongClickListener getOnLongClickListener() {
		return new OnLongClickListener() {
			
			@Override
			public boolean onLongClick(View v) {
				if (mActionMode == null)
				mActionMode = mActivity.startActionMode(new ModeCallback());
				return true;
			}
		};
	}

	public String getTitle() {
		if (mChannel.viewEpg != null)
			return mChannel.viewEpg.titel;
		else
			return "";
	}
	
	private void showData() {
		Epg epg = mChannel.viewEpg;
		
		if (epg == null)
			return;

		setTextSize(mView);
		TextView tv = (TextView) mView.findViewById(R.id.header_text);
		if (tv != null){
			tv.setText(epg.titel);
		}
		tv = (TextView) mView.findViewById(R.id.pgi_title);
		if (tv != null){
			tv.setText(epg.titel);
		}
		tv = (TextView) mView.findViewById(R.id.pgi_shorttext);
		if (tv != null){
			tv.setText(epg.kurztext);
		}
		tv = (TextView) mView.findViewById(R.id.pgi_channel);
		if (tv != null){
			tv.setText(mChannel.name);
		}
		tv = (TextView) mView.findViewById(R.id.pgi_start);
		if (tv != null) {
			SimpleDateFormat dateformatter = new SimpleDateFormat(
					Preferences.dateformat);
			SimpleDateFormat timeformatter = new SimpleDateFormat(
					Preferences.timeformat);
			String[] weekdays = mActivity.getResources().getStringArray(
					R.array.weekday);
			GregorianCalendar calendar = new GregorianCalendar();
			StringBuilder sb = new StringBuilder();

			calendar.setTimeInMillis(epg.startzeit * 1000);
			sb.append(weekdays[calendar.get(Calendar.DAY_OF_WEEK) - 1]
					+ " " + dateformatter.format(calendar.getTime()) + " ");
			sb.append(timeformatter.format(calendar.getTime()));
			sb.append(" - ");
			calendar.setTimeInMillis(epg.startzeit * 1000 + epg.dauer
					* 1000);
			sb.append(timeformatter.format(calendar.getTime()));
			tv.setText(sb.toString());
		}
		tv = (TextView) mView.findViewById(R.id.pgi_duration);
		if (tv != null) {
			StringBuilder sb = new StringBuilder();
			new Formatter(sb).format("%02d:%02d", epg.dauer / 3600,
					(epg.dauer % 3600) / 60);
			tv.setText(sb.toString());
		}
		tv = (TextView) mView.findViewById(R.id.pgi_description);
		if (tv != null){
			tv.setText(epg.beschreibung);
		}

		TableLayout tb = (TableLayout) mView
				.findViewById(R.id.pgi_infotable);
		if (tb != null) {
			StreamInfo si = epg.getVideoStream();
			if (si != null)
				tb.addView(tableRow(mActivity.getString(R.string.videoformat), si));

			ArrayList<StreamInfo> asi = epg.getAudioStreams();
			if (asi != null)
				for (int i = 0; i < asi.size(); i++) {
					if (i == 0)
						tb.addView(tableRow(mActivity.getString(R.string.audiostreams), asi.get(i)));
					else
						tb.addView(tableRow("", asi.get(i)));
				}

			si = epg.getVideoType();
			if (si != null)
				tb.addView(tableRow(mActivity.getString(R.string.videoformat), si));

			si = epg.getAudioType();
			if (si != null)
				tb.addView(tableRow(mActivity.getString(R.string.audioformat), si));
		}
	}

	private TableRow tableRow(String title, StreamInfo streaminfo) {
		TableRow tr = new TableRow(mActivity);
		int px = (int) mActivity.getResources().getDisplayMetrics().density * 6;
		
		TextView tc = new TextView(mActivity);
		tc.setText(title);
		tr.addView(tc);
		
		tc = new TextView(mActivity);
		tc.setText(streaminfo.description);
		tc.setPadding(px, 0, 0, 0);
		tr.addView(tc);
		
		setTextSize(tr);
		return tr;
	}

	private final class ModeCallback implements ActionMode.Callback {
		
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			boolean result = false;
			
			switch (item.getItemId()) {
			case R.id.epg_record:
				action(EPGDATA_ACTION_RECORD);
				result = true;
			}
			mode.finish();
			return result;
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			MenuInflater inflater = mActivity.getMenuInflater();
			if (Preferences.blackOnWhite)
				inflater.inflate(R.menu.epg_menu_light, menu);
			else
				inflater.inflate(R.menu.epg_menu, menu);
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
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
			Response response = VdrCommands.setTimer(params[0]);
			return response;
		}

		@Override
		protected void onPostExecute(Response result) {
			if (result.getCode() != 250)
				logger.error("Couldn't set timer: {}", result.getCode());

			if (!mActivity.isFinishing())
				Toast.makeText(mActivity, result.getCode() + " - "
						+ result.getMessage().replaceAll("\n$", ""),
						Toast.LENGTH_LONG).show();
		}
	}
}