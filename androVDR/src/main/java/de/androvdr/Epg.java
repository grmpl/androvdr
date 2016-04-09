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

package de.androvdr;

import java.util.ArrayList;
import java.util.Date;

public class Epg {
	private long lastUpdate = 0;
	private Double lastPercentDone = 0.0;
	
	private StreamInfo mVideoStream;
	private ArrayList<StreamInfo> mAudioStreams;
	private StreamInfo mVideoType;
	private StreamInfo mAudioType;

	public boolean isEmpty = false;
	public int kanal;
	public String titel, beschreibung, kurztext;
	public int dauer;
	public long startzeit;
	public long eventId;
	public long vps;
	public String channelName;
	
	public Epg() {}
	
	public Epg(int channel, boolean empty) {
		if (empty) {
			kanal = channel;
			isEmpty = true;
			startzeit = new Date().getTime() / 1000;
			dauer = 5 * 60;
		}
	}
	
	public void calculatePercentDone() {
		long now = new Date().getTime();
		if (lastUpdate == 0 || (now - lastUpdate) > (50 * 1000)) {
			lastUpdate = now;
			double start = startzeit / 60;
			double duration = dauer / 60;
			if (duration > 0)
				lastPercentDone = (((now / 1000 / 60) - start) / duration) * 100;
			else
				lastPercentDone = 0.0;
		}
	}

	@Override
	public boolean equals(Object o) {
		return (eventId == ((Epg)o).eventId);
	}
	
	public int getActualPercentDone() {
		return lastPercentDone.intValue();
	}
	
	public void addAudioStream(StreamInfo streaminfo) {
		if (mAudioStreams == null)
			mAudioStreams = new ArrayList<StreamInfo>();
		streaminfo.kind = StreamInfo.STREAM_KIND_AUDIO;
		mAudioStreams.add(streaminfo);
	}
	
	public void addAudioStream(String type, String language, String description) {
		if (mAudioStreams == null)
			mAudioStreams = new ArrayList<StreamInfo>();
		mAudioStreams.add(new StreamInfo(StreamInfo.STREAM_KIND_AUDIO, type, language, description));
	}
	
	public ArrayList<StreamInfo> getAudioStreams() {
		return mAudioStreams;
	}
	public StreamInfo getAudioType() {
		return mAudioType;
	}
	
	public StreamInfo getVideoStream() {
		return mVideoStream;
	}
	
	public StreamInfo getVideoType() {
		return mVideoType;
	}
	
	public void setAudioType(StreamInfo streaminfo) {
		mAudioType = streaminfo;
		mAudioType.kind = StreamInfo.STREAM_TYPE_AUDIO;
	}
	
	public void setAudioType(String type, String language, String description) {
		mAudioType = new StreamInfo(StreamInfo.STREAM_TYPE_AUDIO, type, language, description);
	}
	
	public void setVideoStream(StreamInfo streaminfo) {
		mVideoStream = streaminfo;
		mVideoStream.kind = StreamInfo.STREAM_KIND_VIDEO;
	}
	
	public void setVideoStream(String type, String language, String description) {
		mVideoStream = new StreamInfo(StreamInfo.STREAM_KIND_VIDEO, type, language, description);
	}
	
	public void setVideoType(StreamInfo streaminfo) {
		mVideoType = streaminfo;
		mVideoType.kind = StreamInfo.STREAM_TYPE_VIDEO;
	}
	
	public void setVideoType(String type, String language, String description) {
		mVideoType = new StreamInfo(StreamInfo.STREAM_TYPE_VIDEO, type, language, description);
	}
}
