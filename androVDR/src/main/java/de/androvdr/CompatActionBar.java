package de.androvdr;

import android.app.ActionBar;
import android.app.Activity;

public class CompatActionBar {

	public static void setHomeButtonEnabled(Activity activity, boolean enabled) {
		ActionBar actionBar = activity.getActionBar();
		if (actionBar != null)
			actionBar.setHomeButtonEnabled(enabled);
	}

}
