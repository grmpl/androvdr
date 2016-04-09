package de.androvdr.fragments;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.List;

import org.hampelratte.svdrp.Response;
import org.hampelratte.svdrp.commands.LSTC;
import org.hampelratte.svdrp.parsers.ChannelParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import de.androvdr.Channel;
import de.androvdr.Preferences;
import de.androvdr.R;
import de.androvdr.devices.OnSensorChangeListener;
import de.androvdr.svdrp.VDRConnection;

public class RemoteTabletFragment extends RemoteFragment {
	private static transient Logger logger = LoggerFactory.getLogger(RemoteTabletFragment.class);
	private static final String MSG_RESULT = "result";
	private static final int SENSOR_DISKSTATUS = 1;
	private static final int SENSOR_CHANNEL = 2;

	private boolean mListenerInitialized = false;
	
	private String mLastDiskStatus = null;
	private Channel mLastChannelInfo = null;
	
	private Handler mSensorHandler = new Handler() {
		public void handleMessage(Message msg) {
			int type = msg.what;
			String result = msg.getData().getString(MSG_RESULT);
			
			switch (type) {
			case SENSOR_DISKSTATUS:
				updateDiskStatus(result);
				break;
			case SENSOR_CHANNEL:
				new ChannelViewUpdater().execute(result);
				break;
			}
		};
	};
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		logger.trace("onCreateView");
		View root = inflater.inflate(R.layout.remote_vdr_main, container, false);
		addClickListeners(root);
		
		View view = root.findViewById(R.id.remote_infoarea);
		if (view != null) {
			view.setOnClickListener(getOnClickListener());
		}

		if (mButtonTextSize > 0)
			setButtonsTextSize(root, mButtonTextSize);

