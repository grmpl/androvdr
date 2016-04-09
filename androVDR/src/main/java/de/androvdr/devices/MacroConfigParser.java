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

package de.androvdr.devices;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class MacroConfigParser {
	private final String mFilename;
	
	public String lastError;
	
	public MacroConfigParser(String filename) {
		mFilename = filename;
	}
	
	private InputStream getInputStream() throws IOException {
		return new FileInputStream(mFilename);
	}
	
	public ArrayList<Macro> parse() {
		lastError = "";
		SAXParserFactory factory = SAXParserFactory.newInstance();
		try {
			SAXParser parser = factory.newSAXParser();
			MacroConfigHandler handler = new MacroConfigHandler();
			parser.parse(getInputStream(), handler);
			return handler.getMacros();
		} catch (Exception e) {
			lastError = e.toString();
			return null;
		}
	}
}
