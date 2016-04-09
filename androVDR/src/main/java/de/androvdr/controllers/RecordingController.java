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
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;

import org.hampelratte.svdrp.Response;
import org.hampelratte.svdrp.commands.DELR;
import org.hampelratte.svdrp.commands.PLAY;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SectionIndexer;
import android.widget.TextView;
import android.widget.Toast;
import de.androvdr.AbstractViewHolder;
import de.androvdr.ActionModeHelper;
import de.androvdr.DBHelper;
import de.androvdr.Messages;
import de.androvdr.Preferences;
import de.androvdr.R;
import de.androvdr.Recording;
import de.androvdr.RecordingInfo;
import de.androvdr.RecordingViewItem;
import de.androvdr.Recordings;
import de.androvdr.VdrCommands;
import de.androvdr.activities.AbstractFragmentActivity;
import de.androvdr.activities.RecordingInfoActivity;
import de.androvdr.svdrp.VDRConnection;

public class RecordingController extends AbstractController implements Runnable {
	public static final int RECORDING_ACTION_INFO = 1;
	public static final int RECORDING_ACTION_SORT_NAME = 2;
	public static final int RECORDING_ACTION_SORT_DATE = 3;
	public static final int RECORDING_ACTION_PLAY = 4;
	public static final int RECORDING_ACTION_PLAY_START = 5;
	public static final int RECORDING_ACTION_DELETE = 6;
	public static final int RECORDING_ACTION_REMOTE = 7;
	public static final int RECORDING_ACTION_KEY_BACK = 8;
	
	public interface OnRecordingSelectedListener {
		public boolean OnItemSelected(int position, Recording recording);
	}
	
	private static transient Logger logger = LoggerFactory.getLogger(RecordingController.class);
	
	private ActionMode mActionMode;
	private RecordingViewItemComparer mComparer;
	private int mCurrentSelectedItemIndex = -1;
	private String mDiskStatusResponse;
	private Boolean mDiskStatusResponseSync = true;
	private final ListView mListView;
	private RecordingViewItemList mRecordingViewItems = new RecordingViewItemList();
	private RecordingAdapter mRecordingAdapter;
	private Stack<RecordingViewItemList> mRecordingsStack = new Stack<RecordingViewItemList>();
	private OnRecordingSelectedListener mSelectedListener;
	private RecordingIdUpdateThread mUpdateThread = null;
	private AtomicBoolean mUpdateThreadFinished = new AtomicBoolean(false);

	// --- AsyncTasks ---
	RecordingInfoTask mRecordingTask = null;
	
	// --- needed by each row ---
	private final SimpleDateFormat datetimeformatter;
	private final GregorianCalendar calendar;

