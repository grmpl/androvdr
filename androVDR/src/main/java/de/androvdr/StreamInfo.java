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

public class StreamInfo {
	public static final int STREAM_KIND_VIDEO = 1;
	public static final int STREAM_KIND_AUDIO = 2;
	public static final int STREAM_TYPE_VIDEO = 1;
	public static final int STREAM_TYPE_AUDIO = 2;
	
	public int kind;
	public String type;
	public String language;
	public String description;
	
	public StreamInfo() {}
	
	public StreamInfo(int kind, String type, String language, String description) {
		this.kind = kind;
		this.type = type;
		this.language = language;
		this.description = description;
	}
}
