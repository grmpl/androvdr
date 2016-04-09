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
import de.androvdr.SimpleGestureFilter;
import de.androvdr.SimpleGestureFilter.SimpleGestureListener;

public class AbstractGestureActivity extends AbstractActivity implements SimpleGestureListener {

	protected SimpleGestureFilter mDetector;
	
	@Override
	public boolean dispatchTouchEvent(android.view.MotionEvent ev) {
		mDetector.onTouchEvent(ev);
		return super.dispatchTouchEvent(ev);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mDetector = new SimpleGestureFilter(this, this);
		mDetector.setMode(SimpleGestureFilter.MODE_TRANSPARENT);
	}
	
	@Override
	public void onDoubleTap() {
	}

	@Override
	public void onSwipe(int direction) {
		if (mConfigurationManager.doSwipe(direction))
			finish();
	}
}
