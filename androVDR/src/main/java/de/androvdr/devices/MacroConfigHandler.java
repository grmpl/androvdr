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

import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class MacroConfigHandler extends DefaultHandler {
	private static final String MACRO = "macro";
	private static final String NAME = "name";
	private static final String COMMAND = "command";
	
	private ArrayList<Macro> mMacros;
	private Macro mCurrent;
	private StringBuilder mSb;
	
	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		super.characters(ch, start, length);
		mSb.append(ch, start, length);
	}
	
	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		super.endElement(uri, localName, qName);
		if (mCurrent != null) {
			if (localName.equalsIgnoreCase(NAME)) {
				mCurrent.name = mSb.toString();
			} else if (localName.equalsIgnoreCase(COMMAND)) {
				mCurrent.commands.add(mSb.toString());
			} else if (localName.equalsIgnoreCase(MACRO)) {
				mMacros.add(mCurrent);
			}
		}
	}
	
	public ArrayList<Macro> getMacros() {
		return mMacros;
	}
	
	@Override
	public void startDocument() throws SAXException {
		super.startDocument();
		mMacros = new ArrayList<Macro>();
		mSb = new StringBuilder();
	}
	
	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		super.startElement(uri, localName, qName, attributes);
		if (localName.equalsIgnoreCase(MACRO))
			mCurrent = new Macro();
		mSb.setLength(0);
	}
}
