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

import android.os.Bundle;
import android.view.View;
import de.androvdr.Preferences;
import de.androvdr.R;

public class EpgsdataActivity extends AbstractFragmentActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.epgsdata);
		
		if (isDualPane()) {
			int fragmentId;
			if (Preferences.detailsLeft)
				fragmentId = R.id.detail_fragment_right;
			else
				fragmentId = R.id.detail_fragment_left;
			findViewById(fragmentId).setVisibility(View.GONE);
		}
	}
}
