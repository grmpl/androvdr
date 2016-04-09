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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.StringTokenizer;

import org.hampelratte.svdrp.Command;
import org.hampelratte.svdrp.Response;
import org.hampelratte.svdrp.commands.LSTT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.androvdr.svdrp.VDRConnection;

public class Timers {
	private static transient Logger logger = LoggerFactory.getLogger(Timers.class);
	
	private ArrayList<Timer> mItems = new ArrayList<Timer>();
	
	public Timers() throws IOException {
		init();
	}
	
	public Timers(EpgSearch search) throws IOException {
		init(search);
	}
	
	public ArrayList<Timer> getItems() {
		return mItems;
	}
	
	private void init() throws IOException {
		int lastUpdate = (int) (new Date().getTime() / 60000);
		
		Response response = VDRConnection.send(new LSTT());
		if(response.getCode() == 250) {
		    String message = response.getMessage();
		    StringTokenizer st = new StringTokenizer(message, "\n");
		    while(st.hasMoreTokens()) {
		        try {
		            Timer timer = new Timer(st.nextToken());
		            timer.lastUpdate = lastUpdate;
		            mItems.add(timer);
		        } catch (ParseException e) {
		            logger.error("Invalid timer format" + e);
		            continue;
		        }
		    }
		    Collections.sort(mItems, new TimerComparer());
		} else if(response.getCode() == 550) {
		    if(!"No timers defined".equals(response.getMessage().trim())) {
		        throw new IOException("Couldn't retrieve timers: " 
		        		+ response.getCode() + " " + response.getMessage().replaceAll("\n$", ""));
		    }
		} else {
		    throw new IOException(response.getCode() + " - " + response.getMessage().replaceAll("\n$", ""));
		}
	}

	@SuppressWarnings("serial")
	private void init(EpgSearch search) throws IOException {
		int lastUpdate = (int) (new Date().getTime() / 60000);
		try {
			int marginStart = 0;
			int marginStop = 0;
			int count = 0;
			
			final String command = "PLUG epgsearch FIND 0:"
					+ search.search
					+ ":0:::0::0:0:"
					+ (search.inTitle ? 1 : 0) + ":"
					+ (search.inSubtitle ? 1 : 0) + ":"
					+ (search.inDescription ? 1 : 0) + ":"
					+ "0:::0:0:0:0::::::0:0:0::0::1:1:1:0::::::0:::0::0:::::";
			
			Response response = VDRConnection.send(new Command() {
                @Override
                public String toString() {
                    return command;
                }
                
                @Override
                public String getCommand() {
                    return command;
                }
            });
            
            if (response.getCode() == 550)
                throw new IOException("550 epgsearch plugin not found");
            
			String result = response.getMessage();
			StringTokenizer st = new StringTokenizer(result, "\n");
			while(st.hasMoreTokens()) {
				String s = st.nextToken();
				count += 1;
				if (count > Preferences.epgsearch_max) {
					break;
				}
				
				try {
					if (response.getCode() == 900) {
						Timer timer = new Timer();
						timer.initFromEpgsearchResult(s);
						timer.lastUpdate = lastUpdate;
						mItems.add(timer);
					}
				} catch (ParseException e) {
					logger.error("Invalid timer format", e);
					continue;
				}
			}
			
			response = VDRConnection.send(new Command() {
			    private String command = "PLUG epgsearch SETP";
			    
                @Override
                public String toString() {
                    return command;
                }
                
                @Override
                public String getCommand() {
                    return command;
                }
            });
			
			result = response.getMessage();
			st = new StringTokenizer(result, "\n");
            while(st.hasMoreTokens()) {
                String s = st.nextToken();
				
				try {
					String[] sa = s.split(":");
					if (sa[0].equals("DefMarginStart"))
						marginStart = Integer.parseInt(sa[1].trim()) * 60;
					if (sa[0].equals("DefMarginStop"))
						marginStop = Integer.parseInt(sa[1].trim()) * 60;
				} catch (Exception e) {
					logger.error("Invalid epgsearch setp response", e);
					continue;
				}
			};

			for (Timer timer : mItems) {
				timer.start += marginStart;
				timer.end -= marginStop;
			}
			
			Collections.sort(mItems, new TimerComparer());
		} catch (IOException e) {
			logger.error("Couldn't get epgsearch result", e);
			throw e;
		}
	}
	
	private class TimerComparer implements java.util.Comparator<Timer> {
		public int compare(Timer a, Timer b) {
			Long l = a.start;
			return l.compareTo(b.start);
		}
	}
}
