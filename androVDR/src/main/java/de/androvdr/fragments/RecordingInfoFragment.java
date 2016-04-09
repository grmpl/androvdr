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

package de.androvdr.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import de.androvdr.Preferences;
import de.androvdr.R;
import de.androvdr.activities.AbstractFragmentActivity;
import de.androvdr.controllers.RecordingInfoController;

public class RecordingInfoFragment extends AbstractFragment {

	public static RecordingInfoFragment newInstance(int recordingNumber) {
		RecordingInfoFragment f = new RecordingInfoFragment();
		Bundle bundle = new Bundle();
		bundle.putInt("recordingnumber", recordingNumber);
		f.setArguments(bundle);
		return f;
	}
	
	public int getRecordingNumber() {
		return getArguments().getInt("recordingnumber");
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		LinearLayout view = (LinearLayout) getActivity().findViewById(R.id.reci);
		if (view == null)
			return;

		/*
		 * setTheme doesn't change background color :(
		 */
		if (! ((AbstractFragmentActivity) getActivity()).isDualPane())
			if (Preferences.blackOnWhite && view.getBackground() == null)
				view.setBackgroundColor(Color.WHITE);

		AbstractFragmentActivity activity = (AbstractFragmentActivity) getActivity();
		if (getRecordingNumber() >= 0)
			new RecordingInfoController(activity, activity.getHandler(), 
					view, getRecordingNumber());
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.recordinginfo_fragment, container, false);
		return root;
	}
}
