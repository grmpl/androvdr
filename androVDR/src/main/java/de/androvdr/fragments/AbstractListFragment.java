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

import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ListFragment;
import de.androvdr.Messages;
import de.androvdr.activities.AbstractFragmentActivity;

public class AbstractListFragment extends ListFragment {
	private static transient Logger logger = LoggerFactory.getLogger(AbstractListFragment.class);

	protected AbstractFragmentActivity mActivity;
	protected AtomicBoolean mControllerReady = new AtomicBoolean(false);
	protected int mCurrentItemIndex = -1;
	protected UpdateSelectedItemThread mUpdateSelectedItemThread;

	protected Handler mHandler = new Handler () {

		@Override
		public void handleMessage(Message msg) {
			logger.trace("handleMessage: arg1 = {}", msg.arg1);
			
			switch (msg.arg1) {
			case Messages.MSG_SELECT_ITEM:
				setSelectedItem(msg.arg2);
				break;
			case Messages.MSG_CONTROLLER_READY:
				synchronized (mControllerReady) {
					if (! mControllerReady.get()) {
						mControllerReady.getAndSet(true);
						mControllerReady.notifyAll();
					}
				}
			default:
				Message forward = new Message();
				forward.copyFrom(msg);
				mActivity.getHandler().sendMessage(forward);
				break;
			}
		}
	};

	protected void setSelectedItem(int position) {
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mActivity = (AbstractFragmentActivity) activity;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (savedInstanceState != null)
			mCurrentItemIndex = savedInstanceState.getInt("currentitemindex");
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			getListView().setFastScrollEnabled(true);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}
	
	@Override
	public void onPause() {
		if (mUpdateSelectedItemThread != null)
			mUpdateSelectedItemThread.interrupt();
		super.onPause();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("currentitemindex", mCurrentItemIndex);
	}
	
	protected class UpdateSelectedItemThread extends Thread {
		
		public UpdateSelectedItemThread() {
			start();
		}
		
		public int getPosition() {
			return -1;
		}
		
		@Override
		public void run() {
			boolean isInterrupted = false;

			logger.trace("UpdateSelectedItemThread started");
			synchronized (mControllerReady) {
				if (! mControllerReady.get()) {
					try {
						mControllerReady.wait();
					} catch (InterruptedException e) { 
						logger.trace("UpdateSelectedItemThread interrupted");
						isInterrupted = true;
					}
				}
				if (! isInterrupted) {
					int position = getPosition();
					if (position >= 0) {
						synchronized (mHandler) {
							Message msg = Messages.obtain(Messages.MSG_SELECT_ITEM, position);
							mHandler.sendMessage(msg);
						}
					}
				}
			}
			logger.trace("UpdateSelectedItemThread finished");
		}
	}
}
