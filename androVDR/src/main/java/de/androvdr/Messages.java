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

import android.os.Message;

public class Messages {
	public static final int MSG_DONE = 1;
	public static final int MSG_PROGRESS_SHOW = 2;
	public static final int MSG_PROGRESS_DISMISS = 3;
	public static final int MSG_PROGRESS_UPDATE = 4;
	public static final int MSG_TITLEBAR_PROGRESS_SHOW = 5;
	public static final int MSG_TITLEBAR_PROGRESS_DISMISS = 6;
	public static final int MSG_DATA_UPDATE_DONE = 8;
	public static final int MSG_CONTROLLER_READY = 9;
	public static final int MSG_CONTROLLER_LOADING = 10;
	public static final int MSG_EPGSEARCH_NOT_FOUND = 11;
	public static final int MSG_ERROR = 12;
	public static final int MSG_INFO = 13;
	public static final int MSG_SELECT_ITEM = 14;
	
	public static final String MSG_MESSAGE = "message";
	
	public static Message obtain(int arg1) {
		Message msg = Message.obtain();
		msg.arg1 = arg1;
		return msg;
	}
	
	public static Message obtain(int arg1, int arg2) {
		Message msg = Messages.obtain(arg1);
		msg.arg2 = arg2;
		return msg;
	}
}
