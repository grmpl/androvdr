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
import java.util.ArrayList;
import java.util.List;

import org.hampelratte.svdrp.Response;
import org.hampelratte.svdrp.commands.LSTE;
import org.hampelratte.svdrp.responses.highlevel.EPGEntry;
import org.hampelratte.svdrp.responses.highlevel.Stream;
import org.hampelratte.svdrp.parsers.EPGParser;

import de.androvdr.svdrp.VDRConnection;

public class Epgs {
	public static final String TAG = "Epgs";

	private final int EPG_ALL = 0;
	private final int EPG_NOW = -1;
	private final int EPG_NEXT = -2;
	
	private final int mChannel;
	
	public Epgs(int channel) throws IOException {
		mChannel = channel;
	}
	
	public ArrayList<Epg> getAll() throws IOException {
		try {
			return get(EPG_ALL);
		} catch (NoScheduleException e) {
			return new ArrayList<Epg>();
		}
	}
	
	public Epg getAt(long time) throws IOException {
		ArrayList<Epg> list;
		try {
			list = get(time);
		} catch (NoScheduleException e) {
			list = new ArrayList<Epg>();
		}
		if(list.size() == 1)
			return list.get(0);
		else
			return new Epg(mChannel, true);
	}
	
	public Epg getNext() throws IOException {
		ArrayList<Epg> list;
		try {
			list = get(EPG_NEXT);
		} catch (NoScheduleException e) {
			list = new ArrayList<Epg>();
		}
		if(list.size() == 1)
			return list.get(0);
		else
			return new Epg(mChannel, true);
	}
	
	public Epg getNow() throws IOException {
		ArrayList<Epg> list;
		try {
			list = get(EPG_NOW);
		} catch (NoScheduleException e) {
			list = new ArrayList<Epg>();
		}
		if(list.size() == 1)
			return list.get(0);
		else
			return new Epg(mChannel, true);
	}
	
	public ArrayList<Epg> get(long count) throws IOException, NoScheduleException {
		ArrayList<Epg> result = new ArrayList<Epg>();

		LSTE cmd = new LSTE(mChannel);
		
		if (count == EPG_NOW)
			cmd.setTime("now");
		else if (count == EPG_NEXT)
		    cmd.setTime("next");
		else if (count > 1000)
		    cmd.setTime("at " + count);

		Response response = VDRConnection.send(cmd);
		if(response.getCode() == 215) {
		    List<EPGEntry> epgList = new EPGParser().parse(response.getMessage());
		    int entryCount = 0;
		    for (EPGEntry entry : epgList) {
		        Epg epg = new Epg();
		        epg.eventId = entry.getEventID();
                epg.startzeit = entry.getStartTime().getTimeInMillis() / 1000;
                long end = entry.getEndTime().getTimeInMillis() / 1000;
                epg.dauer = (int) (end - epg.startzeit);
                epg.titel = entry.getTitle();
                epg.beschreibung = entry.getDescription();
                epg.kurztext = entry.getShortText();
                epg.vps = entry.getVpsTime().getTimeInMillis() / 1000;
                epg.kanal = mChannel;
                epg.channelName = entry.getChannelName();
                
                for (Stream stream : entry.getStreams()) {
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
                        epg.setVideoStream(si);
                        break;
                    case MP2A:
                    case AC3:
                    case HEAAC:
                        si.kind = 2;
                        epg.addAudioStream(si);
                        break;
                    }
                }
                
                result.add(epg);
                if(++entryCount >= count && count > 0) {
                    break;
                }
            }
		} else if (response.getCode() == 550) {
			throw new NoScheduleException();
		} else {
			throw new IOException(response.getCode() + " - " + response.getMessage().replaceAll("\n$", ""));
		}
		
		for (int i = 0; i < result.size(); i++)
			result.get(i).calculatePercentDone();
		return result;
	}
	
	public class NoScheduleException extends Exception {
		private static final long serialVersionUID = 1L;
	};
}
