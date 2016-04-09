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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class UsertabParser {
	
	private static transient Logger logger = LoggerFactory.getLogger(UsertabParser.class);
	
	ArrayList<UserButton> buttons = new ArrayList<UserButton>();
	
	
	public UsertabParser(File file){
		if(file.exists() && file.isFile())
			parseFile(file);
		else
			logger.error("Invalid layout description");
	}
	
	private void parseFile(File file){
		String line;
		try {
			BufferedReader r = new BufferedReader(new FileReader(file));
			while((line = r.readLine())!= null){
				if(line.startsWith("#"))
					continue;
				if(line.length()>0){
					UserButton b = parseLine(line);
					if(b!= null)
						buttons.add(b);
				}

			}
			r.close();
		} catch (Exception e) {
			logger.error("Couldn't read layout description", e);
		}
	}
	
	private UserButton parseLine(String line){
		String[] s = line.split(",");
		if(s.length == 7){
			UserButton button = new UserButton();
			if(s[0].equals("Image")){
				button.art = 2;
				File img = new File(Preferences.getExternalRootDirName() + "/" + s[1]);
				if(img.exists() && img.isFile()){
					button.beschriftung = img.getAbsolutePath();	
				}
				else {
					logger.error("Image not found: {}", s[1]);
					return null;
				}
			}
			else{
				if(s[0].equals("Text")){
				 button.art = 1;
				 button.beschriftung = s[1];	
				}
				else // ungueltiger Eintrag
					return null;
			}
			try {
			button.posX = Integer.parseInt(s[2]);
			button.posY = Integer.parseInt(s[3]);
			button.height = Integer.parseInt(s[4]);
			button.width = Integer.parseInt(s[5]);
			}catch(NumberFormatException n){
				return null;
			}
			button.action = s[6];
			logger.debug("Parsed {}", button.beschriftung);
			return button;
		}
		logger.error("Invalid button definition: {}", line);
		return null;
	}
	
	public ArrayList<UserButton> getButtons(){
		return buttons;
	}
	
	public class UserButton {
		public int art;
		public String beschriftung;
		public int posX,posY;
		public int height,width;
		public String action;
	}
}
