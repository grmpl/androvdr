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

package de.androvdr.activities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;
import de.androvdr.Preferences;
import de.androvdr.R;

public class AboutActivity extends AbstractActivity {
	private static transient Logger logger = LoggerFactory.getLogger(AboutActivity.class);
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);

		/*
		 * setTheme doesn't change background color :(
		 */
		if (Preferences.blackOnWhite) {
			View view = findViewById(R.id.aboutid);
			view.setBackgroundColor(Color.WHITE);
		}

		try {
			String version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
			TextView tv = (TextView) findViewById(R.id.about_version);
			tv.setText("Version " + version);
			
			tv = (TextView) findViewById(R.id.about_url);
			tv.setText(Html.fromHtml("Project page at <a href=\"http://code.google.com/p/androvdr\">Google Code</a>."));
			tv.setMovementMethod(LinkMovementMethod.getInstance());

			tv = (TextView) findViewById(R.id.about_wiki);
			tv.setText(Html.fromHtml("<a href=\"http://code.google.com/p/androvdr/wiki/Documentation\">Documentation</a>"));
			tv.setMovementMethod(LinkMovementMethod.getInstance());
		} catch (NameNotFoundException e) {
			logger.error("Couldn't read version name");
		}
	}
}
