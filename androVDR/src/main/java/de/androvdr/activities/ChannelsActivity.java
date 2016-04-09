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

import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.text.format.DateFormat;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TimePicker;
import de.androvdr.Preferences;
import de.androvdr.R;
import de.androvdr.controllers.ChannelController;
import de.androvdr.fragments.ChannelsFragment;

public class ChannelsActivity extends AbstractFragmentActivity {
	private static transient Logger logger = LoggerFactory.getLogger(ChannelsActivity.class);

	public static final int DIALOG_WHATS_ON = 1;
	public static final String SEARCHTIME = "searchtime";
	
	private ChannelController getController() {
    	FragmentManager fm = getSupportFragmentManager();
    	ChannelsFragment f = (ChannelsFragment) fm.findFragmentById(R.id.list_fragment);
    	return f.getController();
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.extendedchannels);
		
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
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_WHATS_ON:
			final Dialog dialog = new Dialog(this);
			dialog.setContentView(R.layout.extendedchannels_whats_on);
			dialog.setTitle(R.string.channels_whats_on);
			
			final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
			final DatePicker dp = (DatePicker) dialog.findViewById(R.id.channels_datePicker);
			final TimePicker tp = (TimePicker) dialog.findViewById(R.id.channels_timePicker);
			tp.setIs24HourView(DateFormat.is24HourFormat(getApplicationContext()));
			if (sp.contains("whats_on_hour")) {
				tp.setCurrentHour(sp.getInt("whats_on_hour", 0));
				tp.setCurrentMinute(sp.getInt("whats_on_minute", 0));
			}
			
			Button button = (Button) dialog.findViewById(R.id.channels_cancel);
			button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					dialog.dismiss();
				}
			});

			button = (Button) dialog.findViewById(R.id.channels_search);
			button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Editor editor = sp.edit();
					editor.putInt("whats_on_hour", tp.getCurrentHour());
					editor.putInt("whats_on_minute", tp.getCurrentMinute());
					editor.commit();
					
					SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy hh:mm");
					try {
						long time = df.parse(
								dp.getDayOfMonth() + "." + (dp.getMonth() + 1) + "." + dp.getYear() + " " +
								tp.getCurrentHour() + ":" + tp.getCurrentMinute()).getTime() / 1000;
						getController().whatsOn(time);
					} catch (ParseException e) {
						logger.error("Couldn't get date from pickers", e);
					}
					dialog.dismiss();
				}
			});
			return dialog;
		default:
			return super.onCreateDialog(id);
		}
	}
}
