package de.androvdr;

import android.app.Activity;
import android.os.Build;
import android.support.v4.app.FragmentActivity;

public class ActionBarHelper {

	public static CompatActionBar getActionBar(FragmentActivity activity) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) 
			return new CompatActionBar();
		else
			return null;
	}

	public static void setHomeButtonEnabled(Activity activity, boolean enabled) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) 
			CompatActionBar.setHomeButtonEnabled(activity, enabled);
	}
}
