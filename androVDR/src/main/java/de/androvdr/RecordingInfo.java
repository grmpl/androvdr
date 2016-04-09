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

public class RecordingInfo {
	private StreamInfo mVideoStream;
	private ArrayList<StreamInfo> mAudioStreams;
	private StreamInfo mVideoType;
	private StreamInfo mAudioType;

	public String id;
	public String title;
	public String subtitle;
	public String description;
	public long date;
	public long duration;
	public String channelName;
	public int priority;
	public int lifetime;
	public String remark;
	
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
