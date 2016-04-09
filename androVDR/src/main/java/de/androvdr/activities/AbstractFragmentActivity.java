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

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.BadTokenException;
import android.widget.Toast;
import de.androvdr.ActionBarHelper;
import de.androvdr.ConfigurationManager;
import de.androvdr.Messages;
import de.androvdr.Preferences;
import de.androvdr.R;
import de.androvdr.SimpleGestureFilter;
import de.androvdr.SimpleGestureFilter.SimpleGestureListener;

public class AbstractFragmentActivity extends FragmentActivity implements SimpleGestureListener {
	private static transient Logger logger = LoggerFactory.getLogger(AbstractFragmentActivity.class);

	protected ConfigurationManager mConfigurationManager;
	protected SimpleGestureFilter mDetector;
	protected boolean mDualPane;
	
	protected Handler handler = new Handler () {
		private ProgressDialog pd = null;
		
		protected void dismiss() {
			if (pd != null) {
				try {
					pd.dismiss();
				} catch (IllegalArgumentException e) { }
				pd = null;
			}
		}

		@Override
		public void handleMessage(Message msg) {
			logger.trace("handleMessage: arg1 = {}", msg.arg1);
			
			Bundle bundle;
			switch (msg.arg1) {
			case Messages.MSG_PROGRESS_SHOW:
				if (mDualPane) {
					if (pd == null)
						setProgressBarIndeterminateVisibility(true);
				} else {
					dismiss();
					if (pd == null && ! isFinishing()) {
						pd = new ProgressDialog(AbstractFragmentActivity.this);
						pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
						pd.setMessage(AbstractFragmentActivity.this.getString(msg.arg2));
						pd.show();
					}
				}
				break;
			case Messages.MSG_CONTROLLER_LOADING:
				dismiss();
				if (pd == null && ! isFinishing()) {
					pd = new ProgressDialog(AbstractFragmentActivity.this);
					pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
					pd.setMessage(AbstractFragmentActivity.this.getString(msg.arg2));
					pd.show();
				}
				break;
			case Messages.MSG_CONTROLLER_READY:
				dismiss();
				break;
			case Messages.MSG_PROGRESS_UPDATE:
				if (mDualPane) {
					setProgressBarIndeterminateVisibility(true);
				} else {
					if (pd != null && pd.isShowing()) {
						pd.setMessage(AbstractFragmentActivity.this.getString(msg.arg2));
					}
				}
				break;
			case Messages.MSG_TITLEBAR_PROGRESS_SHOW:
				setProgressBarIndeterminateVisibility(true);
				break;
			case Messages.MSG_TITLEBAR_PROGRESS_DISMISS:
				setProgressBarIndeterminateVisibility(false);
				break;
			case Messages.MSG_PROGRESS_DISMISS:
				if (mDualPane)
					setProgressBarIndeterminateVisibility(false);
				else
					dismiss();
				break;
			case Messages.MSG_EPGSEARCH_NOT_FOUND:
				dismiss();
				showError(AbstractFragmentActivity.this.getString(R.string.epgsearch_not_installed));
				break;
			case Messages.MSG_ERROR:
				bundle = msg.getData();
				if (bundle != null) {
					String message = bundle.getString(Messages.MSG_MESSAGE);
					if (message != null) {
						dismiss();
						showError(message);
					}
				}
				break;
			case Messages.MSG_INFO:
				bundle = msg.getData();
				if (bundle != null) {
					String message = bundle.getString(Messages.MSG_MESSAGE);
					if (message != null) {
						dismiss();
						Toast.makeText(AbstractFragmentActivity.this, message, Toast.LENGTH_LONG).show();
					}
				}
				break;
			}
		}
	};
	
	@Override
	public boolean dispatchTouchEvent(android.view.MotionEvent ev) {
		mDetector.onTouchEvent(ev);
		return super.dispatchTouchEvent(ev);
	}
	
	public Handler getHandler() {
		return handler;
	}
	
	public boolean isDualPane() {
		return mDualPane;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		logger.trace("onCreate");
		
		Preferences.init(false);
		if (Preferences.blackOnWhite)
			setTheme(R.style.Theme_Light);
		
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setProgressBarIndeterminateVisibility(false);
		mDetector = new SimpleGestureFilter(this, this);
		mDetector.setMode(SimpleGestureFilter.MODE_TRANSPARENT);
		mConfigurationManager = ConfigurationManager.getInstance(this);
		ActionBarHelper.setHomeButtonEnabled(this, true);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		logger.trace("onDestroy");
	}
	
	@Override
	public void onDoubleTap() {
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		return mConfigurationManager.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
            Intent intent = new Intent(this, AndroVDR.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		logger.trace("onPause");
		mConfigurationManager.onPause();
		handler.sendMessage(Messages.obtain(Messages.MSG_PROGRESS_DISMISS));
		handler.sendMessage(Messages.obtain(Messages.MSG_TITLEBAR_PROGRESS_DISMISS));
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		logger.trace("onResume");
		mConfigurationManager.onResume();
	}
	
	@Override
	public void onSwipe(int direction) {
		if (mConfigurationManager.doSwipe(direction))
			finish();
	}

	@Override
	public void setContentView(int layoutResID) {
		super.setContentView(layoutResID);
		
		// --- on Honeycomb progress in ActionBar starts visible ---
		setProgressBarIndeterminateVisibility(false);
		
		int fragmentId;
		if (Preferences.detailsLeft)
			fragmentId = R.id.detail_fragment_left;
		else
			fragmentId = R.id.detail_fragment_right;
		
		View details = findViewById(fragmentId);
		mDualPane = (details != null && details.getVisibility() == View.VISIBLE);
		logger.trace("setContentView: mDualPane = {}", mDualPane);
	}
	
	protected void showError(String message) {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(this.getText(R.string.error));
			alert.setMessage(message);
		alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				finish();
			}
		});
		if (! isFinishing())
			try {
				alert.show();
			} catch (BadTokenException e) { }
	}
}
