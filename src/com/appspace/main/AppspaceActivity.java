package com.appspace.main;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ToggleButton;

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
		
		if(isMyServiceRunning(c)) {
			tb.setChecked(true);
		}
		
		tb.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				if(arg1) {
					c.startService(i);
				}
				else {
					new Thread() {
						public void run() {
							c.stopService(i);
						}
					}.start();
				}
			}
		});
	}
	
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
