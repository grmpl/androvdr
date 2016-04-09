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

import java.io.IOException;
import java.text.ParseException;
import java.util.GregorianCalendar;
import java.util.List;

import org.hampelratte.svdrp.Response;
import org.hampelratte.svdrp.commands.LSTR;
import org.hampelratte.svdrp.commands.NEWT;
import org.hampelratte.svdrp.parsers.RecordingParser;
import org.hampelratte.svdrp.parsers.TimerParser;
import org.hampelratte.svdrp.responses.R250;
import org.hampelratte.svdrp.responses.highlevel.Recording;
import org.hampelratte.svdrp.responses.highlevel.Stream;
import org.hampelratte.svdrp.responses.highlevel.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.androvdr.svdrp.VDRConnection;

public class VdrCommands {
	private static transient Logger logger = LoggerFactory.getLogger(VdrCommands.class);

	public static RecordingInfo getRecordingInfo(int number) throws IOException {
	    Response response = VDRConnection.send(new LSTR(number));
	    if (response != null && response.getCode() == 215) {
            try {
                org.hampelratte.svdrp.responses.highlevel.Recording rec = new Recording();
                new RecordingParser().parseRecording(rec, response.getMessage());
                
                RecordingInfo recordingInfo = new RecordingInfo();
                recordingInfo.id = MD5.calculate(response.getMessage());

                recordingInfo.channelName = rec.getChannelName();
                recordingInfo.date = rec.getStartTime().getTimeInMillis() / 1000;
                recordingInfo.description = rec.getDescription();
                long end = rec.getEndTime().getTimeInMillis() / 1000;
                recordingInfo.duration = end - recordingInfo.date;
                recordingInfo.title = rec.getDisplayTitle();
                recordingInfo.priority = rec.getPriority();
                recordingInfo.lifetime = rec.getLifetime();

                for (Stream stream : rec.getStreams()) {
                    StreamInfo si = new StreamInfo();
                    // stream type
                    si.type = Integer.toString(stream.getType(), 16);
                    
                    // stream language
                    si.language = stream.getLanguage();
                    
                    // stream description
                    si.description = stream.getDescription();
                    
                    // stream kind
                    switch(stream.getContent()) {
                    case MP2V:
                    case H264:
                        si.kind = 1;
                        recordingInfo.setVideoStream(si);
                        break;
                    case MP2A:
                    case AC3:
                    case HEAAC:
                        si.kind = 2;
                        recordingInfo.addAudioStream(si);
                        break;
                    }
                }
                return recordingInfo;
			} catch (ParseException e) {
				logger.error("Couldn't get recording info", e);
				throw new IOException(e.getMessage());
			} catch (Exception e) {
				// --- Parser could throw NPE ---
				logger.error("Couldn't get recording info", e);
				throw new IOException("Invalid recording info");
			}
        } else {
            throw new IOException(response.getCode() + " - " + response.getMessage().replaceAll("\n$", ""));
        }
	}

	public static Response setTimer(Epg epg) {
		GregorianCalendar startTime = new GregorianCalendar();
		startTime.setTimeInMillis(epg.startzeit * 1000 - Preferences.getVdr().margin_start * 60 * 1000);

		GregorianCalendar endTime = new GregorianCalendar();
		endTime.setTimeInMillis(epg.startzeit * 1000 + epg.dauer * 1000 + Preferences.getVdr().margin_stop * 60 * 1000);

		Timer timer = new Timer();
		timer.setChannelNumber(epg.kanal);
		timer.setStartTime(startTime);
		timer.setEndTime(endTime);
		timer.setPriority(50);
		timer.setLifetime(99);
		timer.setTitle((epg.titel == null) ? "Unknown" : epg.titel);
		timer.setDescription(AndroApplication.getAppContext().getString(R.string.app_name));
		timer.changeStateTo(Timer.VPS, Preferences.getVdr().vps);

		NEWT newt = new NEWT(timer.toNEWT());
		Response response = VDRConnection.send(newt);
		if(response.getCode() != 250)
		    logger.error("Couldn't set timer: {}", response.getMessage());
		else {
			List<Timer> timers = TimerParser.parse(response.getMessage());
			if  (timers.size() > 0)
				response = new R250("New timer \"" + timers.get(0).getID() + "\"");
		}
		return response;
	}
}
