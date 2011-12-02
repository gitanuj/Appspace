package com.appspace.main;

import java.util.ArrayList;
import java.util.StringTokenizer;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

// Background service
public class DetectAppLaunchService extends Service {

	private static final String tag = "Service";
    public static BackgroundTask bt;
    public static ArrayList<String> freq;
    
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(tag, "service started");
		
		// Fetch CPU info before doing anything
		freq = new ArrayList<String>();
      
		int minFrequency = Integer.parseInt(SysFS.getSCALING_MIN_FREQ());
		int maxFrequency = Integer.parseInt(SysFS.getSCALING_MAX_FREQ());
		String all = SysFS.getSCALING_AVAILABLE_FREQUENCIES();
		int temp;
		String temp1;
		
		StringTokenizer st = new StringTokenizer(all, " ");
		while(st.hasMoreTokens()) {
			temp1 = st.nextToken();
			temp = Integer.parseInt(temp1);
			if((temp >= minFrequency) && (temp <= maxFrequency)) {
				freq.add(temp1);
			}
		}
		
		// Spawn a superuser process to get root access
		SysFS.spawnSuperuserProcess();
		
		// Prepare and execute BackgroundTask
		bt = new BackgroundTask();
		bt.execute(this, null, null);
		
		// The service has to be persistent
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		// Destroy BackgroundTask
		bt.cancel(true);
		
		// Kill superuser process
		SysFS.killSuperuserProcess();
		Log.i(tag, "service destroyed");
	}
}
