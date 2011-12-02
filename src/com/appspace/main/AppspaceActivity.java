package com.appspace.main;

import java.io.IOException;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ToggleButton;

// Activity to provide ToggleButton and various other settings
public class AppspaceActivity extends Activity {

	private ToggleButton tb;
	private Context c;
	private Intent i;
	public static final String myService = "com.appspace.main.DetectAppLaunchService";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		tb = (ToggleButton) findViewById(R.id.toggleButton1);
		c = getApplicationContext();
		i = new Intent(c, DetectAppLaunchService.class);
		
		// Install the database
		AppspaceDbHelper helper = new AppspaceDbHelper(c);
		try {
			helper.createDataBase();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// Set initial state of ToggleButton
		if(isMyServiceRunning(c)) {
			tb.setChecked(true);
		}
		
		tb.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				if(arg1) {
					// ToggleButton returns ON
					c.startService(i);
				}
				else {
					// ToggleButton returns OFF
					new Thread() {
						public void run() {
							c.stopService(i);
						}
					}.start();
				}
			}
		});
	}
	
	// Method to check if the service is already running
	public static boolean isMyServiceRunning(Context c) {
	    ActivityManager manager = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	        if (myService.equals(service.service.getClassName())) {
	            return true;
	        }
	    }
	    return false;
	}

}
