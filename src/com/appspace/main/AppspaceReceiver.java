package com.appspace.main;

import java.util.ArrayList;
import java.util.StringTokenizer;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class AppspaceReceiver extends BroadcastReceiver {

	private static final String tag = "Receiver";
	public static final String APP_LAUNCH_DETECTED = "com.appspace.main.NEW_APP_LAUNCHED";
	public static final String SCREEN_OFF = "com.appspace.main.SCREEN_OFF";
	public static final String SCREEN_ON = "com.appspace.main.SCREEN_ON";
	
	@Override
	public void onReceive(Context arg0, Intent arg1) {
		
		String action = arg1.getAction();
//		ArrayList<String> freq = new ArrayList<String>();
//        
//        String all = SysFS.getSCALING_AVAILABLE_FREQUENCIES();
//        String min = SysFS.getSCALING_MIN_FREQ();
//        String max = SysFS.getSCALING_MAX_FREQ();
//        String temp = "";
//        
//        StringTokenizer st = new StringTokenizer(all, " ");
//        while(st.hasMoreTokens())
//        {
//        	temp = st.nextToken();
//        	if(Integer.parseInt(temp) >= Integer.parseInt(min))
//        	{
//        		if(Integer.parseInt(temp) <= Integer.parseInt(max))
//        		{
//        			freq.add(temp);
//        		}
//        	}
//        }
		
		String min = SysFS.getSCALING_MIN_FREQ();
		String max = SysFS.getSCALING_MAX_FREQ();
        
		if(action.equals(APP_LAUNCH_DETECTED)) {
			Log.i(tag, "receiver called with data ::: "+arg1.getExtras().getString("appName"));
	        if(!SysFS.setSCALING_SETSPEED(max)) {
	        	Toast.makeText(arg0, "Please change to userspace governor", Toast.LENGTH_SHORT).show();
	        }
		}
		else if(action.equals(Intent.ACTION_USER_PRESENT)) {
			if(AppspaceActivity.isMyServiceRunning(arg0)) {
				Log.i(tag, "user present");
				if(!SysFS.setSCALING_SETSPEED(max)) {
		        	Toast.makeText(arg0, "Please change to userspace governor", Toast.LENGTH_SHORT).show();
				}
			}
		}
		else if(action.equals(SCREEN_OFF)) {
			Log.i(tag, "screen off");
			if(!SysFS.setSCALING_SETSPEED(min)) {
	        	Toast.makeText(arg0, "Please change to userspace governor", Toast.LENGTH_SHORT).show();
			}
		}
		else if(action.equals(SCREEN_ON)) {
			Log.i(tag, "screen on");
			if(!SysFS.setSCALING_SETSPEED(min)) {
	        	Toast.makeText(arg0, "Please change to userspace governor", Toast.LENGTH_SHORT).show();
			}
		}
	}
}