	private Handler mThreadHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.arg1) {
			case Messages.MSG_DONE:
				// --- set adapter ---
				mRecordingAdapter = new RecordingAdapter(mActivity,	mRecordingViewItems, mListView);
				setRecordingAdapter(mRecordingAdapter, mListView);
				new DiskStatusTask().execute();
				mHandler.sendMessage(Messages.obtain(Messages.MSG_PROGRESS_DISMISS));
				mHandler.sendMessage(Messages.obtain(Messages.MSG_CONTROLLER_READY));
				break;
			default:
				Message newMsg = new Message();
				newMsg.copyFrom(msg);
				mHandler.sendMessage(newMsg);
			}
		}
	};

	public RecordingController(AbstractFragmentActivity activity, Handler handler, ListView listView, Bundle bundle) {
		super.onCreate(activity, handler);
		mListView = listView;
		datetimeformatter = new SimpleDateFormat(Preferences.dateformatLong);
		calendar = new GregorianCalendar();
		
		RecordingViewItem[] recordings = null;
		
		if (bundle != null) {
			try {
				recordings = (RecordingViewItem[]) bundle.getParcelableArray("recordings");
			} catch (Exception e) {
				logger.error("restore SavedInstanceState", e);
			}
		}
		
		if (recordings != null) {
			mDiskStatusResponse = bundle.getString("diskstatus");
			
			// --- restore sort order ---
			mComparer = new RecordingViewItemComparer(bundle.getInt("sortby"));
			mComparer.ascending = bundle.getBoolean("sortascending");
			
			// --- restore view items ---
			mRecordingViewItems = new RecordingViewItemList();
			for (int i = 0; i < recordings.length; i++) {
				mRecordingViewItems.add((RecordingViewItem) recordings[i]);
				// mRecordingViewItems.get(i).recording.db = db;
			}
			
			// --- restore view item stack ---
			ArrayList<Parcelable> stack = bundle.getParcelableArrayList("stack");
			ArrayList<Integer> index = bundle.getIntegerArrayList("stackindex");
			int current = 0;
			for (int i = 0; i < index.size(); i++) {
				int last = index.get(i);
				RecordingViewItemList list = new RecordingViewItemList();
				for (int j = current; j < last; j++) {
					list.add((RecordingViewItem) stack.get(j));
				}
				mRecordingsStack.add(list);
			}
			
			mRecordingAdapter = new RecordingAdapter(mActivity, mRecordingViewItems, mListView);
			setRecordingAdapter(mRecordingAdapter, mListView);
			showDiskStatus();
			sendMsg(mHandler, Messages.MSG_CONTROLLER_READY, null);
		}
		else {
			sendMsg(mHandler, Messages.MSG_CONTROLLER_LOADING, R.string.loading);
			sendMsg(mHandler, Messages.MSG_PROGRESS_SHOW, R.string.loading);

			Thread thread = new Thread(this);
			thread.start();
		}
	}
	
	public void action(int action) {
		if (mUpdateThread != null)
			mUpdateThread.interrupt();

		switch (action) {
		case RECORDING_ACTION_KEY_BACK:
			if (mRecordingsStack.empty())
				mActivity.finish();
			else {
				unselectItem();
				
				// --- restore adapter state ---
				RecordingViewItemList list = mRecordingsStack.pop();
				mRecordingAdapter.clear();
				if (list.size() > 0) {
					for (int i = 0; i < list.size(); i++)
						mRecordingAdapter.add(list.get(i));
					mRecordingAdapter.sort(mComparer);
				}
				else if (! mRecordingsStack.empty())
					action(RECORDING_ACTION_KEY_BACK);
			}
			break;
		case RECORDING_ACTION_REMOTE:
			mActivity.finish();
			break;
		case RECORDING_ACTION_SORT_DATE:
			if (mComparer == null || mComparer.compareBy != RECORDING_ACTION_SORT_DATE)
				mComparer =  new RecordingViewItemComparer(RECORDING_ACTION_SORT_DATE);
			else
				mComparer.ascending = ! mComparer.ascending;
			mRecordingAdapter.sort(mComparer);
			break;
		case RECORDING_ACTION_SORT_NAME:
			if (mComparer == null || mComparer.compareBy != RECORDING_ACTION_SORT_NAME)
				mComparer =  new RecordingViewItemComparer(RECORDING_ACTION_SORT_NAME);
			else
				mComparer.ascending = ! mComparer.ascending;
			mRecordingAdapter.sort(mComparer);
			break;
		}
	}
	
	public void action(final int action, int position) {
		if (mUpdateThread != null)
			mUpdateThread.interrupt();

		if (action == RECORDING_ACTION_INFO && position == -1) {
			unselectItem();
			return;
		}
		
		if (position >= mRecordingAdapter.getCount())
			return;
		
		final RecordingViewItem item = mRecordingAdapter.getItem(position);
		
		switch (action) {
		case RECORDING_ACTION_DELETE:
			mRecordingTask = new RecordingDeleteTask();
			mRecordingTask.execute(item);
			break;
		case RECORDING_ACTION_INFO:
			if (item.isFolder) {
				unselectItem();
				
				// --- save current items ---
				RecordingViewItemList list = new RecordingViewItemList();
				for (int i = 0; i < mRecordingAdapter.getCount(); i++)
					list.add(mRecordingAdapter.getItem(i));
				mRecordingsStack.add(list);

				// --- fill adapter ---
				mRecordingAdapter.clear();
				for (int i = 0; i < item.folderItems.size(); i++)
					mRecordingAdapter.add(item.folderItems.get(i));
				
				// --- apply last sort criterion ---
				mRecordingAdapter.sort(mComparer);
			}
			else {
				mRecordingTask = new RecordingInfoTask();
				mRecordingTask.execute(item);
			}
			break;
			
		case RECORDING_ACTION_PLAY:
		case RECORDING_ACTION_PLAY_START:
			mRecordingTask = new RecordingPlayTask(action == RECORDING_ACTION_PLAY_START);
			mRecordingTask.execute(item);
			break;
		}
	}
	
	public String getTitle(int position) {
		RecordingViewItem item = mRecordingAdapter.getItem(position);
		return item.title;
	}

	private OnItemClickListener getOnItemClickListener() {
		return new OnItemClickListener() {
			public void onItemClick(AdapterView<?> listView, View v,
					int position, long ID) {
				if (!mActivity.isDualPane())
					mListView.setItemChecked(position, false);
				if (mActionMode != null) {
					if (mActivity.isDualPane())
						action(RECORDING_ACTION_INFO, position);
					ActionModeHelper.finish(mActionMode);
				} else {
					mCurrentSelectedItemIndex = position;
					action(RECORDING_ACTION_INFO, position);
				}
			}
		};
	}

	private OnItemLongClickListener getOnItemLongClickListener() {
		return new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> listView, View v,
					int position, long ID) {
				if (position >= mRecordingAdapter.getCount())
					return false;
				
				RecordingViewItem rv = mRecordingAdapter.getItem(position);
				if (rv.isFolder)
					return false;
				
				mCurrentSelectedItemIndex = position;
				mListView.setItemChecked(position, true);
				if (mActivity.isDualPane())
					action(RECORDING_ACTION_INFO, position);
				
				if (mActionMode == null)
					mActionMode = mActivity.startActionMode(new ModeCallback());
				return true;
			}
		};
	}

	public RecordingViewItem[] getRecordingViewItems() {
		RecordingViewItem[] items = new RecordingViewItem[mRecordingViewItems.size()];
		int count = 0;
		for (RecordingViewItem rvi : mRecordingViewItems)
			items[count++] = rvi;
		return items;
	}
	
	public boolean isFolder(int position) {
		RecordingViewItem item = mRecordingAdapter.getItem(position);
		return item.isFolder;
	}

	public void onPause() {
		logger.trace("onPause");
		if (mUpdateThread != null) {
			mUpdateThread.interrupt();
		}
		if (mRecordingTask != null) {
			mRecordingTask.cancel(true);
			mRecordingTask = null;
		}
	}
	
	public void onResume() {
		logger.trace("onResume");
		if (mUpdateThread != null)
			mUpdateThread = new RecordingIdUpdateThread(mThreadHandler);
	}
	
    public void onSaveInstanceState(Bundle outState) {
    	outState.putString("diskstatus", mDiskStatusResponse);
    	outState.putParcelableArray("recordings", getRecordingViewItems());
    	if (mComparer == null) {
    		outState.putInt("sortby", RECORDING_ACTION_SORT_NAME);
    		outState.putBoolean("sortascending", true);
    	} else {
    		outState.putInt("sortby", mComparer.compareBy);
    		outState.putBoolean("sortascending", mComparer.ascending);
    	}
    	ArrayList<RecordingViewItem> stack = new ArrayList<RecordingViewItem>();
    	ArrayList<Integer> index = new ArrayList<Integer>();
    	for (int i = 0; i < mRecordingsStack.size(); i++) {
    		RecordingViewItemList list = mRecordingsStack.get(i);
    		for (RecordingViewItem item : list)
    			stack.add(item);
    		index.add(stack.size());
    	}
    	outState.putParcelableArrayList("stack", stack);
    	outState.putIntegerArrayList("stackindex", index);
    }
    
	@Override
	public void run() {
		try {
			Recordings recordings = new Recordings();
			for(RecordingViewItem recordingViewItem: recordings.getItems()) {
				mRecordingViewItems.add(recordingViewItem);
			}
			sendMsg(mThreadHandler, Messages.MSG_DONE, null);
		} catch (IOException e) {
			logger.error("Couldn't read recordings", e);
			sendMsg(mThreadHandler, Messages.MSG_ERROR, e.getMessage());
		}
	}
	
	public void setOnRecordingSelectedListener(OnRecordingSelectedListener listener) {
		mSelectedListener = listener;
	}
	
	private void setRecordingAdapter(RecordingAdapter adapter, ListView listView) {
		if (mComparer == null)
			action(RECORDING_ACTION_SORT_NAME);
		else {
			adapter.sort(mComparer);
		}
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(getOnItemClickListener());
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
			listView.setOnItemLongClickListener(getOnItemLongClickListener());
		listView.setSelected(true);
		listView.setSelection(0);
		mUpdateThread = new RecordingIdUpdateThread(mThreadHandler);
	}

	private void showDiskStatus() {
		// --- disk status ---
		LinearLayout lay = (LinearLayout) mListView.getParent();
		if (lay != null && lay.findViewById(R.id.recdiskstatus) != null) {
			TextView tv = (TextView) lay.findViewById(R.id.recdiskstatus_values);
			synchronized (mDiskStatusResponseSync) {
				if (mDiskStatusResponse != null) {
					try {
						String[] sa = mDiskStatusResponse.split(" ");
						Integer total = Integer.parseInt(sa[0].replaceAll("MB$", "")) / 1024;
						Integer free = Integer.parseInt(sa[1].replaceAll("MB$", "")) / 1024;
						Integer used = total - free;
						
						tv.setText(used.toString() + " GB / " + total.toString() + " GB");
						
						ProgressBar pg = (ProgressBar) lay.findViewById(R.id.recdiskstatus_progressbar);
						pg.setMax(total);
						pg.setProgress(used);
					} catch (Exception e) {
						logger.error("Couldn't parse disk status: {}", mDiskStatusResponse);
						tv.setText("N/A");
					}
				} else {
					tv.setText("N/A");
				}
			}
		}
	}

	private void unselectItem() {
		mListView.setItemChecked(mCurrentSelectedItemIndex, false);
		mCurrentSelectedItemIndex = -1;
		mSelectedListener.OnItemSelected(-1, null);
	}

	private final class ModeCallback implements ActionMode.Callback {

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			boolean result = false;
			
			switch (item.getItemId()) {
			case R.id.rec_play:
				action(RECORDING_ACTION_PLAY, mCurrentSelectedItemIndex);
				result = true;
				break;
			case R.id.rec_play_start:
				action(RECORDING_ACTION_PLAY_START, mCurrentSelectedItemIndex);
				result = true;
				break;
			case R.id.rec_delete:
				AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
				builder.setMessage(R.string.rec_delete_recording)
				       .setCancelable(false)
				       .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
				           public void onClick(DialogInterface dialog, int id) {
				        	   dialog.dismiss();
				        	   action(RECORDING_ACTION_DELETE, mCurrentSelectedItemIndex);
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
			}
			mode.finish();
			return result;
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			MenuInflater inflater = mActivity.getMenuInflater();
			if (Preferences.blackOnWhite)
				inflater.inflate(R.menu.recordings_menu_light, menu);
			else
				inflater.inflate(R.menu.recordings_menu, menu);
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

	private class DiskStatusTask extends AsyncTask<Void, Void, String> {
		
		@Override
		protected String doInBackground(Void... params) {
			return Preferences.getVdr().read("disk");
		}
		
		@Override
		protected void onPostExecute(String result) {
			synchronized (mDiskStatusResponseSync) {
				mDiskStatusResponse = result;
			}
			showDiskStatus();
		}
	}
	
	private class RecordingAdapter extends ArrayAdapter<RecordingViewItem> implements SectionIndexer {
		private final Activity mActivity;
		private final ListView mListView;
		
		private RecordingViewItemComparer mComparer;
		private HashMap<String, Integer> mIndexer;
		private String[] mSections;
		
		private class ViewHolder extends AbstractViewHolder {
			public LinearLayout folder;
			public RelativeLayout recording;
			public TextView foldertitle;
			public TextView date;
			public ImageView state;
			public TextView title;
		}
		
		public RecordingAdapter(Activity activity, ArrayList<RecordingViewItem> recording, ListView listView) {
			super(activity, R.layout.recordings_item, recording);
			mActivity = activity;
			mListView = listView;
		}
		
		@Override
		public int getPositionForSection(int section) {
			if (mSections.length == 0 || section >= mSections.length)
				return 0;
			
			logger.trace("getPositionForSection {} = {}", section, mIndexer.get(mSections[section]));
			return mIndexer.get(mSections[section]);
		}

		@Override
		public int getSectionForPosition(int position) {
			logger.trace("getSectionForPosition {}", position);
			return 0;
		}

		@Override
		public Object[] getSections() {
			logger.trace("sections = {}", mSections.length);
			return mSections;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View row;
			if (convertView == null) {
				LayoutInflater inflater = mActivity.getLayoutInflater();
				row = inflater.inflate(R.layout.recordings_item, null);
				
				ViewHolder vh = new ViewHolder();
				vh.folder = (LinearLayout) row.findViewById(R.id.recording_folder);
				vh.recording = (RelativeLayout) row.findViewById(R.id.recording_recording);
				vh.foldertitle = (TextView) row.findViewById(R.id.recording_foldertitle);
				vh.date = (TextView) row.findViewById(R.id.recording_date);
				vh.state = (ImageView) row.findViewById(R.id.recording_stateimage);
				vh.title = (TextView) row.findViewById(R.id.recording_title);
				vh.setTextSize(Preferences.textSizeOffset,
						mActivity.getResources().getDisplayMetrics().scaledDensity);
				row.setTag(vh);
			} else {
				row = convertView;
			}
			
			RecordingViewItem item = this.getItem(position);
			ViewHolder vh = (ViewHolder) row.getTag();
			
			if (item.isFolder) {
				vh.folder.setVisibility(View.VISIBLE);
				vh.recording.setVisibility(View.GONE);

				vh.foldertitle.setText(item.title);
			}
			else {
				vh.folder.setVisibility(View.GONE);
				vh.recording.setVisibility(View.VISIBLE);

				calendar.setTimeInMillis(item.recording.date * 1000);
				vh.date.setText(datetimeformatter.format(calendar.getTime()));

				if (! item.recording.isNew)
					vh.state.setImageResource(R.drawable.presence_online);
				else
					vh.state.setImageDrawable(null);
				
				vh.title.setText(item.recording.title);
			}
			return row;
		}

		private void initIndexer() {
			if (mComparer.compareBy == RECORDING_ACTION_SORT_NAME) {
				logger.trace("initialize indexer");
				
				mIndexer = new HashMap<String, Integer>();
				int size = getCount();
				for (int i = size - 1; i >= 0; i--) {
					RecordingViewItem item = getItem(i);
					if (! item.isFolder) {
						if (item.recording.title.length() > 0)
							mIndexer.put(item.recording.title.substring(0, 1)
									.toUpperCase(), i);
						else
							mIndexer.put(" ", i);
					} else {
						if (mComparer.ascending)
							mIndexer.put("A", i);
						else
							mIndexer.put("Z", i);
					}
				}
				
				Set<String> keys = mIndexer.keySet();
				Iterator<String> it = keys.iterator();
				ArrayList<String> keyList = new ArrayList<String>();
				while (it.hasNext()) {
					keyList.add(it.next());
				}
				Collections.sort(keyList);
				if (! mComparer.ascending)
					Collections.reverse(keyList);
				
				mSections = new String[keyList.size()];
				keyList.toArray(mSections);
				
				mListView.setFastScrollEnabled(true);
				jiggleWidth();
				logger.trace("fastscroll with indexer enabled");
			} else {
				mSections = new String[0];
				mListView.setFastScrollEnabled(true);
				logger.trace("fastscroll enabled");
			}
		}

		private boolean FLAG_THUMB_PLUS = false;
		private void jiggleWidth() {
		    ListView view = mListView;
		    if (view.getWidth() <= 0)
		        return;

		    int newWidth = FLAG_THUMB_PLUS ? view.getWidth() - 1 : view.getWidth() + 1;
		    ViewGroup.LayoutParams params = view.getLayoutParams();
		    params.width = newWidth;
		    view.setLayoutParams( params );

		    FLAG_THUMB_PLUS = !FLAG_THUMB_PLUS;
		}
		
		@Override
		public void remove(RecordingViewItem object) {
			mListView.setFastScrollEnabled(false);
			super.remove(object);
			logger.trace("RecordingAdapter.remove: {}", object.recording.number);
			initIndexer();
		}
		
		@Override
		public void sort(Comparator<? super RecordingViewItem> comparator) {
			mListView.setFastScrollEnabled(false);
			super.sort(comparator);

			RecordingViewItemComparer comparer = (RecordingViewItemComparer) comparator;
			mComparer = comparer;
			initIndexer();
		}
	}
	
	private class RecordingDeleteTask extends RecordingInfoTask {
		private boolean mDoUnselectItem = false;
		
		@Override
		protected String doIt() {
			if (mCurrentSelectedItemIndex >= 0 && mCurrentSelectedItemIndex < mListView.getCount()) {
				RecordingViewItem rvi = (RecordingViewItem) mListView
						.getItemAtPosition(mCurrentSelectedItemIndex);
				if (rvi.recording.id.equals(mRecording.id))
					mDoUnselectItem = true;
			}
		    Response response = VDRConnection.send(new DELR(mRecording.number));
	        return response.getCode() + " - " + response.getMessage().replaceAll("\n$", "");
		}
		
		@Override
		protected void onPostExecute(String result) {
			if (result.startsWith("250", 0)) {
				mRecordingAdapter.remove(mRecordingViewItem);
				if (! mRecordingsStack.empty()) {
					RecordingViewItemList list = mRecordingsStack.lastElement();
					for (int i = 0; i < list.size(); i++)
						if (list.get(i).isFolder) {
							list.get(i).folderItems.remove(mRecordingViewItem);
						}
				}
				if (mDoUnselectItem)
					unselectItem();
			}
			new DiskStatusTask().execute();
			super.onPostExecute(result);
		}
	}
	
	private class RecordingIdUpdateThread extends Thread {
		private final Handler mHandler;
		private final DBHelper mDbHelper = new DBHelper(mActivity);
		
		public RecordingIdUpdateThread(Handler handler) {
			mHandler = handler;
			start();
		}
		
		@Override
		public void run() {
			synchronized (mUpdateThreadFinished) {
				if (mUpdateThreadFinished.get())
					return;
			}
			
			logger.trace("UpdateThread started");
			sendMsg(mHandler, Messages.MSG_TITLEBAR_PROGRESS_SHOW, null);

			SQLiteDatabase database = mDbHelper.getWritableDatabase();
			try {
				if (Preferences.deleteRecordingIds && ! Preferences.useInternet) {
					Recordings.clearIds();
					for(Recording recording: mRecordingViewItems.getAllRecordings()) {
						recording.setInfoId(null, database);
					}
					Preferences.deleteRecordingIds = false;
					Preferences.store();
				}
				
				try {
					for (Recording recording: mRecordingViewItems.getAllRecordings()) {
						if (isInterrupted()) {
							logger.trace("UpdateThread interrupted");
							return;
						}
						if (recording.getInfoId(database) == null) {
							RecordingInfo info = VdrCommands.getRecordingInfo(recording.number);
							if (isInterrupted()) {
								logger.trace("UpdateThread interrupted");
								return;
							}
							recording.setInfoId(info.id, database);
							logger.trace("Set id {} --> infoId {}", recording.id, info.id);
						}
					}
					
					if (Preferences.doRecordingIdCleanUp) {
						Recordings.deleteUnusedIds(mRecordingViewItems.getAllRecordings());
						Preferences.doRecordingIdCleanUp = false;
					}
					
					synchronized (mUpdateThreadFinished) {
						mUpdateThreadFinished.getAndSet(true);
					}
				} catch (IOException e) {
					logger.error("Couldn't update recording ids", e);
				} finally {
					sendMsg(mHandler, Messages.MSG_TITLEBAR_PROGRESS_DISMISS, null);
				}
			} finally {
				database.close();
			}
			logger.trace("UpdateThread finished");
		}
	}

	private class RecordingInfoTask extends AsyncTask<RecordingViewItem, Void, String> {
		protected Recording mRecording;
		protected RecordingViewItem mRecordingViewItem;
		protected RecordingInfo mInfo;
		
		protected void onPreExecute() {
			sendMsg(mHandler, Messages.MSG_PROGRESS_SHOW, R.string.searching);
		}

		@Override
		protected String doInBackground(RecordingViewItem... params) {
			final SQLiteDatabase database = new DBHelper(mActivity).getWritableDatabase();
			
			mRecordingViewItem = params[0];
			mRecording = mRecordingViewItem.recording;
			try {
				mInfo = VdrCommands.getRecordingInfo(mRecording.number);
				logger.trace("MD5: " + mRecording.getInfoId(database) + " --- " + mInfo.id);
				
				if (mRecording.getInfoId(database) != null
						&& mRecording.getInfoId(database).compareTo(mInfo.id) == 0) {
					if (mRecording.number < 0)
						return mActivity.getString(R.string.rec_not_found);
					else
						return doIt();
				} else {
					onProgressUpdate();
					mRecordingViewItems.update();
					ArrayList<Recording> allRecordings = mRecordingViewItems.getAllRecordings();
					int index = Collections.binarySearch(allRecordings, mRecording);
					if (index >= 0) {
						Recording foundRecording = allRecordings.get(index);
						if (foundRecording.number < 0)
							return mActivity.getString(R.string.rec_not_found);
						else
							return doIt(foundRecording, database);
					} else {
						return mActivity.getString(R.string.rec_not_found);
					}
				} 
			} catch (IOException e) {
				logger.error("Couldn't get recording info", e);
				return e.getMessage();
			} finally {
				database.close();
			}
		}
		
		protected String doIt() {
			int position = mRecordingAdapter.getPosition(mRecordingViewItem);
			if (mSelectedListener == null || ! mSelectedListener.OnItemSelected(position, mRecording)) {
				Intent intent = new Intent(mActivity, RecordingInfoActivity.class);
				intent.putExtra("recordingnumber", mRecording.number);
				mActivity.startActivityForResult(intent, 1);
			} else {
				if (mUpdateThread != null)
					mUpdateThread = new RecordingIdUpdateThread(mThreadHandler);
			}
			return "";
		}

		protected String doIt(Recording recording, SQLiteDatabase database) throws IOException {
			mInfo = VdrCommands.getRecordingInfo(recording.number);
			recording.setInfoId(mInfo.id, database);
			mRecording = recording;
			return doIt();
		}
		
		protected void onProgressUpdate(Void... values) {
			sendMsg(mHandler, Messages.MSG_PROGRESS_UPDATE, R.string.updating);
		}
		
		protected void onPostExecute(String result) {
			sendMsg(mHandler, Messages.MSG_PROGRESS_DISMISS, null);
			mRecordingAdapter.notifyDataSetChanged();
			if (result != "")
				Toast.makeText(mActivity, result, Toast.LENGTH_SHORT).show();
		}
	}
	
	private class RecordingPlayTask extends RecordingInfoTask {
		private boolean mFromBeginning = false;
		
		public RecordingPlayTask(boolean fromBeginning) {
			mFromBeginning = fromBeginning;
		}
		
		protected String doIt() {
		    PLAY play = new PLAY(mRecording.number);
		    if(mFromBeginning) {
		        play.setStartTime(PLAY.BEGIN);
		    }
		    
		    Response response = VDRConnection.send(play);
		    if(response.getCode() == 250) {
		        mActivity.finish();
		        return "";
		    } else {
		        return response.getCode() + " - " + response.getMessage().replaceAll("\n$", "");
		    }
		}
	}
	
	private class RecordingViewItemList extends ArrayList<RecordingViewItem> {
		private static final long serialVersionUID = -4365980877168388915L;
		
		private ArrayList<Recording> mRecordings = new ArrayList<Recording>();
		private boolean mIsSorted = false;
		
		@Override
		public boolean add(RecordingViewItem recordingViewItem) {
			boolean result = super.add(recordingViewItem);
			if (result && recordingViewItem.isFolder) 
				initRecordings(recordingViewItem.folderItems);
			else {
				if (mRecordings.indexOf(recordingViewItem.recording) < 0)
					mRecordings.add(recordingViewItem.recording);
			}
			mIsSorted = false;
			return result;
		}
		
		public ArrayList<Recording> getAllRecordings() {
			if (! mIsSorted) {
				Collections.sort(mRecordings);
				mIsSorted = true;
			}
			return mRecordings;
		}
		
		private void initRecordings(ArrayList<RecordingViewItem> list) {
			for (int i = 0; i < list.size(); i++) {
				RecordingViewItem item = list.get(i);
				if (item.isFolder)
					initRecordings(item.folderItems);
				else {
					if (mRecordings.indexOf(item.recording) < 0)
						mRecordings.add(item.recording);
				}
			}
		}
		
		public void update() throws IOException {
			logger.trace("updateRecordings started");
			// --- get recordings from vdr ---
			Recordings recordings = new Recordings();
			RecordingViewItemList recordingViewItems = new RecordingViewItemList();
			for(RecordingViewItem recordingViewItem: recordings.getItems())
				recordingViewItems.add(recordingViewItem);
			ArrayList<Recording> allRecordings = recordingViewItems.getAllRecordings();
			// --- update recordings ---
			for (int i = 0; i < mRecordings.size(); i++) {
				Recording dst = mRecordings.get(i);
				int index = Collections.binarySearch(allRecordings, dst);
				if (index >= 0) {
					Recording src = allRecordings.get(index);
					if (dst.number != src.number) {
						logger.trace(dst.fullTitle + " " + dst.number + " -> " + src.number);
					}
					dst.number = src.number;
					dst.isNew = src.isNew;
				}
				else
					dst.number = -1;
			}
			logger.trace("updateRecordings finished");
		}
	}

	private class RecordingViewItemComparer implements java.util.Comparator<RecordingViewItem> {
		public boolean ascending = true;
		public final int compareBy;
		
		public RecordingViewItemComparer(int compareBy) {
			this.compareBy = compareBy;
		}
		
		public int compare(RecordingViewItem a, RecordingViewItem b) {
			switch (compareBy) {
			case RECORDING_ACTION_SORT_DATE:
				return compareByDate(a, b);
			case RECORDING_ACTION_SORT_NAME:
				return compareByName(a, b);
			}
			return 0;
		}

		public int compareByDate(RecordingViewItem a, RecordingViewItem b) {
			if (a.isFolder || b.isFolder) {
				if (a.isFolder && ! b.isFolder)
					return -1;
				else if (! a.isFolder && b.isFolder)
					return 1;
				else
					return a.title.compareToIgnoreCase(b.title);
			}
			else {
				int result;
				Long l = a.recording.date;
				result = l.compareTo(b.recording.date);
				if (!ascending && result != 0) {
					result = result - result * 2;
				}
				return result;
			}
		}
		
		public int compareByName(RecordingViewItem a, RecordingViewItem b) {
			if (a.isFolder || b.isFolder) {
				if (a.isFolder && ! b.isFolder)
					return -1;
				else if (! a.isFolder && b.isFolder)
					return 1;
				else
					return a.title.compareToIgnoreCase(b.title);
			}
			else {
				int result = a.title.compareToIgnoreCase(b.title);
				if (result == 0) {
					Long l = a.recording.date;
					result = l.compareTo(b.recording.date);
				}
				if (!ascending && result != 0) {
					result = result - result * 2;
				}
				return result;
			}
		}
	}
}