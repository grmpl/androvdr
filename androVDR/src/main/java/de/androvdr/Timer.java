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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Timer {
	private ArrayList<String> folders = new ArrayList<String>();
	
	public static final int TIMER_INACTIVE = 1;
	public static final int TIMER_ACTIVE = 2;
	public static final int TIMER_INSTANT = 3;
	public static final int TIMER_VPS = 4;
	public static final int TIMER_RECORDING = 5;
	
	private static SimpleDateFormat sDateformatter = new SimpleDateFormat("yyyy-MM-dd HHmm");
	private static SimpleDateFormat sTimeformatter = new SimpleDateFormat("HHmm");
	
	public int number;
	public int channel;
	public String title;
	public long start;
	public long end;
	public int priority;
	public int lifetime;
	public int lastUpdate;
	public int status;
	public String noDate = "";
	
	public Timer() { }
	
	public Timer(String vdrtimerinfo) throws ParseException {
		parse(vdrtimerinfo);
	}
	
	private boolean checkBit(int n, int pos) {
		int mask = 1 << pos;
		return (n & mask) == mask;
	}
	
	public String folder() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < folders.size(); i++) {
			if (i > 0)
				sb.append(" / ");
			sb.append(folders.get(i));
		}
		return sb.toString();
	}
	
	public int getStatus() {
		if (checkBit(status, 3))
			return TIMER_RECORDING;
		else if (checkBit(status, 2) && checkBit(status, 0))
			return TIMER_VPS;
		else if (checkBit(status, 0))
			return TIMER_ACTIVE;
		else
			return TIMER_INACTIVE;
	}
	
	public boolean inFolder() {
		return (folders.size() > 0);
	}
	
	public boolean isActive() {
		return checkBit(status, 0);
	}
	
	private void parse(String vdrtimerinfo) throws ParseException {
		String[] sa = vdrtimerinfo.split(" ");
		String[] tsa;
		
		try {
			number = Integer.valueOf(sa[0]);
			sa = vdrtimerinfo.substring(vdrtimerinfo.indexOf(' ') + 1).split(":");
			status = Integer.valueOf(sa[0]);
			channel = Integer.valueOf(sa[1]);

			try {
				start = sDateformatter.parse(sa[2] + " " + sa[3]).getTime() / 1000;
			} catch (ParseException e) {
				start = sTimeformatter.parse(sa[3]).getTime() / 1000;
				noDate = sa[2];
			}
				
			Date t1 = sTimeformatter.parse(sa[3]);
			Date t2 = sTimeformatter.parse(sa[4]);
			if (t2.before(t1))
				t2.setDate(t2.getDate() + 1);
			end = new Date(start * 1000 + (t2.getTime() - t1.getTime())).getTime() / 1000;
			
			priority = Integer.valueOf(sa[5]);
			lifetime = Integer.valueOf(sa[6]);
			tsa = sa[7].split("~");
			for (int i = 0; i < tsa.length - 1; i++)
				folders.add(tsa[i]);
			title = tsa[tsa.length - 1].replace('|', ':');
		} catch (Exception e) {
			throw new ParseException(e.toString(), 0);
		}
	}
	
	public void initFromEpgsearchResult(String vdrtimerinfo) throws ParseException {
		String[] sa = vdrtimerinfo.split(" ");
		
		try {
			sa = vdrtimerinfo.substring(vdrtimerinfo.indexOf(' ') + 1).split(":");
			channel = Integer.valueOf(sa[1]);

			start = sDateformatter.parse(sa[2] + " " + sa[3]).getTime() / 1000;
				
			Date t1 = sTimeformatter.parse(sa[3]);
			Date t2 = sTimeformatter.parse(sa[4]);
			if (t2.before(t1))
				t2.setDate(t2.getDate() + 1);
			end = new Date(start * 1000 + (t2.getTime() - t1.getTime())).getTime() / 1000;
			
			priority = Integer.valueOf(sa[5]);
			lifetime = Integer.valueOf(sa[6]);
			
			if (sa.length > 8) {
				StringBuilder sb = new StringBuilder();
				for (int i = 7; i < sa.length; i++) {
					sb.append(sa[i]);
					if (i < (sa.length - 1))
						sb.append(':');
				}
				title = sb.toString();
			} else {
				title = sa[7];
			}
		} catch (Exception e) {
			throw new ParseException(e.toString(), 0);
		}
	}
}