		return root;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		logger.trace("onViewCreated");
		if (! mListenerInitialized) {
			if (view.findViewById(R.id.remote_channel_info) != null) {
				mDevices.addOnSensorChangeListener("VDR.channel", 1,
						new OnSensorChangeListener() {
							@Override
							public void onChange(String result) {
								logger.trace("Channel: {}", result);
								Message msg = Message.obtain(mSensorHandler,
										SENSOR_CHANNEL);
								Bundle bundle = new Bundle();
								bundle.putString(MSG_RESULT, result);
								msg.setData(bundle);
								msg.sendToTarget();
							}
				});
			}

			if (view.findViewById(R.id.remote_diskstatus_values) != null) {
				mDevices.addOnSensorChangeListener("VDR.disk", 5,
						new OnSensorChangeListener() {
							@Override
							public void onChange(String result) {
								logger.trace("DiskStatus: {}", result);
								Message msg = Message.obtain(mSensorHandler,
										SENSOR_DISKSTATUS);
								Bundle bundle = new Bundle();
								bundle.putString(MSG_RESULT, result);
								msg.setData(bundle);
								msg.sendToTarget();
							}
				});
			}
			
			mListenerInitialized = true;
			mDevices.startSensorUpdater(0);
		} else {
			if (mLastChannelInfo != null)
				updateChannelInfo(mLastChannelInfo);
			if (mLastDiskStatus != null)
				updateDiskStatus(mLastDiskStatus);
		}
	}

	public void updateChannelInfo(Channel channel) {
		mLastChannelInfo = channel;

		LinearLayout channelInfo = (LinearLayout) mActivity.findViewById(R.id.remote_channel_info);

		if (channelInfo == null)
			return;
		
		if (channel == null) {
			channelInfo.setVisibility(View.GONE);
		} else {
			channelInfo.setVisibility(View.VISIBLE);
			final SimpleDateFormat timeformatter = new SimpleDateFormat(
					Preferences.timeformat);
			final GregorianCalendar calendar = new GregorianCalendar();

			TextView tv = (TextView) mActivity.findViewById(R.id.channelnumber);
			ImageView iv = (ImageView) mActivity.findViewById(R.id.channellogo);

			if (Preferences.useLogos) {
				tv.setVisibility(View.GONE);
				iv.setVisibility(View.VISIBLE);
				iv.setImageBitmap(channel.logo);
			} else {
				tv.setVisibility(View.VISIBLE);
				iv.setVisibility(View.GONE);
				tv.setText(String.valueOf(channel.nr));
			}

			tv = (TextView) mActivity.findViewById(R.id.channeltext);
			tv.setText(channel.name);

			ProgressBar pb = (ProgressBar) mActivity.findViewById(R.id.channelprogress);
			if (channel.getNow().isEmpty) {
				pb.setProgress(0);
				tv = (TextView) mActivity.findViewById(R.id.channelnowplayingtime);
				tv.setText("");
				tv = (TextView) mActivity.findViewById(R.id.channelnowplaying);
				tv.setText("");
			} else {
				calendar.setTimeInMillis(channel.getNow().startzeit * 1000);
				pb.setProgress(channel.getNow().getActualPercentDone());
				tv = (TextView) mActivity.findViewById(R.id.channelnowplayingtime);
				tv.setText(timeformatter.format(calendar.getTime()));
				tv = (TextView) mActivity.findViewById(R.id.channelnowplaying);
				tv.setText(channel.getNow().titel);
			}

			if (channel.getNext().isEmpty) {
				tv = (TextView) mActivity.findViewById(R.id.channelnextplayingtime);
				tv.setText("");
				tv = (TextView) mActivity.findViewById(R.id.channelnextplaying);
				tv.setText("");
			} else {
				calendar.setTimeInMillis(channel.getNext().startzeit * 1000);
				tv = (TextView) mActivity.findViewById(R.id.channelnextplayingtime);
				tv.setText(timeformatter.format(calendar.getTime()));
				tv = (TextView) mActivity.findViewById(R.id.channelnextplaying);
				tv.setText(channel.getNext().titel);
			}
		}
	}

	public void updateDiskStatus(String result) {
		TextView tv = (TextView) mActivity.findViewById(R.id.remote_diskstatus_values);
		
		if (tv == null)
			return;
		
		if (! result.equals("N/A")) {
			try {
				String[] sa = result.split(" ");
				Integer total = Integer.parseInt(sa[0].replaceAll("MB$", "")) / 1024;
				int free = Integer.parseInt(sa[1].replaceAll("MB$", "")) / 1024;
				Integer used = total - free;

				tv.setText(used.toString() + " GB / " + total.toString() + " GB");

				ProgressBar pg = (ProgressBar) mActivity.findViewById(R.id.remote_diskstatus_progressbar);
				pg.setMax(total);
				pg.setProgress(used);
				mLastDiskStatus = result;
			} catch (Exception e) {
				logger.error("Couldn't parse disk status: {}", e);
				tv.setText("N/A");
			}
		}
	}
	
	private class ChannelViewUpdater extends AsyncTask<String, Void, Channel> {

		@Override
		protected Channel doInBackground(String... params) {
			int channelNumber;
			StringBuilder channelName = new StringBuilder();
			
			if (params[0].equalsIgnoreCase("N/A"))
				return null;
			
			try {
				String[] sa = params[0].split(" ");
				channelNumber = Integer.parseInt(sa[0]);
				for (int i = 1; i < sa.length; i++)
					channelName.append(sa[i] + " ");
			} catch (Exception e) {
				logger.error("Couldn't parse channel: {}", e);
				return null;
			}

		    Response response = VDRConnection.send(new LSTC(channelNumber));
			if(response.getCode() != 250) {
				logger.error("Couldn't get channel: {} {}", response.getCode(), response.getMessage());
				return null;
			}
			
			Channel channel;
			try {
				List<org.hampelratte.svdrp.responses.highlevel.Channel> channels = ChannelParser
						.parse(response.getMessage(), true);
				if (channels.size() > 0) {
					channel = new Channel(channels.get(0));
					channel.updateEpg(true);
					return channel;
				} else {
					logger.error("No channel found");
					return null;
				}
			} catch(ParseException pe) {
			    logger.error("Couldn't parse channel details", pe);
			    return null;
			} catch(IOException e) {
				logger.error("Couldn't get channel", e);
				return null;
			}
			
		}
		
		@Override
		protected void onPostExecute(Channel channel) {
			if (mActivity.isFinishing())
				return;
			
			updateChannelInfo(channel);
		}
	}
}
