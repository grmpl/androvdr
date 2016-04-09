package de.androvdr;

import java.io.File;
import java.util.ArrayList;

import android.content.Intent;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.Prediction;
import android.gesture.GestureOverlayView.OnGesturePerformedListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
import de.androvdr.activities.AbstractActivity;
import de.androvdr.devices.VdrDevice;

public class GesturesFind extends AbstractActivity implements OnGesturePerformedListener {
	
	@Override
	protected void onPause() {
		super.onPause();
		if(mLibrary != null) {
			mLibrary = null;
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if(mLibrary == null){
			mLibrary = GestureLibraries.fromFile(mStoreFile);
		}
	}

    private GestureLibrary mLibrary;
    private final File mStoreFile = new File(Preferences.getGestureFileName());

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (Preferences.blackOnWhite)
        	setTheme(R.style.Theme);
        
        setContentView(R.layout.gestures);
        
        GestureOverlayView gestures = (GestureOverlayView) findViewById(R.id.gestures);
        gestures.addOnGesturePerformedListener(this);
    }
	
	public void onGesturePerformed(GestureOverlayView overlay, Gesture gesture) {
        if (!mLibrary.load()) {
        	Toast.makeText(getApplicationContext(),getString(R.string.no_gestures),Toast.LENGTH_LONG).show();  	
        	return;
        }
        if(mLibrary.getGestureEntries().size()==0){
        	Toast.makeText(getApplicationContext(),getString(R.string.no_gestures),Toast.LENGTH_LONG).show();  	
        	return;
        }
        
		ArrayList<Prediction> predictions = mLibrary.recognize(gesture);

		// We want at least one prediction
		if (predictions.size() > 0) {
			Prediction prediction = predictions.get(0);
			// We want at least some confidence in the result
			if (prediction.score > 1.0) {
				new SendTask().execute(prediction.name);
			}
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.gesture_option_menu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.gom_gesturebuilder:
			Intent intent = new Intent(this, GesturesBuilder.class);
			startActivityForResult(intent, 1);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	private class SendTask extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... params) {
			String result = null;
			VdrDevice vdr = Preferences.getVdr();
			if (vdr != null) {
				if (! vdr.write(params[0])) {
					result = vdr.getLastError();
				} else {
					result = params[0];
				}
			} else {
				result = "No VDR defined";
			}
			return result;
		}
		
		@Override
		protected void onPostExecute(String result) {
			if (result != null)
				Toast.makeText(GesturesFind.this, result, Toast.LENGTH_SHORT).show();
		}
		
	}
}
