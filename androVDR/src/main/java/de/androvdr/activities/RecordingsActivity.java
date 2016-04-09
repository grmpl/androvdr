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
import android.support.v4.app.FragmentManager;
import android.view.KeyEvent;
import android.view.View;
import de.androvdr.Preferences;
import de.androvdr.R;
import de.androvdr.controllers.RecordingController;
import de.androvdr.fragments.RecordingsFragment;

public class RecordingsActivity extends AbstractFragmentActivity {
	
	private RecordingController getController() {
    	FragmentManager fm = getSupportFragmentManager();
    	RecordingsFragment f = (RecordingsFragment) fm.findFragmentById(R.id.list_fragment);
    	return f.getController();
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.recordings);
		
		if (isDualPane()) {
			int fragmentId;
			if (Preferences.detailsLeft)
				fragmentId = R.id.detail_fragment_right;
			else
				fragmentId = R.id.detail_fragment_left;
			findViewById(fragmentId).setVisibility(View.GONE);
		}
	}

	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            getController().action(RecordingController.RECORDING_ACTION_KEY_BACK);
            return true;
        }
        else
        	return super.onKeyDown(keyCode, event);
    }
	
	@Override
    public void onSwipe(int direction) {
    	if (mConfigurationManager.doSwipe(direction))
    		getController().action(RecordingController.RECORDING_ACTION_KEY_BACK);
    }
}
