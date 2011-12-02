package com.appspace.main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

// BroadcastReceiver to listen for updates
public class AppspaceReceiver extends BroadcastReceiver {

	private static final String tag = "Receiver";
	public static final String APP_LAUNCH_DETECTED = "com.appspace.main.NEW_APP_LAUNCHED";
	public static final String SCREEN_OFF = "com.appspace.main.SCREEN_OFF";
	public static final String SCREEN_ON = "com.appspace.main.SCREEN_ON";
	public static final int CATEGORY_HIGH_DEMANDING = 1;
	public static final int CATEGORY_MODERATE_DEMANDING = 2;
	public static final int CATEGORY_LOW_DEMANDING = 3;
	
	@Override
	public void onReceive(Context arg0, Intent arg1) {
		String action = arg1.getAction();
        
		// New app launch detected
		if(action.equals(APP_LAUNCH_DETECTED)) {
			AppspaceDbAdapter adapter = new AppspaceDbAdapter(arg0);
			adapter.open();
			String pname = arg1.getExtras().getString("package");
			System.out.println(pname);
			int category = adapter.fetchPackageCategory(pname);
			adapter.close();
			Log.i(tag, "Category = "+category);
			
			if(category == CATEGORY_HIGH_DEMANDING) {
				if(!SysFS.setSCALING_SETSPEED(DetectAppLaunchService.freq.get(DetectAppLaunchService.freq.size()-1))) {
		        	Toast.makeText(arg0, "Please change to userspace governor", Toast.LENGTH_SHORT).show();
		        }
			}
			else if(category == CATEGORY_MODERATE_DEMANDING) {
				if(!SysFS.setSCALING_SETSPEED(DetectAppLaunchService.freq.get(DetectAppLaunchService.freq.size()/2))) {
		        	Toast.makeText(arg0, "Please change to userspace governor", Toast.LENGTH_SHORT).show();
		        }
			}
			else if(category == CATEGORY_LOW_DEMANDING) {
				if(!SysFS.setSCALING_SETSPEED(DetectAppLaunchService.freq.get(1))) {
		        	Toast.makeText(arg0, "Please change to userspace governor", Toast.LENGTH_SHORT).show();
		        }
			}
			else {
				// Either category not defined or there is no entry for the package
			}
		}
		
		// User has unlocked the screen lock
		else if(action.equals(Intent.ACTION_USER_PRESENT)) {
			if(AppspaceActivity.isMyServiceRunning(arg0)) {
				Log.i(tag, "user present");
				if(!SysFS.setSCALING_SETSPEED(DetectAppLaunchService.freq.get(DetectAppLaunchService.freq.size()/2))) {
		        	Toast.makeText(arg0, "Please change to userspace governor", Toast.LENGTH_SHORT).show();
				}
				DetectAppLaunchService.bt.loop_cpu = true;
			}
		}
		
		// User has turned the screen OFF
		else if(action.equals(SCREEN_OFF)) {
			Log.i(tag, "screen off");
			// Set frequency to minimum
			if(!SysFS.setSCALING_SETSPEED(DetectAppLaunchService.freq.get(0))) {
	        	Toast.makeText(arg0, "Please change to userspace governor", Toast.LENGTH_SHORT).show();
			}
			DetectAppLaunchService.bt.loop_cpu = false;
		}
		
		// User has turned the screen ON
		else if(action.equals(SCREEN_ON)) {
			Log.i(tag, "screen on");
		}
	}
}