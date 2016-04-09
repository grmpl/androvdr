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
import java.util.Formatter;
import java.util.GregorianCalendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.os.Handler;
import android.os.Message;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import de.androvdr.Messages;
import de.androvdr.Preferences;
import de.androvdr.R;
import de.androvdr.RecordingInfo;
import de.androvdr.StreamInfo;
import de.androvdr.VdrCommands;
import de.androvdr.activities.AbstractFragmentActivity;

public class RecordingInfoController extends AbstractController implements Runnable {
	private static transient Logger logger = LoggerFactory.getLogger(RecordingInfoController.class);
	
	public static final int RECORDINGINFO_ACTION_PLAY = 1;
	public static final int RECORDINGINFO_ACTION_PLAY_START = 2;
	public static final int RECORDINGINFO_ACTION_DELETE = 3;
	
	private RecordingInfo mRecordingInfo;
	private final int mRecordingNumber;
	private final LinearLayout mView;

	public String lastError;
	
	private Handler mThreadHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.arg1) {
			case Messages.MSG_DONE:
				showData(mRecordingInfo);
				mHandler.sendMessage(Messages.obtain(Messages.MSG_PROGRESS_DISMISS));
				break;
			}
		}
	};

	public RecordingInfoController(AbstractFragmentActivity activity, Handler handler,
			LinearLayout view, int recordingNumber) {
		super.onCreate(activity, handler);
		mView = view;
		mRecordingNumber = recordingNumber;
		mActivity.registerForContextMenu(mView.findViewById(R.id.reci_layout_content));
		
		Message msg = Messages.obtain(Messages.MSG_PROGRESS_SHOW);
		msg.arg2 = R.string.loading;
		mHandler.sendMessage(msg);
		Thread thread = new Thread(this);
		thread.start();
	}

	public CharSequence getTitle() {
		return mRecordingInfo.title;
	}

	private void showData(RecordingInfo recordingInfo) {
		if (recordingInfo == null)
			return;

		setTextSize(mView);
		TextView tv = (TextView) mView.findViewById(R.id.header_text);
		if (tv != null)
			tv.setText(recordingInfo.title);

		tv = (TextView) mView.findViewById(R.id.reci_channel);
		if (tv != null){
			tv.setText(recordingInfo.channelName);
		}
		tv = (TextView) mView.findViewById(R.id.reci_title);
		if (tv != null){
			tv.setText(recordingInfo.title);
		}
		tv = (TextView) mView.findViewById(R.id.reci_shorttext);
		if (tv != null){
			tv.setText(recordingInfo.subtitle);
		}
		tv = (TextView) mView.findViewById(R.id.reci_start);
		if (tv != null) {
			SimpleDateFormat dateformatter = new SimpleDateFormat(Preferences.dateformatLong);
			GregorianCalendar calendar = new GregorianCalendar();
			StringBuilder sb = new StringBuilder();

			calendar.setTimeInMillis(recordingInfo.date * 1000);
			sb.append(dateformatter.format(calendar.getTime()));
			tv.setText(sb.toString());
		}
		tv = (TextView) mView.findViewById(R.id.reci_durationtext);
		if (tv != null){
			tv.setText(R.string.duration);
		}
		
		tv = (TextView) mView.findViewById(R.id.reci_duration);
		if (tv != null) {
			StringBuilder sb = new StringBuilder();
			new Formatter(sb).format("%02d:%02d", recordingInfo.duration / 3600,
					(recordingInfo.duration % 3600) / 60);
			tv.setText(sb.toString());
		}
		tv = (TextView) mView.findViewById(R.id.reci_description);
		if (tv != null){
			tv.setText(recordingInfo.description);
		}
		
		TableLayout tb = (TableLayout) mView.findViewById(R.id.reci_infotable);
		if (tb != null) {
			StreamInfo si = recordingInfo.getVideoStream();
			if (si != null)
				tb.addView(tableRow(mActivity.getString(R.string.videostream), si.description));
		
			ArrayList<StreamInfo> asi = recordingInfo.getAudioStreams();
			if (asi != null)
				for (int i = 0; i < asi.size(); i++) {
					if (i == 0)
						tb.addView(tableRow(mActivity.getString(R.string.audiostreams), asi.get(i).description));
					else
						tb.addView(tableRow("", asi.get(i).description));
				}
		
			si = recordingInfo.getVideoType();
			if (si != null)
				tb.addView(tableRow(mActivity.getString(R.string.videoformat), si.description));
		
			si = recordingInfo.getAudioType();
			if (si != null)
				tb.addView(tableRow(mActivity.getString(R.string.audioformat), si.description));
			
			tb.addView(tableRow("", ""));
			tb.addView(tableRow(mActivity.getString(R.string.priority), String.valueOf(recordingInfo.priority)));
			tb.addView(tableRow(mActivity.getString(R.string.lifetime), String.valueOf(recordingInfo.lifetime)));
			
			tv = (TextView) mView.findViewById(R.id.reci_remark);
			if (tv != null){
				tv.setText(recordingInfo.remark);
			}
		}
	}

	private TableRow tableRow(String title, String value) {
		TableRow tr = new TableRow(mActivity);
		int px = (int) mActivity.getResources().getDisplayMetrics().density * 6;
		
		TextView tc = new TextView(mActivity);
		tc.setText(title);
		tr.addView(tc);
		
		tc = new TextView(mActivity);
		tc.setText(value);
		tc.setPadding(px, 0, 0, 0);
		tr.addView(tc);

		setTextSize(tr);
		return tr;
	}

	@Override
	public void run() {
		try {
			mRecordingInfo = VdrCommands.getRecordingInfo(mRecordingNumber);
			mThreadHandler.sendMessage(Messages.obtain(Messages.MSG_DONE));
		} catch (IOException e) {
			logger.error("Couldn't read recording info", e);
			sendMsg(mHandler, Messages.MSG_ERROR, e.getMessage());
		}
	}
}
